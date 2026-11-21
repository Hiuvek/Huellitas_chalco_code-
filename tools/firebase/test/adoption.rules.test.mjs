import {before,after,beforeEach,test} from 'node:test';
import {readFile} from 'node:fs/promises';
import {createHash,randomUUID} from 'node:crypto';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,setDoc,getDoc,updateDoc,deleteDoc,writeBatch,serverTimestamp} from 'firebase/firestore';
let env;
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-huellitas-chalco',firestore:{host:'127.0.0.1',port:8180,rules:await readFile(new URL('../../../firestore.rules',import.meta.url),'utf8')}});});
after(async()=>{await env?.cleanup();});
const as=uid=>uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();
const key=(uid,dog='dog')=>createHash('sha256').update(`${uid}\0${dog}`).digest('hex');
const admin=(path,data)=>env.withSecurityRulesDisabled(ctx=>updateDoc(doc(ctx.firestore(),path),data));
beforeEach(async()=>{
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx=>{
    const db=ctx.firestore();
    for(const uid of ['adopter','other','a','b']) await setDoc(doc(db,'usuarios',uid),{activo:true});
    for(const id of ['a','b']) {
      await setDoc(doc(db,'refugios',id),{activo:true,municipio:'Chalco',entidad:'Estado de México',requisitos:['Mayor de edad'],pasos:['Solicitud']});
      await setDoc(doc(db,'membresias',`${id}__${id}`),{usuarioUid:id,refugioId:id,activo:true});
    }
    await setDoc(doc(db,'perritos','dog'),{refugioId:'a',publicado:true,estado:'Disponible'});
    await setDoc(doc(db,'perritos_control','dog'),{refugioId:'a',solicitudAprobadaId:null,adopcionId:null});
  });
});
const request=(uid,id,patch={})=>({folio:`HC-${id}`,usuarioUid:uid,perritoId:'dog',refugioId:'a',estado:'Enviada',
  contacto:{nombre:'Nombre Prueba',correo:'prueba@example.test',telefono:'5500000001'},
  hogar:{tipoVivienda:'CASA',tenencia:'PROPIA',espacioExterior:true,personas:3,horasSolo:'DE_4_A_8'},
  otrasMascotas:{tiene:false,detalle:null},motivos:'Tengo espacio seguro y quiero darle cuidados y compañía durante toda su vida.',
  motivoNoAprobacion:null,creadoEn:serverTimestamp(),actualizadoEn:serverTimestamp(),...patch});
