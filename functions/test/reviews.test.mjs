import {before,after,beforeEach,afterEach,test} from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {initializeApp,deleteApp} from 'firebase-admin/app';
import {getFirestore,Timestamp} from 'firebase-admin/firestore';
assert.equal(process.env.FIRESTORE_EMULATOR_HOST,'127.0.0.1:8180');
const project='demo-huellitas-chalco',app=initializeApp({projectId:project},'hu15-tests'),db=getFirestore(app);
let actors,fixture;
async function login(code) {
 const response=await fetch('http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-firebase-auth-emulator-key',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:`hu07-${code}@example.test`,password:'PruebaHU07!',returnSecureToken:true})});const result=await response.json();assert.ok(result.idToken);return {uid:result.localId,token:result.idToken};
}
async function program(data,actor=actors.a) {const response=await fetch(`http://127.0.0.1:5001/${project}/us-central1/programReview`,{method:'POST',headers:{'Content-Type':'application/json',...(actor?{Authorization:`Bearer ${actor.token}`}:{})},body:JSON.stringify({data})});return response.json();}
const input=()=>({id:fixture.review,adoptionId:fixture.adoption,shelterId:'hu07-a',date:'2026-12-01',type:'VISITA'});
before(async()=>{actors={a:await login('a'),b:await login('b'),owner:await login('adopter')};});
after(async()=>{await db.terminate();await deleteApp(app);});
beforeEach(async()=>{fixture={adoption:`hu15-${randomUUID()}`,review:randomUUID()};await db.doc(`adopciones/${fixture.adoption}`).set({solicitudId:'demo-request',perritoId:fixture.adoption,usuarioUid:actors.owner.uid,refugioId:'hu07-a',fechaEntrega:Timestamp.fromDate(new Date('2026-11-21T00:00:00-06:00')),cartaFirmada:true,registradoPorUid:actors.a.uid,creadoEn:Timestamp.now()});});
afterEach(async()=>{await db.doc(`revisiones/${fixture.review}`).delete();await db.doc(`adopciones/${fixture.adoption}`).delete();});
test('HU15 guarda una cita con fecha tipo relaciones y auditoría sin inventar resultado',async()=>{
 const response=await program(input());assert.ok(response.result,JSON.stringify(response));
 const doc=await db.doc(`revisiones/${fixture.review}`).get();assert.equal(doc.get('estado'),'PROGRAMADA');assert.equal(doc.get('seguimientoId'),null);assert.equal(doc.get('tipo'),'VISITA');assert.equal(doc.get('adopcionId'),fixture.adoption);assert.ok(doc.get('creadoEn'));
});
test('HU15 reintentos simultáneos conservan una cita y sus fechas',async()=>{
 const response=await Promise.all([program(input()),program(input())]);assert.ok(response.every(item=>item.result),JSON.stringify(response));assert.equal(response.filter(item=>!item.result.alreadyExisting).length,1);
 const before=(await db.doc(`revisiones/${fixture.review}`).get()).get('creadoEn').toMillis();assert.ok((await program(input())).result.alreadyExisting);assert.equal((await db.doc(`revisiones/${fixture.review}`).get()).get('creadoEn').toMillis(),before);
 assert.equal((await program({...input(),type:'LLAMADA'})).error?.status,'ALREADY_EXISTS');
});
test('HU15 fechas anteriores a entrega fechas imposibles y tipos ajenos se rechazan',async()=>{
 for(const patch of [{date:'2026-11-20'},{date:'2026-02-30'},{type:'OTRO'}]) assert.equal((await program({...input(),...patch})).error?.status,'INVALID_ARGUMENT');
 assert.equal((await db.doc(`revisiones/${fixture.review}`).get()).exists,false);
});
test('HU15 visitante adoptante y otro refugio no programan',async()=>{
 for(const actor of [null,actors.owner,actors.b]) assert.ok(['UNAUTHENTICATED','PERMISSION_DENIED'].includes((await program(input(),actor)).error?.status));
});
test('HU15 adopción inexistente o ajena no permite crear una cita',async()=>{
 assert.equal((await program({...input(),adoptionId:'missing'})).error?.status,'FAILED_PRECONDITION');
 await db.doc(`adopciones/${fixture.adoption}`).update({refugioId:'hu07-b'});assert.equal((await program(input())).error?.status,'PERMISSION_DENIED');
});
