import {createHash} from 'node:crypto';
import {FieldValue} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
import {civilTimestamp,requireId,requireMembership} from './shared.js';
import {reviewTypes} from './program-review.js';
export const adaptations=['MUY_BUENA','BUENA','REGULAR','CON_DIFICULTADES'];
export const nextReviewId=id=>createHash('sha256').update(`${id}\0next`).digest('hex');
export async function recordFollowUpOperation(db,bucket,uid,data,now=Date.now()) {
 if(!uid) throw new HttpsError('unauthenticated','Inicia sesión para guardar el seguimiento.');
 const {reviewId,shelterId,date,adaptation,notes,photo=null,next=null}=data??{};
 for(const id of [reviewId,shelterId]) requireId(id);
 const done=civilTimestamp(date);
 if(done.toMillis()>now||!adaptations.includes(adaptation)||typeof notes!=='string'||notes!==notes.trim()||notes.length<15||notes.length>2000) throw new HttpsError('invalid-argument','Revisa la fecha, la adaptación y las observaciones (15 a 2000 caracteres).');
 let nextDate=null;
 if(next!==null) {
  if(!next||!reviewTypes.includes(next.type)) throw new HttpsError('invalid-argument','Selecciona el tipo de la siguiente revisión.');
  nextDate=civilTimestamp(next.date);
  if(nextDate.toMillis()<=done.toMillis()) throw new HttpsError('invalid-argument','La siguiente revisión debe ser posterior al seguimiento realizado.');
 }
 const prefix=`refugios/${shelterId}/seguimientos/${reviewId}/`;
 if(photo!==null&&(typeof photo!=='string'||!photo.startsWith(prefix)||!new RegExp('^[0-9a-f-]{36}\\.jpg$').test(photo.slice(prefix.length)))) throw new HttpsError('invalid-argument','La fotografía no pertenece a esta revisión.');
 return db.runTransaction(async tx=>{
  await requireMembership(tx,db,uid,shelterId);
  const reviewRef=db.doc(`revisiones/${reviewId}`),resultRef=db.doc(`seguimientos/${reviewId}`),nextRef=db.doc(`revisiones/${nextReviewId(reviewId)}`);
  const [review,existing,existingNext]=await tx.getAll(reviewRef,resultRef,nextRef);
  if(!review.exists) throw new HttpsError('failed-precondition','La revisión no está disponible.');
  if(review.get('refugioId')!==shelterId) throw new HttpsError('permission-denied','Esta revisión no pertenece al refugio.');
  const adoption=await tx.get(db.doc(`adopciones/${review.get('adopcionId')}`));
  if(!adoption.exists||adoption.get('refugioId')!==shelterId||adoption.get('perritoId')!==review.get('perritoId')) throw new HttpsError('failed-precondition','La revisión necesita una adopción válida.');
  if(done.toMillis()<adoption.get('fechaEntrega').toMillis()) throw new HttpsError('invalid-argument','El seguimiento no puede ser anterior a la entrega.');
  const receipt=alreadyExisting=>({id:reviewId,reviewId,shelterId,date,nextReviewId:next?nextRef.id:null,alreadyExisting});
  if(existing.exists) {
   const photos=existing.get('fotos')??[];
   const samePhoto=photo===null?photos.length===0:photos.length===1&&photos[0].ruta===photo;
   const sameNext=next===null?!existingNext.exists:existingNext.exists&&existingNext.get('adopcionId')===review.get('adopcionId')&&existingNext.get('refugioId')===shelterId&&existingNext.get('tipo')===next.type&&existingNext.get('fechaProgramada').isEqual(nextDate);
   if(existing.get('registradoPorUid')!==uid||existing.get('revisionId')!==reviewId||!existing.get('fechaRealizacion').isEqual(done)||existing.get('adaptacion')!==adaptation||existing.get('observaciones')!==notes||!samePhoto||!sameNext) throw new HttpsError('already-exists','Esta revisión ya tiene otro resultado guardado. Consulta el historial.');
   return receipt(true);
  }
  if(review.get('estado')!=='PROGRAMADA'||review.get('seguimientoId')!==null||existingNext.exists) throw new HttpsError('failed-precondition','Esta revisión ya no admite un resultado nuevo.');
  if(photo!==null) {
   try {
    const file=bucket.file(photo),[metadata]=await file.getMetadata();
    if(metadata.contentType!=='image/jpeg'||Number(metadata.size)<4||Number(metadata.size)>2*1024*1024) throw Error('format');
    const [bytes]=await file.download();
    if(bytes[0]!==255||bytes[1]!==216||bytes[bytes.length-2]!==255||bytes[bytes.length-1]!==217) throw Error('jpeg');
   } catch {throw new HttpsError('failed-precondition','No se confirmó la fotografía. Vuelve a cargar una imagen JPEG válida.');}
  }
  tx.create(resultRef,{adopcionId:review.get('adopcionId'),revisionId:reviewId,perritoId:review.get('perritoId'),refugioId:shelterId,fechaRealizacion:done,adaptacion:adaptation,observaciones:notes,fotos:photo?[{archivoId:photo.slice(prefix.length,-4),ruta:photo,orden:0}]:[],registradoPorUid:uid,creadoEn:FieldValue.serverTimestamp()});
  tx.update(reviewRef,{estado:'REALIZADA',seguimientoId:reviewId});
  if(next) tx.create(nextRef,{adopcionId:review.get('adopcionId'),perritoId:review.get('perritoId'),refugioId:shelterId,fechaProgramada:nextDate,tipo:next.type,estado:'PROGRAMADA',seguimientoId:null,creadoEn:FieldValue.serverTimestamp()});
  return receipt(false);
 });
}