function bundle(uid='adopter',patch={},options={}) {
  const id=options.id??randomUUID(),db=as(uid),batch=writeBatch(db),d=request(uid,id,patch);
  if(options.request!==false) batch.set(doc(db,'solicitudes',id),d);
  if(options.key!==false) batch.set(doc(db,'solicitudes_claves',options.keyId??key(uid)),{usuarioUid:uid,perritoId:'dog',solicitudId:id,...options.keyPatch});
  if(options.history!==false) batch.set(doc(db,'historial_solicitudes',options.historyId??`${id}-1`),{solicitudId:id,usuarioUid:uid,refugioId:'a',estadoAnterior:null,estadoNuevo:'Enviada',motivo:null,actorUid:uid,creadoEn:serverTimestamp(),...options.historyPatch});
  return {id,commit:()=>batch.commit()};
}
test('HU05 crea solicitud, clave e historial atómicos y permite lecturas propias',async()=>{
  const value=bundle();await assertSucceeds(value.commit());
  await assertSucceeds(getDoc(doc(as('adopter'),'solicitudes',value.id)));
  await assertSucceeds(getDoc(doc(as('adopter'),'historial_solicitudes',`${value.id}-1`)));
  await assertSucceeds(getDoc(doc(as('adopter'),'solicitudes_claves',key('adopter'))));
  await assertSucceeds(getDoc(doc(as('a'),'solicitudes',value.id)));
});
test('HU05 una cuenta nueva sin perfil administrativo puede solicitar sin obtener permisos de refugio',async()=>{
  await assertSucceeds(bundle('new-auth-user').commit());
  await assertFails(getDoc(doc(as('new-auth-user'),'perritos_control','dog')));
  await assertFails(setDoc(doc(as('new-auth-user'),'membresias','new-auth-user__a'),{usuarioUid:'new-auth-user',refugioId:'a',activo:true}));
});
test('HU05 visitante o suplantación de solicitante se rechazan',async()=>{
  await assertFails(bundle(null).commit());
  await assertFails(bundle('adopter',{usuarioUid:'other'}).commit());
  await assertFails(bundle('a',{usuarioUid:'adopter'}).commit());
});
test('HU05 datos incompletos, estados adelantados, folio o fechas falsas se rechazan',async()=>{
  for(const patch of [
    {contacto:{nombre:'P',correo:'mal',telefono:'1'}},{contacto:{nombre:'  ',correo:'ok@example.test',telefono:'5500000001'}},
    {contacto:{nombre:'Persona',correo:'a b@example.test',telefono:'5500000001'}},{contacto:{nombre:'Persona',correo:'ok@example.test',telefono:'55000000012'}},
    {hogar:{tipoVivienda:'CASA',tenencia:'PROPIA',espacioExterior:true,personas:0,horasSolo:'DE_4_A_8'}},
    {hogar:{tipoVivienda:'CASA',tenencia:'PROPIA',espacioExterior:true,personas:2.5,horasSolo:'DE_4_A_8'}},
    {hogar:{tipoVivienda:'OTRA',tenencia:'PROPIA',espacioExterior:true,personas:3,horasSolo:'DE_4_A_8'}},
    {otrasMascotas:{tiene:false,detalle:'No corresponde'}},{otrasMascotas:{tiene:'Sí',detalle:null}},
    {motivos:'Corto'},{motivos:' '.repeat(40)},{motivos:'x'.repeat(2001)},
    {estado:'Aprobada'},{folio:'HC-inventado'},{motivoNoAprobacion:'No corresponde'},{creadoEn:new Date(0)},{actualizadoEn:new Date(0)},{rol:'admin'},
  ]) await assertFails(bundle('adopter',patch).commit());
});
test('HU05 mascotas opcionales se guardan con y sin descripción',async()=>{
  await assertSucceeds(bundle('adopter',{otrasMascotas:{tiene:true,detalle:null}}).commit());
  await assertSucceeds(bundle('other',{otrasMascotas:{tiene:true,detalle:'Un gato adulto.'}}).commit());
});
test('HU05 sin cualquiera de los tres documentos se rechaza la operación completa',async()=>{
  for(const options of [{key:false},{history:false},{request:false},{key:false,history:false}]) await assertFails(bundle('adopter',{},options).commit());
});
test('HU05 no se permite una segunda solicitud de la misma cuenta y perrito',async()=>{
  const first=bundle();await assertSucceeds(first.commit());
  await assertFails(bundle().commit());
  await assertSucceeds(bundle('other').commit());
});
test('HU05 perrito sin publicar, en proceso o adoptado y control comprometido bloquean solicitudes',async()=>{
  await admin('perritos/dog',{publicado:false});await assertFails(bundle().commit());
  await admin('perritos/dog',{publicado:true,estado:'En proceso'});await assertFails(bundle().commit());
  await admin('perritos/dog',{estado:'Adoptado'});await assertFails(bundle().commit());
  await admin('perritos/dog',{estado:'Disponible'});await admin('perritos_control/dog',{solicitudAprobadaId:'otra'});await assertFails(bundle().commit());
  await admin('perritos_control/dog',{solicitudAprobadaId:null,adopcionId:'otra'});await assertFails(bundle().commit());
});
test('HU05 refugio inactivo, externo, sin requisitos o perfil desactivado bloquean el envío',async()=>{
  for(const patch of [{activo:false},{municipio:'Ixtapaluca'},{requisitos:[]},{pasos:[]}]) {
    await admin('refugios/a',patch);await assertFails(bundle().commit());
    await admin('refugios/a',{activo:true,municipio:'Chalco',requisitos:['Mayor de edad'],pasos:['Solicitud']});
  }
  await admin('usuarios/adopter',{activo:false});await assertFails(bundle().commit());
});
test('HU05 no se mezcla el perrito de un refugio con otro refugio',async()=>{
  await assertFails(bundle('adopter',{refugioId:'b'}).commit());
});
test('HU05 otra cuenta u otro refugio no leen respuestas, historial ni clave',async()=>{
  const value=bundle();await value.commit();
  for(const uid of [null,'other','b']) {
    await assertFails(getDoc(doc(as(uid),'solicitudes',value.id)));
    await assertFails(getDoc(doc(as(uid),'historial_solicitudes',`${value.id}-1`)));
    await assertFails(getDoc(doc(as(uid),'solicitudes_claves',key('adopter'))));
  }
  await assertSucceeds(getDoc(doc(as('a'),'historial_solicitudes',`${value.id}-1`)));
});
test('HU05 solicitante no modifica o borra documentos confirmados',async()=>{
  const value=bundle();await value.commit();
  for(const [collection,id] of [['solicitudes',value.id],['historial_solicitudes',`${value.id}-1`],['solicitudes_claves',key('adopter')]]) {
    await assertFails(deleteDoc(doc(as('adopter'),collection,id)));
    await assertFails(updateDoc(doc(as('adopter'),collection,id),{estado:'Aprobada'}));
  }
});
test('HU05 actor, historial y clave falsificados se rechazan',async()=>{
  for(const options of [{historyPatch:{actorUid:'a'}},{historyPatch:{estadoNuevo:'Aprobada'}},{historyPatch:{usuarioUid:'other'}},{historyId:'falso'},{keyId:'falsa'},{keyPatch:{usuarioUid:'other'}},{keyPatch:{perritoId:'otro'}}]) await assertFails(bundle('adopter',{},options).commit());
});
test('HU05 desactivar el perfil bloquea la consulta privada pero no convierte datos públicos en privados',async()=>{
  const value=bundle();await value.commit();
  await admin('usuarios/adopter',{activo:false});
  await assertFails(getDoc(doc(as('adopter'),'solicitudes',value.id)));
  await assertSucceeds(getDoc(doc(as('adopter'),'perritos','dog')));
});
