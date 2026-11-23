import {before,after,beforeEach,test} from 'node:test';
import {readFile} from 'node:fs/promises';
import assert from 'node:assert/strict';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,setDoc,getDoc,updateDoc,deleteDoc,serverTimestamp,Timestamp} from 'firebase/firestore';
let env;
const as=uid=>uid?env.authenticatedContext(uid).firestore():env.unauthenticatedContext().firestore();
const ref=(db,p)=>doc(db,p);
const admin=f=>env.withSecurityRulesDisabled(c=>f(c.firestore()));
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-huellitas-chalco',firestore:{host:'127.0.0.1',port:8180,rules:await readFile(new URL('../../../firestore.rules',import.meta.url),'utf8')}})});
after(async()=>env?.cleanup());
beforeEach(async()=>{await env.clearFirestore();await admin(async db=>{
 for(const uid of ['a','b','adopter'])await setDoc(ref(db,`usuarios/${uid}`),{activo:true});
 for(const id of ['a','b']){
  await setDoc(ref(db,`refugios/${id}`),{nombre:`Refugio ${id}`,activo:true,municipio:'Chalco',entidad:'Estado de México',esDemostracion:true,zonaPublica:'Centro',horarioPublico:'10 a 17 h',requisitos:['Ser mayor de edad.'],pasos:['Enviar solicitud.']});
  await setDoc(ref(db,`membresias/${id}__${id}`),{usuarioUid:id,refugioId:id,activo:true});
 }
});});
const fields=(uid='a',patch={})=>({nombre:'Nombre actualizado',zonaPublica:'Zona pública nueva',horarioPublico:'Lunes a viernes',requisitos:['Identificación.','Entrevista.'],pasos:['Enviar solicitud.','Visita.'],ubicacionAproximada:{latitud:19.26,longitud:-98.9},actualizadoEn:serverTimestamp(),actualizadoPorUid:uid,...patch});
const save=(uid='a',patch={})=>updateDoc(ref(as(uid),'refugios/a'),fields(uid,patch));
test('Responsable edita datos públicos de su refugio y visitantes reciben cambios',async()=>{
 await assertSucceeds(save());const result=await getDoc(ref(as(null),'refugios/a'));
 assert.equal(result.get('nombre'),'Nombre actualizado');assert.equal(result.get('activo'),true);assert.equal(result.get('municipio'),'Chalco');assert.equal(result.get('esDemostracion'),true);assert.equal(result.get('actualizadoPorUid'),'a');
});
test('Visitantes, adoptantes y otro refugio no editan información',async()=>{
 for(const uid of [null,'adopter','b'])await assertFails(save(uid));
});
test('Edición no cambia municipio, activación, demostración ni permisos',async()=>{
 for(const patch of [{municipio:'Otro'},{entidad:'Otra'},{activo:false},{esDemostracion:false},{responsableUid:'adopter'},{extra:'dato'}])await assertFails(save('a',patch));
});
test('Nombre, zona y horario exigen texto válido y límites',async()=>{
 for(const patch of [{nombre:''},{nombre:'  '},{nombre:' espacio '},{nombre:'x'.repeat(101)},{zonaPublica:''},{zonaPublica:'x'.repeat(251)},{horarioPublico:''},{horarioPublico:'x'.repeat(251)}])await assertFails(save('a',patch));
});
test('Todas las entradas de requisitos y pasos se validan incluyendo la décima',async()=>{
 for(const key of ['requisitos','pasos']){
  for(const value of [[],[''],['  '],['x'.repeat(301)],[true],'texto',Array(11).fill('Válido'),[...Array(9).fill('Válido'),'']])await assertFails(save('a',{[key]:value}));
 }
 await assertSucceeds(save('a',{requisitos:Array(10).fill('Válido'),pasos:Array(10).fill('Válido')}));
});
test('Coordenadas completas, numéricas y acotadas o ubicación vacía',async()=>{
 for(const value of [{latitud:91,longitud:0},{latitud:0,longitud:-181},{latitud:19},{latitud:'19',longitud:0},{latitud:0,longitud:0,privado:true},{latitud:NaN,longitud:0}])await assertFails(save('a',{ubicacionAproximada:value}));
 await assertSucceeds(save('a',{ubicacionAproximada:null}));
});
test('Perfil, membresía o refugio inactivos bloquean el guardado',async()=>{
 for(const path of ['usuarios/a','membresias/a__a','refugios/a']){
  await admin(db=>updateDoc(ref(db,path),{activo:false}));await assertFails(save());await admin(db=>updateDoc(ref(db,path),{activo:true}));
 }
});
test('Auditoría requiere fecha del servidor y autor real',async()=>{
 await assertFails(save('a',{actualizadoPorUid:'b'}));await assertFails(save('a',{actualizadoEn:Timestamp.fromMillis(1)}));
});
test('La app no crea ni elimina refugios ni asigna responsables',async()=>{
 await assertFails(setDoc(ref(as('a'),'refugios/nuevo'),fields('a',{activo:true,municipio:'Chalco',entidad:'Estado de México'})));
 await assertFails(deleteDoc(ref(as('a'),'refugios/a')));
 await assertFails(setDoc(ref(as('a'),'membresias/adopter__a'),{usuarioUid:'adopter',refugioId:'a',activo:true}));
});
