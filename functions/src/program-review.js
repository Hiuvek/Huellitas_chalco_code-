import {FieldValue} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
import {civilTimestamp,requireId,requireMembership} from './shared.js';
export const reviewTypes=['VISITA','LLAMADA','VIDEOLLAMADA'];
export async function programReviewOperation(db,uid,data) {
 if(!uid) throw new HttpsError('unauthenticated','Inicia sesión para programar una revisión.');
 const {id,adoptionId,shelterId,date,type}=data??{};
 for(const value of [id,adoptionId,shelterId]) requireId(value);
 if(!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(id)||!reviewTypes.includes(type)) throw new HttpsError('invalid-argument','Selecciona un tipo válido de revisión.');
 const planned=civilTimestamp(date);
 return db.runTransaction(async tx=>{
  await requireMembership(tx,db,uid,shelterId);
  const adoptionRef=db.doc(`adopciones/${adoptionId}`),reviewRef=db.doc(`revisiones/${id}`);
  const [adoption,existing]=await tx.getAll(adoptionRef,reviewRef);
  if(!adoption.exists) throw new HttpsError('failed-precondition','Registra la entrega antes de programar revisiones.');
  if(adoption.get('refugioId')!==shelterId||adoption.get('perritoId')!==adoptionId) throw new HttpsError('permission-denied','La adopción no pertenece al refugio.');
  if(planned.toMillis()<adoption.get('fechaEntrega').toMillis()) throw new HttpsError('invalid-argument','La revisión no puede ser anterior a la entrega.');
  if(existing.exists) {
   if(existing.get('adopcionId')!==adoptionId||existing.get('refugioId')!==shelterId||existing.get('tipo')!==type||!existing.get('fechaProgramada').isEqual(planned)) throw new HttpsError('already-exists','Este intento ya programó otra revisión.');
   return {id,adoptionId,shelterId,date,type,alreadyExisting:true};
  }
  tx.create(reviewRef,{adopcionId:adoptionId,perritoId:adoptionId,refugioId:shelterId,fechaProgramada:planned,tipo:type,estado:'PROGRAMADA',seguimientoId:null,creadoEn:FieldValue.serverTimestamp()});
  return {id,adoptionId,shelterId,date,type,alreadyExisting:false};
 });
}
