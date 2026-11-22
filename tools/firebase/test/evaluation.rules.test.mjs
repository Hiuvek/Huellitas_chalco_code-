import {before,after,beforeEach,test} from 'node:test';
import {readFile} from 'node:fs/promises';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,setDoc,getDoc,updateDoc,deleteDoc,writeBatch,serverTimestamp,Timestamp} from 'firebase/firestore';
import assert from 'node:assert/strict';
let env;
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-huellitas-chalco',firestore:{host:'127.0.0.1',port:8180,rules:await readFile(new URL('../../../firestore.rules',import.meta.url),'utf8')}});});
after(async()=>{await env?.cleanup();});
const as=uid=>uid?env.authenticatedContext(uid).firestore():env.unauthenticatedContext().firestore();
beforeEach(async()=>{
 await env.clearFirestore();
 await env.withSecurityRulesDisabled(async ctx=>{
  const db=ctx.firestore();
  for(const uid of ['a','b','owner']) await setDoc(doc(db,'usuarios',uid),{activo:true});
  for(const id of ['a','b']) {
   await setDoc(doc(db,'refugios',id),{activo:true});
   await setDoc(doc(db,'membresias',`${id}__${id}`),{usuarioUid:id,refugioId:id,activo:true});
  }
  await setDoc(doc(db,'perritos','dog'),{refugioId:'a',publicado:true,estado:'Disponible',nombre:'Luna',actualizadoEn:Timestamp.fromMillis(1)});
  await setDoc(doc(db,'perritos_control','dog'),{refugioId:'a',solicitudAprobadaId:null,adopcionId:null});
  for(const id of ['one','two']) await setDoc(doc(db,'solicitudes',id),{usuarioUid:'owner',refugioId:'a',perritoId:'dog',estado:'Enviada',motivoNoAprobacion:null,contacto:{nombre:'Persona'},creadoEn:Timestamp.fromMillis(1),actualizadoEn:Timestamp.fromMillis(1)});
 });
});
const suffix=status=>status==='En revisión'?'revision':status==='Aprobada'?'aprobada':'no-aprobada';
function decision(id='one',previous='Enviada',status='En revisión',options={}) {
 const uid=options.uid===undefined?'a':options.uid,db=as(uid),batch=writeBatch(db),reason=status==='No aprobada'?'No cumple las condiciones de adopción.':null;
 batch.update(doc(db,'solicitudes',id),{estado:status,motivoNoAprobacion:reason,actualizadoEn:serverTimestamp(),...options.patch});
 if(options.history!==false) batch.set(doc(db,'historial_solicitudes',`${id}-${suffix(status)}`),{solicitudId:id,usuarioUid:'owner',refugioId:'a',estadoAnterior:previous,estadoNuevo:status,motivo:reason,actorUid:uid,creadoEn:serverTimestamp(),...options.event});
 if(status==='Aprobada') {
  if(options.control!==false) batch.update(doc(db,'perritos_control','dog'),{solicitudAprobadaId:id});
  if(options.dog!==false) batch.update(doc(db,'perritos','dog'),{estado:'En proceso',actualizadoEn:serverTimestamp()});
 }
 return batch.commit();
}
const prepareReview=async(id='one')=>assertSucceeds(decision(id));
test('HU13 Enviada pasa a revisión con historial privado y fecha del servidor',async()=>{
 await prepareReview();
 const db=as('owner'),request=await getDoc(doc(db,'solicitudes','one')),history=await getDoc(doc(db,'historial_solicitudes','one-revision'));
 assert.equal(request.data().estado,'En revisión');assert.equal(history.data().estadoAnterior,'Enviada');assert.equal(history.data().actorUid,'a');assert.equal(request.data().actualizadoEn.toMillis(),history.data().creadoEn.toMillis());
});
test('HU13 aprobación modifica solicitud control y perrito en el mismo commit',async()=>{
 await prepareReview();await assertSucceeds(decision('one','En revisión','Aprobada'));
 assert.equal((await getDoc(doc(as('a'),'perritos_control','dog'))).data().solicitudAprobadaId,'one');
 assert.equal((await getDoc(doc(as(null),'perritos','dog'))).data().estado,'En proceso');
});
test('HU13 no aprobación conserva perrito y motivo visible al solicitante',async()=>{
 await prepareReview();await assertSucceeds(decision('one','En revisión','No aprobada'));
 assert.equal((await getDoc(doc(as('owner'),'solicitudes','one'))).data().motivoNoAprobacion,'No cumple las condiciones de adopción.');
 assert.equal((await getDoc(doc(as('a'),'perritos_control','dog'))).data().solicitudAprobadaId,null);
});
test('HU13 visitante adoptante y otro refugio no deciden',async()=>{
 for(const uid of [null,'owner','b']) await assertFails(decision('one','Enviada','En revisión',{uid}));
});
test('HU13 no se omite revisión ni se vuelve atrás después de decidir',async()=>{
 await assertFails(decision('one','Enviada','Aprobada'));await assertFails(decision('one','Enviada','No aprobada'));
 await prepareReview();await assertSucceeds(decision('one','En revisión','No aprobada'));await assertFails(decision('one','No aprobada','En revisión'));
});
test('HU13 respuestas propietario motivos inválidos y fechas falsas no se modifican',async()=>{
 await prepareReview();
 for(const patch of [{contacto:{nombre:'Alterada'}},{usuarioUid:'a'},{refugioId:'b'},{actualizadoEn:Timestamp.fromMillis(1)},{motivoNoAprobacion:' '},{motivoNoAprobacion:'x'.repeat(1001)}]) await assertFails(decision('one','En revisión','No aprobada',{patch}));
});
test('HU13 cualquier parte ausente rechaza todo el cambio de aprobación',async()=>{
 await prepareReview();
 for(const option of ['history','dog','control']) await assertFails(decision('one','En revisión','Aprobada',{[option]:false}));
 assert.equal((await getDoc(doc(as('a'),'solicitudes','one'))).data().estado,'En revisión');
});
test('HU13 dos aprobaciones simultáneas solo dejan una aprobada',async()=>{
 await prepareReview('one');await prepareReview('two');
 const results=await Promise.allSettled([decision('one','En revisión','Aprobada'),decision('two','En revisión','Aprobada')]);
 assert.equal(results.filter(r=>r.status==='fulfilled').length,1);
 const control=(await getDoc(doc(as('a'),'perritos_control','dog'))).data();
 for(const id of ['one','two']) assert.equal((await getDoc(doc(as('a'),'solicitudes',id))).data().estado,id===control.solicitudAprobadaId?'Aprobada':'En revisión');
});
test('HU13 historial falsificado duplicado o eliminado se rechaza',async()=>{
 await assertFails(decision('one','Enviada','En revisión',{event:{actorUid:'b'}}));
 await assertFails(decision('one','Enviada','En revisión',{event:{usuarioUid:'a'}}));
 await prepareReview();await assertFails(decision());
 await assertFails(deleteDoc(doc(as('a'),'historial_solicitudes','one-revision')));
});
test('HU13 controlar o mover perrito sin aprobar una solicitud se rechaza',async()=>{
 await assertFails(updateDoc(doc(as('a'),'perritos_control','dog'),{solicitudAprobadaId:'one'}));
 await assertFails(updateDoc(doc(as('a'),'perritos','dog'),{estado:'En proceso',actualizadoEn:serverTimestamp()}));
});
test('HU13 revocar membresía impide nuevas decisiones',async()=>{
 await env.withSecurityRulesDisabled(ctx=>updateDoc(doc(ctx.firestore(),'membresias','a__a'),{activo:false}));
 await assertFails(decision());
});
