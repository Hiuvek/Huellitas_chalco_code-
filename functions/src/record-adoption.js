import {FieldValue} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
import {civilTimestamp,requireMembership} from './shared.js';
export const adoptedElsewhereReason='El perrito fue adoptado por otra persona.';
export function deliveryTimestamp(value,now=Date.now()) {
 const date=civilTimestamp(value);
 if(date.toMillis()>now) throw new HttpsError('invalid-argument','La fecha de entrega no puede estar en el futuro.');
 return date;
}
export async function recordAdoptionOperation(db,uid,data) {
 if(!uid) throw new HttpsError('unauthenticated','Inicia sesión para registrar la entrega.');
 const {requestId,shelterId,dogId,date,signedLetter}=data??{};
 for(const id of [requestId,shelterId,dogId]) if(typeof id!=='string'||!id.match(/^[A-Za-z0-9_-]{1,100}$/)) throw new HttpsError('invalid-argument','El registro no es válido.');
 if(signedLetter!==true) throw new HttpsError('invalid-argument','Confirma la entrega y la carta de adopción firmada.');
 const delivery=deliveryTimestamp(date);
 return db.runTransaction(async tx=>{
  await requireMembership(tx,db,uid,shelterId);
  const adoptionRef=db.doc(`adopciones/${dogId}`),requestRef=db.doc(`solicitudes/${requestId}`),dogRef=db.doc(`perritos/${dogId}`),controlRef=db.doc(`perritos_control/${dogId}`);
  const [adoption,application,dog,control]=await tx.getAll(adoptionRef,requestRef,dogRef,controlRef);
  if(application.get('refugioId')!==shelterId||application.get('perritoId')!==dogId||dog.get('refugioId')!==shelterId||control.get('refugioId')!==shelterId) throw new HttpsError('permission-denied','La solicitud y el perrito deben pertenecer al refugio.');
  if(adoption.exists) {
   if(adoption.get('solicitudId')===requestId&&adoption.get('fechaEntrega').isEqual(delivery)&&adoption.get('registradoPorUid')===uid) return {id:dogId,requestId,dogId,shelterId,date,alreadyExisting:true};
   throw new HttpsError('already-exists','Este perrito ya tiene una adopción registrada.');
  }
  if(application.get('estado')!=='Aprobada'||dog.get('estado')!=='En proceso'||control.get('solicitudAprobadaId')!==requestId||control.get('adopcionId')!==null) throw new HttpsError('failed-precondition','Se necesita la solicitud aprobada y un perrito En proceso sin adopción previa.');
  const applications=await tx.get(db.collection('solicitudes').where('refugioId','==',shelterId).where('perritoId','==',dogId));
  const pending=applications.docs.filter(doc=>doc.id!==requestId&&['Enviada','En revisión'].includes(doc.get('estado')));
  if(applications.docs.some(doc=>doc.id!==requestId&&doc.get('estado')==='Aprobada')) throw new HttpsError('failed-precondition','El registro tiene más de una aprobación. Solicita su revisión.');
  if(pending.length>200) throw new HttpsError('resource-exhausted','Hay demasiadas solicitudes para una sola entrega. No se guardó ningún cambio.');
  tx.create(adoptionRef,{solicitudId:requestId,perritoId:dogId,usuarioUid:application.get('usuarioUid'),refugioId:shelterId,fechaEntrega:delivery,cartaFirmada:true,registradoPorUid:uid,creadoEn:FieldValue.serverTimestamp()});
  tx.update(dogRef,{estado:'Adoptado',actualizadoEn:FieldValue.serverTimestamp()});
  tx.update(controlRef,{adopcionId:dogId});
  for(const other of pending) {
   tx.update(other.ref,{estado:'No aprobada',motivoNoAprobacion:adoptedElsewhereReason,actualizadoEn:FieldValue.serverTimestamp()});
   tx.create(db.doc(`historial_solicitudes/${other.id}-no-aprobada`),{solicitudId:other.id,usuarioUid:other.get('usuarioUid'),refugioId:shelterId,estadoAnterior:other.get('estado'),estadoNuevo:'No aprobada',motivo:adoptedElsewhereReason,actorUid:uid,creadoEn:FieldValue.serverTimestamp()});
  }
  return {id:dogId,requestId,dogId,shelterId,date,alreadyExisting:false};
 });
}
