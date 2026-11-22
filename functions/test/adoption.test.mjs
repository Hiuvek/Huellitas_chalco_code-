import {before,after,beforeEach,afterEach,test} from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {initializeApp,deleteApp} from 'firebase-admin/app';
import {getFirestore,Timestamp} from 'firebase-admin/firestore';
const project='demo-huellitas-chalco';
assert.equal(process.env.FIRESTORE_EMULATOR_HOST,'127.0.0.1:8180','Estas pruebas solo admiten el emulador.');
const app=initializeApp({projectId:project},'hu14-test'),db=getFirestore(app);
let actors,fixture;
const today=()=>new Intl.DateTimeFormat('sv-SE',{timeZone:'America/Mexico_City'}).format(new Date());
async function login(code) {
 const response=await fetch('http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-firebase-auth-emulator-key',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:`hu07-${code}@example.test`,password:'PruebaHU07!',returnSecureToken:true})});
 const result=await response.json();assert.ok(result.idToken,JSON.stringify(result));return {uid:result.localId,token:result.idToken};
}
async function record(body,actor=actors.a) {
 const response=await fetch(`http://127.0.0.1:5001/${project}/us-central1/recordAdoption`,{method:'POST',headers:{'Content-Type':'application/json',...(actor?{Authorization:`Bearer ${actor.token}`}:{})},body:JSON.stringify({data:body})});
 return response.json();
}
const input=()=>({requestId:fixture.ids[0],dogId:fixture.dog,shelterId:'hu07-a',date:today(),signedLetter:true});
before(async()=>{actors={a:await login('a'),b:await login('b'),owner:await login('adopter')};});
after(async()=>{await db.terminate();await deleteApp(app);});
beforeEach(async()=>{
 const dog=`hu14-${randomUUID()}`,ids=Array.from({length:4},()=>randomUUID()),stamp=Timestamp.now();fixture={dog,ids,otherRequest:`hu14-other-${randomUUID()}`};
 await db.doc(`perritos/${dog}`).set({refugioId:'hu07-a',nombre:'Demo entrega',publicado:true,estado:'En proceso',actualizadoEn:stamp});
 await db.doc(`perritos_control/${dog}`).set({refugioId:'hu07-a',solicitudAprobadaId:ids[0],adopcionId:null});
 await db.doc(`solicitudes/${fixture.otherRequest}`).set({usuarioUid:actors.owner.uid,refugioId:'hu07-a',perritoId:'another-dog',estado:'Enviada'});
 for(const [index,id] of ids.entries()) await db.doc(`solicitudes/${id}`).set({usuarioUid:index===0?actors.owner.uid:`demo-owner-${index}`,refugioId:'hu07-a',perritoId:dog,estado:['Aprobada','Enviada','En revisión','No aprobada'][index],motivoNoAprobacion:index===3?'Decisión anterior':null,creadoEn:stamp,actualizadoEn:stamp});
});
afterEach(async()=>{
 const batch=db.batch();
 for(const path of [`perritos/${fixture.dog}`,`perritos_control/${fixture.dog}`,`adopciones/${fixture.dog}`,`solicitudes/${fixture.otherRequest}`,...fixture.ids.map(id=>`solicitudes/${id}`),...fixture.ids.map(id=>`historial_solicitudes/${id}-no-aprobada`)]) batch.delete(db.doc(path));
 await batch.commit();
});
test('HU14 registra entrega y cierra todas las pendientes con historial sin alterar decisiones previas',async()=>{
 const response=await record(input());assert.ok(response.result,JSON.stringify(response));assert.equal(response.result.id,fixture.dog);
 assert.equal((await db.doc(`solicitudes/${fixture.otherRequest}`).get()).get('estado'),'Enviada');
 const adoption=await db.doc(`adopciones/${fixture.dog}`).get();assert.equal(adoption.get('solicitudId'),fixture.ids[0]);assert.equal(adoption.get('usuarioUid'),actors.owner.uid);assert.equal(adoption.get('cartaFirmada'),true);assert.ok(adoption.get('creadoEn'));
 assert.equal((await db.doc(`perritos/${fixture.dog}`).get()).get('estado'),'Adoptado');
 assert.equal((await db.doc(`perritos_control/${fixture.dog}`).get()).get('adopcionId'),fixture.dog);
 for(const [index,id] of fixture.ids.entries()) {
  const request=await db.doc(`solicitudes/${id}`).get();assert.equal(request.get('estado'),index===0?'Aprobada':'No aprobada');
  if(index===1||index===2) {assert.equal(request.get('motivoNoAprobacion'),'El perrito fue adoptado por otra persona.');assert.equal((await db.doc(`historial_solicitudes/${id}-no-aprobada`).get()).get('estadoAnterior'),index===1?'Enviada':'En revisión');}
  if(index===3) assert.equal(request.get('motivoNoAprobacion'),'Decisión anterior');
 }
});
test('HU14 reintento y dos llamadas simultáneas conservan una adopción y sus fechas',async()=>{
 const responses=await Promise.all([record(input()),record(input())]);assert.ok(responses.every(value=>value.result),JSON.stringify(responses));
 assert.equal(responses.filter(value=>value.result.alreadyExisting===false).length,1);
 const ref=db.doc(`adopciones/${fixture.dog}`),before=(await ref.get()).get('creadoEn').toMillis();
 assert.equal((await record(input())).result.alreadyExisting,true);assert.equal((await ref.get()).get('creadoEn').toMillis(),before);
 const changed=await record({...input(),date:'2026-01-01'});assert.equal(changed.error.status,'ALREADY_EXISTS');
});
test('HU14 visitante adoptante y otro refugio no registran entregas',async()=>{
 for(const actor of [null,actors.owner,actors.b]) {const response=await record(input(),actor);assert.ok(['UNAUTHENTICATED','PERMISSION_DENIED'].includes(response.error?.status),JSON.stringify(response));}
 assert.equal((await db.doc(`adopciones/${fixture.dog}`).get()).exists,false);
});
test('HU14 fechas imposibles futuras y falta de carta no dejan cambios parciales',async()=>{
 for(const patch of [{date:'2026-02-30'},{date:'2999-01-01'},{date:'mal'},{signedLetter:false}]) assert.equal((await record({...input(),...patch})).error?.status,'INVALID_ARGUMENT');
 assert.equal((await db.doc(`adopciones/${fixture.dog}`).get()).exists,false);assert.equal((await db.doc(`perritos/${fixture.dog}`).get()).get('estado'),'En proceso');
});
test('HU14 solicitud sin aprobación control ajeno o perrito incorrecto bloquean la operación',async()=>{
 assert.equal((await record({...input(),requestId:fixture.ids[1]})).error?.status,'FAILED_PRECONDITION');
 await db.doc(`perritos_control/${fixture.dog}`).update({solicitudAprobadaId:'otra'});
 assert.equal((await record(input())).error?.status,'FAILED_PRECONDITION');
 await db.doc(`perritos_control/${fixture.dog}`).update({refugioId:'hu07-b'});
 assert.equal((await record(input())).error?.status,'PERMISSION_DENIED');
 assert.equal((await db.doc(`adopciones/${fixture.dog}`).get()).exists,false);
});
test('HU14 revocar membresía bloquea la función incluso con token de sesión válido',async()=>{
 const ref=db.doc(`membresias/${actors.a.uid}__hu07-a`);
 try {await ref.update({activo:false});assert.equal((await record(input())).error?.status,'PERMISSION_DENIED');}
 finally {await ref.update({activo:true});}
});
