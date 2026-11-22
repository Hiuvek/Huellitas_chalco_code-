import {before,after,beforeEach,test} from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {createHash,randomUUID} from 'node:crypto';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,setDoc,getDoc,updateDoc,writeBatch,serverTimestamp,Timestamp,runTransaction} from 'firebase/firestore';
let env;
const day=Timestamp.fromDate(new Date('2026-11-01T06:00:00Z'));
const sha=s=>createHash('sha256').update(s).digest('hex');
const nextId=sha('review\0next');
const as=uid=>uid?env.authenticatedContext(uid).firestore():env.unauthenticatedContext().firestore();
const admin=callback=>env.withSecurityRulesDisabled(ctx=>callback(ctx.firestore()));
const ref=(db,path)=>doc(db,path);
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-huellitas-chalco',firestore:{host:'127.0.0.1',port:8180,rules:await readFile(new URL('../../../firestore.rules',import.meta.url),'utf8')}})});
after(async()=>env?.cleanup());
beforeEach(async()=>{
 await env.clearFirestore();
 await admin(async db=>{
  const data={
   'usuarios/a':{activo:true},'usuarios/b':{activo:true},'usuarios/owner':{activo:true},
   'refugios/a':{activo:true},'refugios/b':{activo:true},
   'membresias/a__a':{usuarioUid:'a',refugioId:'a',activo:true},'membresias/b__b':{usuarioUid:'b',refugioId:'b',activo:true},
   'perritos/dog':{refugioId:'a',estado:'En proceso',publicado:true,fotos:[{ruta:'perritos/a/a/dog/photo.jpg'}]},
   'perritos_control/dog':{refugioId:'a',solicitudAprobadaId:'application',adopcionId:null},
   'solicitudes/application':{refugioId:'a',perritoId:'dog',usuarioUid:'owner',estado:'Aprobada'},
  };await Promise.all(Object.entries(data).map(([p,d])=>setDoc(ref(db,p),d)));
 });
});
function adoption(db,patch={},omit=''){
 const b=writeBatch(db);
 b.set(ref(db,'adopciones/dog'),{solicitudId:'application',perritoId:'dog',usuarioUid:'owner',refugioId:'a',fechaEntrega:day,cartaFirmada:true,registradoPorUid:'a',creadoEn:serverTimestamp(),...patch});
 if(omit!=='dog')b.update(ref(db,'perritos/dog'),{estado:'Adoptado',actualizadoEn:serverTimestamp()});
 if(omit!=='control')b.update(ref(db,'perritos_control/dog'),{adopcionId:'dog'});
 return b.commit();
}
async function delivered(){await assertSucceeds(adoption(as('a')))}
async function review(){await delivered();await admin(db=>setDoc(ref(db,'revisiones/review'),{adopcionId:'dog',perritoId:'dog',refugioId:'a',fechaProgramada:day,tipo:'VISITA',estado:'PROGRAMADA',seguimientoId:null,creadoEn:day}));}
function reviewData(patch={}){return {adopcionId:'dog',perritoId:'dog',refugioId:'a',fechaProgramada:day,tipo:'VISITA',estado:'PROGRAMADA',seguimientoId:null,creadoEn:serverTimestamp(),...patch}}
function followup(db,patch={},options={}){
 const b=writeBatch(db);
 b.set(ref(db,'seguimientos/review'),{adopcionId:'dog',revisionId:'review',perritoId:'dog',refugioId:'a',fechaRealizacion:day,adaptacion:'BUENA',observaciones:'Se adapta bien al nuevo hogar.',fotos:[],registradoPorUid:'a',siguienteRevisionId:options.next?nextId:null,creadoEn:serverTimestamp(),...patch});
 if(!options.omit)b.update(ref(db,'revisiones/review'),{estado:'REALIZADA',seguimientoId:'review'});
 if(options.next)b.set(ref(db,`revisiones/${nextId}`),reviewData({fechaProgramada:Timestamp.fromMillis(day.toMillis()+86400000),revisionOrigenId:'review',...options.next}));
 return b.commit();
}
test('Spark entrega requiere los tres documentos, responsable, carta y fecha válida',async()=>{
 for(const omit of ['dog','control'])await assertFails(adoption(as('a'),{},omit));
 for(const patch of [{cartaFirmada:false},{usuarioUid:'b'},{refugioId:'b'},{registradoPorUid:'owner'},{fechaEntrega:Timestamp.fromMillis(Date.now()+86400000)},{perritoId:'otro'},{extra:true}])await assertFails(adoption(as('a'),patch));
 for(const uid of ['b','owner',null])await assertFails(adoption(as(uid)));
 await delivered();
 await assertFails(adoption(as('a')));
 await assertFails(updateDoc(ref(as('a'),'adopciones/dog'),{cartaFirmada:false}));
});
test('Spark entrega no admite solicitud no aprobada o control distinto',async()=>{
 await admin(db=>updateDoc(ref(db,'solicitudes/application'),{estado:'En revisión'}));await assertFails(adoption(as('a')));
 await admin(db=>updateDoc(ref(db,'solicitudes/application'),{estado:'Aprobada'}));
 await admin(db=>updateDoc(ref(db,'perritos_control/dog'),{solicitudAprobadaId:'otra'}));await assertFails(adoption(as('a')));
});
test('Spark transacciones concurrentes solo crean una adopción',async()=>{
 const db=as('a');
 const run=()=>runTransaction(db,async tx=>{
  const a=await tx.get(ref(db,'adopciones/dog'));
  if(a.exists())return 'existing';
  tx.set(a.ref,{solicitudId:'application',perritoId:'dog',usuarioUid:'owner',refugioId:'a',fechaEntrega:day,cartaFirmada:true,registradoPorUid:'a',creadoEn:serverTimestamp()});
  tx.update(ref(db,'perritos/dog'),{estado:'Adoptado',actualizadoEn:serverTimestamp()});tx.update(ref(db,'perritos_control/dog'),{adopcionId:'dog'});return 'created';
 });
 const results=await Promise.allSettled([run(),run()]);assert.equal(results.filter(r=>r.status==='fulfilled' && r.value==='created').length,1);
 assert.equal((await getDoc(ref(db,'adopciones/dog'))).get('solicitudId'),'application');
 assert.equal(await run(),'existing');
});
test('Spark cierre de pendientes requiere adopción y conserva historial privado',async()=>{
 await admin(db=>setDoc(ref(db,'solicitudes/other'),{refugioId:'a',perritoId:'dog',usuarioUid:'other',estado:'Enviada'}));
 const close=()=>{const db=as('a'),b=writeBatch(db),reason='El perrito fue adoptado por otra persona.';
 b.update(ref(db,'solicitudes/other'),{estado:'No aprobada',motivoNoAprobacion:reason,actualizadoEn:serverTimestamp()});
 b.set(ref(db,'historial_solicitudes/other-no-aprobada'),{solicitudId:'other',usuarioUid:'other',refugioId:'a',estadoAnterior:'Enviada',estadoNuevo:'No aprobada',motivo:reason,actorUid:'a',creadoEn:serverTimestamp()});return b.commit();};
 await assertFails(close());await delivered();await assertSucceeds(close());
 await assertFails(getDoc(ref(as('owner'),'historial_solicitudes/other-no-aprobada')));
});
test('Spark revisión requiere adopción y rechaza campos, fechas o refugios incorrectos',async()=>{
 const db=as('a');const save=(patch={},uid='a')=>setDoc(ref(as(uid),`revisiones/${randomUUID()}`),reviewData(patch));
 await assertFails(save());await delivered();
 for(const p of [{tipo:'OTRO'},{fechaProgramada:Timestamp.fromMillis(0)},{adopcionId:'otra'},{perritoId:'otra'},{refugioId:'b'},{estado:'REALIZADA'},{revisionOrigenId:'inventada'}])await assertFails(save(p));
 await assertFails(save({},'b'));await assertFails(save({},'owner'));await assertSucceeds(save());
});
test('Spark seguimiento y siguiente revisión se guardan juntos',async()=>{
 await review();await assertSucceeds(followup(as('a'),{}, {next:{}}));
 await assertSucceeds(getDoc(ref(as('a'),`revisiones/${nextId}`)));
 await assertFails(getDoc(ref(as('owner'),'seguimientos/review')));
 await assertFails(followup(as('a')));
});
test('Spark seguimiento rechaza falta de cierre, actor falso, datos ajenos y fotos inexistentes',async()=>{
 await review();
 await assertFails(followup(as('a'),{}, {omit:true}));
 for(const p of [{registradoPorUid:'owner'},{adaptacion:'inventada'},{observaciones:'corto'},{fechaRealizacion:Timestamp.fromMillis(0)},{fechaRealizacion:Timestamp.fromMillis(Date.now()+86400000)},{refugioId:'b'},{siguienteRevisionId:nextId},{fotos:[{archivoId:'a',ruta:'refugios/a/seguimientos/review/a.jpg',orden:0}]}])await assertFails(followup(as('a'),p));
 await assertFails(followup(as('b')));await assertFails(followup(as('owner')));
 await assertSucceeds(followup(as('a')));
});
function image(path='perritos/a/a/dog/photo.jpg',patch={}){return {ruta:path,refugioId:'a',perritoId:'dog',revisionId:null,datos:'/9j/AAAA/9k=',...patch}}
test('Spark imágenes limitadas, inmutables y aisladas por refugio',async()=>{
 const path='perritos/a/a/dog/photo.jpg';const id=sha(path);const save=(d,uid='a')=>setDoc(ref(as(uid),`imagenes/${id}`),d);
 await assertFails(save(image(path,{datos:'A'.repeat(140000)})));
 await assertFails(save(image(path,{datos:'texto'})));
 await assertFails(save(image(),'b'));await assertFails(save(image(),'owner'));
 await assertSucceeds(save(image()));await assertSucceeds(getDoc(ref(as(),`imagenes/${id}`)));
 await assertFails(updateDoc(ref(as('a'),`imagenes/${id}`),{datos:'/9j/BBBB/9k='}));
 await delivered();await assertFails(getDoc(ref(as(),`imagenes/${id}`)));
 await assertSucceeds(getDoc(ref(as('owner'),`imagenes/${id}`)));
});
test('Spark foto retirada deja de ser pública aunque el perrito siga publicado',async()=>{
 const path='perritos/a/a/dog/photo.jpg',id=sha(path);
 await assertSucceeds(setDoc(ref(as('a'),`imagenes/${id}`),image()));
 await assertSucceeds(getDoc(ref(as(),`imagenes/${id}`)));
 await admin(db=>updateDoc(ref(db,'perritos/dog'),{fotos:[{ruta:'perritos/a/a/dog/nueva.jpg'}]}));
 await assertFails(getDoc(ref(as(),`imagenes/${id}`)));
 await assertSucceeds(getDoc(ref(as('a'),`imagenes/${id}`)));
});
test('Spark evidencia del seguimiento solo es visible al refugio',async()=>{
 await review();const path='refugios/a/seguimientos/review/photo.jpg',id=sha(path);
 await assertSucceeds(setDoc(ref(as('a'),`imagenes/${id}`),image(path,{perritoId:null,revisionId:'review'})));
 for(const uid of [null,'b','owner'])await assertFails(getDoc(ref(as(uid),`imagenes/${id}`)));
 await assertSucceeds(followup(as('a'),{fotos:[{archivoId:'photo',ruta:path,orden:0}]}));
});
