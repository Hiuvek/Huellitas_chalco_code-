import {before,after,beforeEach,afterEach,test} from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {initializeApp,deleteApp} from 'firebase-admin/app';
import {getFirestore,Timestamp} from 'firebase-admin/firestore';
import {getStorage} from 'firebase-admin/storage';
import {nextReviewId} from '../src/record-followup.js';
assert.equal(process.env.FIRESTORE_EMULATOR_HOST,'127.0.0.1:8180');
assert.equal(process.env.FIREBASE_STORAGE_EMULATOR_HOST,'127.0.0.1:9199');
const project='demo-huellitas-chalco',app=initializeApp({projectId:project},'hu16-tests'),db=getFirestore(app),bucket=getStorage(app).bucket(`${project}.appspot.com`);
let actors,fixture;
async function login(code) {const r=await fetch('http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-firebase-auth-emulator-key',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:`hu07-${code}@example.test`,password:'PruebaHU07!',returnSecureToken:true})});const d=await r.json();assert.ok(d.idToken);return {uid:d.localId,token:d.idToken};}
async function record(data,actor=actors.a) {const r=await fetch(`http://127.0.0.1:5001/${project}/us-central1/recordFollowUp`,{method:'POST',headers:{'Content-Type':'application/json',...(actor?{Authorization:`Bearer ${actor.token}`}:{})},body:JSON.stringify({data})});return r.json();}
const input=()=>({reviewId:fixture.id,shelterId:'hu07-a',date:'2026-11-21',adaptation:'BUENA',notes:'Se adapta bien a las rutinas de su hogar.',photo:null,next:null});
before(async()=>{actors={a:await login('a'),b:await login('b'),owner:await login('adopter')};});
after(async()=>{await db.terminate();await deleteApp(app);});
beforeEach(async()=>{fixture={id:randomUUID(),adoption:`hu16-${randomUUID()}`,photo:null};await db.doc(`adopciones/${fixture.adoption}`).set({solicitudId:'demo-request',perritoId:fixture.adoption,usuarioUid:actors.owner.uid,refugioId:'hu07-a',fechaEntrega:Timestamp.fromDate(new Date('2026-11-20T00:00:00-06:00')),cartaFirmada:true,registradoPorUid:actors.a.uid,creadoEn:Timestamp.now()});await db.doc(`revisiones/${fixture.id}`).set({adopcionId:fixture.adoption,perritoId:fixture.adoption,refugioId:'hu07-a',fechaProgramada:Timestamp.fromDate(new Date('2026-11-22T00:00:00-06:00')),tipo:'LLAMADA',estado:'PROGRAMADA',seguimientoId:null,creadoEn:Timestamp.now()});});
afterEach(async()=>{for(const path of [`revisiones/${fixture.id}`,`revisiones/${nextReviewId(fixture.id)}`,`seguimientos/${fixture.id}`,`adopciones/${fixture.adoption}`]) await db.doc(path).delete();if(fixture.photo) await bucket.file(fixture.photo).delete({ignoreNotFound:true});});
test('HU16 guarda resultado real y marca realizada conservando la fecha programada',async()=>{
 const r=await record(input());assert.ok(r.result,JSON.stringify(r));
 const doc=await db.doc(`seguimientos/${fixture.id}`).get(),review=await db.doc(`revisiones/${fixture.id}`).get();
 assert.equal(doc.get('revisionId'),fixture.id);assert.equal(doc.get('observaciones'),input().notes);assert.equal(doc.get('registradoPorUid'),actors.a.uid);assert.ok(doc.get('creadoEn'));assert.deepEqual(doc.get('fotos'),[]);assert.equal(review.get('estado'),'REALIZADA');assert.equal(review.get('seguimientoId'),fixture.id);assert.equal(review.get('fechaProgramada').toDate().toISOString(),'2026-11-22T06:00:00.000Z');assert.equal((await db.doc(`revisiones/${nextReviewId(fixture.id)}`).get()).exists,false);
});
test('HU16 siguiente revisión se crea atómicamente y reintento simultáneo no duplica ni cambia fechas',async()=>{
 const data={...input(),next:{date:'2026-12-01',type:'VISITA'}};
 const results=await Promise.all([record(data),record(data)]);assert.ok(results.every(r=>r.result),JSON.stringify(results));assert.equal(results.filter(r=>!r.result.alreadyExisting).length,1);
 const before=(await db.doc(`seguimientos/${fixture.id}`).get()).get('creadoEn').toMillis();assert.equal((await record(data)).result.alreadyExisting,true);assert.equal((await db.doc(`seguimientos/${fixture.id}`).get()).get('creadoEn').toMillis(),before);
 const next=await db.doc(`revisiones/${nextReviewId(fixture.id)}`).get();assert.equal(next.get('estado'),'PROGRAMADA');assert.equal(next.get('tipo'),'VISITA');assert.equal(next.get('adopcionId'),fixture.adoption);
 assert.equal((await record({...data,notes:'Otra descripción suficientemente extensa.'})).error?.status,'ALREADY_EXISTS');assert.equal((await record(input())).error?.status,'ALREADY_EXISTS');
});
test('HU16 rechaza fechas inválidas futuras anteriores a entrega y datos insuficientes sin guardar nada',async()=>{
 for(const patch of [{date:'2026-02-30'},{date:'2099-01-01'},{date:'2026-11-19'},{adaptation:'EXCELENTE'},{notes:'Corto'},{next:{date:'2026-11-21',type:'VISITA'}},{next:{date:'2026-12-01',type:'OTRO'}}]) assert.equal((await record({...input(),...patch})).error?.status,'INVALID_ARGUMENT');
 assert.equal((await db.doc(`seguimientos/${fixture.id}`).get()).exists,false);assert.equal((await db.doc(`revisiones/${fixture.id}`).get()).get('estado'),'PROGRAMADA');
});
test('HU16 visitante adoptante y otro refugio no guardan resultados privados',async()=>{
 for(const actor of [null,actors.owner,actors.b]) assert.ok(['UNAUTHENTICATED','PERMISSION_DENIED'].includes((await record(input(),actor)).error?.status));assert.equal((await db.doc(`seguimientos/${fixture.id}`).get()).exists,false);
});
test('HU16 foto debe existir ser JPEG y pertenecer a la revisión',async()=>{
 fixture.photo=`refugios/hu07-a/seguimientos/${fixture.id}/${randomUUID()}.jpg`;
 assert.equal((await record({...input(),photo:'refugios/hu07-b/otra.jpg'})).error?.status,'INVALID_ARGUMENT');
 assert.equal((await record({...input(),photo:fixture.photo})).error?.status,'FAILED_PRECONDITION');
 await bucket.file(fixture.photo).save(Buffer.from('no es jpeg'),{metadata:{contentType:'image/jpeg'}});assert.equal((await record({...input(),photo:fixture.photo})).error?.status,'FAILED_PRECONDITION');
 assert.equal((await db.doc(`seguimientos/${fixture.id}`).get()).exists,false);
 // JPEG mínimo de prueba; el cliente de Android comprueba y recodifica las imágenes reales.
 await bucket.file(fixture.photo).save(Buffer.from([255,216,255,217]),{metadata:{contentType:'image/jpeg'}});
 assert.ok((await record({...input(),photo:fixture.photo})).result);assert.equal((await db.doc(`seguimientos/${fixture.id}`).get()).get('fotos')[0].ruta,fixture.photo);
});
test('HU16 revisión inexistente o ya realizada no permite inventar un resultado',async()=>{
 assert.equal((await record({...input(),reviewId:'missing'})).error?.status,'FAILED_PRECONDITION');
 await db.doc(`revisiones/${fixture.id}`).update({estado:'REALIZADA',seguimientoId:'otro'});assert.equal((await record(input())).error?.status,'FAILED_PRECONDITION');
});
