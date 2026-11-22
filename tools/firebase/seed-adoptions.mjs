// Solo prepara las dos entregas del escenario hu12 en emuladores mediante las operaciones reales.
import {initializeApp,deleteApp} from 'firebase/app';
import {getAuth,connectAuthEmulator,signInWithEmailAndPassword} from 'firebase/auth';
import {getFirestore,connectFirestoreEmulator,collection,query,where,getDocs,doc,getDoc,writeBatch,serverTimestamp,Timestamp} from 'firebase/firestore';
const projectId='demo-huellitas-chalco',app=initializeApp({apiKey:'demo-firebase-auth-emulator-key',projectId},'hu15-seed');
const auth=getAuth(app),db=getFirestore(app);connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});connectFirestoreEmulator(db,'127.0.0.1',8180);
async function closeOthers(shelterId,dogId,requestId,uid) {
 const all=await getDocs(query(collection(db,'solicitudes'),where('refugioId','==',shelterId),where('perritoId','==',dogId)));
 for(const other of all.docs) {
  const old=other.data();if(other.id===requestId||!['Enviada','En revisión'].includes(old.estado))continue;
  const reason='El perrito fue adoptado por otra persona.',b=writeBatch(db);
  b.update(other.ref,{estado:'No aprobada',motivoNoAprobacion:reason,actualizadoEn:serverTimestamp()});
  b.set(doc(db,'historial_solicitudes',other.id+'-no-aprobada'),{solicitudId:other.id,usuarioUid:old.usuarioUid,refugioId:shelterId,estadoAnterior:old.estado,estadoNuevo:'No aprobada',motivo:reason,actorUid:uid,creadoEn:serverTimestamp()});
  await b.commit();
 }
}
try {
 for(const code of ['a','b']) {
  const {user}=await signInWithEmailAndPassword(auth,`hu07-${code}@example.test`,'PruebaHU07!');
  const shelterId=`hu07-${code}`,dogId=`hu12-dog-${code}`,id=`12000000-0000-4000-8000-0000000000${code}0`;
  if(!(await getDocs(query(collection(db,'adopciones'),where('refugioId','==',shelterId),where('perritoId','==',dogId)))).empty) {await closeOthers(shelterId,dogId,id,user.uid);continue;}
  const reference=doc(db,'solicitudes',id);
  for(const [previous,status,suffix] of [['Enviada','En revisión','revision'],['En revisión','Aprobada','aprobada']]) {
   const record=await getDoc(reference);if(record.data().estado!==previous) {if(record.data().estado===status) continue;throw new Error('Restablece el escenario con seed:requests antes de preparar las entregas.');}
   const batch=writeBatch(db);batch.update(reference,{estado:status,motivoNoAprobacion:null,actualizadoEn:serverTimestamp()});
   batch.set(doc(db,'historial_solicitudes',`${id}-${suffix}`),{solicitudId:id,usuarioUid:record.data().usuarioUid,refugioId:shelterId,estadoAnterior:previous,estadoNuevo:status,motivo:null,actorUid:user.uid,creadoEn:serverTimestamp()});
   if(status==='Aprobada') {batch.update(doc(db,'perritos_control',dogId),{solicitudAprobadaId:id});batch.update(doc(db,'perritos',dogId),{estado:'En proceso',actualizadoEn:serverTimestamp()});}
   await batch.commit();
  }
  const batch=writeBatch(db);
  batch.set(doc(db,'adopciones',dogId),{solicitudId:id,perritoId:dogId,usuarioUid:(await getDoc(reference)).data().usuarioUid,refugioId:shelterId,fechaEntrega:Timestamp.fromDate(new Date('2026-11-20T06:00:00Z')),cartaFirmada:true,registradoPorUid:user.uid,creadoEn:serverTimestamp()});
  batch.update(doc(db,'perritos',dogId),{estado:'Adoptado',actualizadoEn:serverTimestamp()});
  batch.update(doc(db,'perritos_control',dogId),{adopcionId:dogId});
  await batch.commit();
  await closeOthers(shelterId,dogId,id,user.uid);
 }
 console.log('HU-15: entregas ficticias A/B preparadas; no se crean citas hasta programarlas.');
} finally {await deleteApp(app);}
