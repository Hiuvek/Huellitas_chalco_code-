// Solo prepara las dos entregas del escenario hu12 en emuladores mediante las operaciones reales.
import {initializeApp,deleteApp} from 'firebase/app';
import {getAuth,connectAuthEmulator,signInWithEmailAndPassword} from 'firebase/auth';
import {getFirestore,connectFirestoreEmulator,collection,query,where,getDocs,doc,getDoc,writeBatch,serverTimestamp} from 'firebase/firestore';
const projectId='demo-huellitas-chalco',app=initializeApp({apiKey:'demo-firebase-auth-emulator-key',projectId},'hu15-seed');
const auth=getAuth(app),db=getFirestore(app);connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});connectFirestoreEmulator(db,'127.0.0.1',8180);
try {
 for(const code of ['a','b']) {
  const {user}=await signInWithEmailAndPassword(auth,`hu07-${code}@example.test`,'PruebaHU07!');
  const shelterId=`hu07-${code}`,dogId=`hu12-dog-${code}`,id=`12000000-0000-4000-8000-0000000000${code}0`;
  if(!(await getDocs(query(collection(db,'adopciones'),where('refugioId','==',shelterId),where('perritoId','==',dogId)))).empty) continue;
  const reference=doc(db,'solicitudes',id);
  for(const [previous,status,suffix] of [['Enviada','En revisión','revision'],['En revisión','Aprobada','aprobada']]) {
   const record=await getDoc(reference);if(record.data().estado!==previous) {if(record.data().estado===status) continue;throw new Error('Restablece el escenario con seed:requests antes de preparar las entregas.');}
   const batch=writeBatch(db);batch.update(reference,{estado:status,motivoNoAprobacion:null,actualizadoEn:serverTimestamp()});
   batch.set(doc(db,'historial_solicitudes',`${id}-${suffix}`),{solicitudId:id,usuarioUid:record.data().usuarioUid,refugioId:shelterId,estadoAnterior:previous,estadoNuevo:status,motivo:null,actorUid:user.uid,creadoEn:serverTimestamp()});
   if(status==='Aprobada') {batch.update(doc(db,'perritos_control',dogId),{solicitudAprobadaId:id});batch.update(doc(db,'perritos',dogId),{estado:'En proceso',actualizadoEn:serverTimestamp()});}
   await batch.commit();
  }
  const date=new Intl.DateTimeFormat('sv-SE',{timeZone:'America/Mexico_City'}).format(new Date());
  const response=await fetch(`http://127.0.0.1:5001/${projectId}/us-central1/recordAdoption`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${await user.getIdToken()}`},body:JSON.stringify({data:{requestId:id,shelterId,dogId,date,signedLetter:true}})});
  const result=await response.json();if(!result.result) throw new Error(JSON.stringify(result.error));
 }
 console.log('HU-15: entregas ficticias A/B preparadas; no se crean citas hasta programarlas.');
} finally {await deleteApp(app);}
