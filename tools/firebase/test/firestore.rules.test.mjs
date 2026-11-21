import { before, after, beforeEach, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, getDocs, collection, query, where, updateDoc, deleteDoc } from 'firebase/firestore';
let env;
const privateCollections = ['solicitudes','historial_solicitudes','adopciones','revisiones','seguimientos','perritos_control'];
before(async () => {
  env = await initializeTestEnvironment({projectId:'demo-huellitas-chalco',firestore:{host:'127.0.0.1',port:8180,rules:await readFile(new URL('../../../firestore.rules',import.meta.url),'utf8')}});
});
after(async () => { await env?.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context => {
    const db=context.firestore(); const writes=[];
    for (const uid of ['a','b','adopter','other']) writes.push(setDoc(doc(db,'usuarios',uid),{uid,activo:true}));
    for (const id of ['a','b']) {
      writes.push(setDoc(doc(db,'refugios',id),{nombre:`Refugio ${id}`,activo:true}));
      writes.push(setDoc(doc(db,'membresias',`${id}__${id}`),{usuarioUid:id,refugioId:id,activo:true}));
      writes.push(setDoc(doc(db,'perritos',`private-${id}`),{refugioId:id,publicado:false,estado:'Disponible'}));
      writes.push(setDoc(doc(db,'perritos',`public-${id}`),{refugioId:id,publicado:true,estado:'Disponible'}));
      for (const c of privateCollections) writes.push(setDoc(doc(db,c,id),{refugioId:id,usuarioUid:id==='a'?'adopter':'other'}));
    }
    await Promise.all(writes);
  });
});
const as = uid => uid ? env.authenticatedContext(uid).firestore() : env.unauthenticatedContext().firestore();
const adminUpdate = (path, data) => env.withSecurityRulesDisabled(ctx=>updateDoc(doc(ctx.firestore(),path),data));

test('visitante consulta datos públicos, pero no perritos sin publicar', async()=>{
  const db=as(); await assertSucceeds(getDoc(doc(db,'refugios','a')));
  await assertSucceeds(getDoc(doc(db,'perritos','public-a')));
  await assertFails(getDoc(doc(db,'perritos','private-a')));
});
test('visitante no lee ninguna colección privada',async()=>{
  for(const c of privateCollections) await assertFails(getDoc(doc(as(),c,'a')));
});
test('adoptante consulta su solicitud y su historial, pero no los de otra cuenta',async()=>{
  for(const c of ['solicitudes','historial_solicitudes','adopciones']) {
    await assertSucceeds(getDoc(doc(as('adopter'),c,'a')));
    await assertFails(getDoc(doc(as('adopter'),c,'b')));
  }
});
test('adoptante no recibe permisos administrativos por estar autenticado',async()=>{
  for(const c of ['revisiones','seguimientos','perritos_control']) await assertFails(getDoc(doc(as('adopter'),c,'a')));
  await assertFails(getDoc(doc(as('adopter'),'perritos','private-a')));
});
for(const c of privateCollections) test(`refugio A solo consulta sus ${c}`,async()=>{
  await assertSucceeds(getDoc(doc(as('a'),c,'a')));
  await assertFails(getDoc(doc(as('a'),c,'b')));
  await assertSucceeds(getDoc(doc(as('b'),c,'b')));
  await assertFails(getDoc(doc(as('b'),c,'a')));
});
test('consultas deben indicar el refugio autorizado',async()=>{
  const db=as('a');
  await assertSucceeds(getDocs(query(collection(db,'seguimientos'),where('refugioId','==','a'))));
  await assertFails(getDocs(query(collection(db,'seguimientos'),where('refugioId','==','b'))));
  await assertFails(getDocs(collection(db,'seguimientos')));
});
test('perritos privados de A están aislados de B',async()=>{
  await assertSucceeds(getDoc(doc(as('a'),'perritos','private-a')));
  await assertFails(getDoc(doc(as('a'),'perritos','private-b')));
  await assertSucceeds(getDocs(query(collection(as('a'),'perritos'),where('refugioId','==','a'))));
});
test('solo se leen las membresías de la cuenta actual',async()=>{
  await assertSucceeds(getDoc(doc(as('a'),'membresias','a__a')));
  await assertFails(getDoc(doc(as('a'),'membresias','b__b')));
  await assertSucceeds(getDocs(query(collection(as('a'),'membresias'),where('usuarioUid','==','a'),where('activo','==',true))));
  await assertFails(getDocs(collection(as('a'),'membresias')));
});
test('nadie se autoriza ni modifica membresías desde la app',async()=>{
  await assertFails(setDoc(doc(as('adopter'),'membresias','adopter__a'),{usuarioUid:'adopter',refugioId:'a',activo:true}));
  await assertFails(updateDoc(doc(as('a'),'membresias','a__a'),{refugioId:'b'}));
  await assertFails(deleteDoc(doc(as('a'),'membresias','a__a')));
});
test('perfil propio es privado y no permite autoactivación',async()=>{
  await assertSucceeds(getDoc(doc(as('a'),'usuarios','a')));
  await assertFails(getDoc(doc(as('a'),'usuarios','b')));
  await assertFails(getDocs(collection(as('a'),'usuarios')));
  await assertFails(setDoc(doc(as('adopter'),'usuarios','adopter'),{uid:'adopter',activo:true,rol:'refugio'}));
});
test('revocar membresía retira permisos sin cambiar la contraseña',async()=>{
  await assertSucceeds(getDoc(doc(as('a'),'seguimientos','a')));
  await adminUpdate('membresias/a__a',{activo:false});
  await assertFails(getDoc(doc(as('a'),'seguimientos','a')));
});
test('desactivar el perfil retira todos sus permisos privados',async()=>{
  await adminUpdate('usuarios/a',{activo:false});
  await assertFails(getDoc(doc(as('a'),'seguimientos','a')));
});
test('desactivar refugio retira el acceso administrativo',async()=>{
  await adminUpdate('refugios/a',{activo:false});
  await assertFails(getDoc(doc(as('a'),'seguimientos','a')));
});
test('membresía con UID o refugio adulterado no concede permisos',async()=>{
  await adminUpdate('membresias/a__a',{usuarioUid:'b'});
  await assertFails(getDoc(doc(as('a'),'seguimientos','a')));
  await adminUpdate('membresias/a__a',{usuarioUid:'a',refugioId:'b'});
  await assertFails(getDoc(doc(as('a'),'seguimientos','a')));
});
test('la misma cuenta puede estar autorizada en dos refugios',async()=>{
  await env.withSecurityRulesDisabled(ctx=>setDoc(doc(ctx.firestore(),'membresias','a__b'),{usuarioUid:'a',refugioId:'b',activo:true}));
  await assertSucceeds(getDoc(doc(as('a'),'seguimientos','a')));
  await assertSucceeds(getDoc(doc(as('a'),'seguimientos','b')));
});
test('sin perfil activo no basta con una membresía',async()=>{
  await env.withSecurityRulesDisabled(ctx=>deleteDoc(doc(ctx.firestore(),'usuarios','a')));
  await assertFails(getDoc(doc(as('a'),'seguimientos','a')));
});
test('no se pueden cambiar propietarios ni adelantar escrituras de otras historias',async()=>{
  const db=as('a');
  await assertFails(updateDoc(doc(db,'perritos','private-a'),{refugioId:'b'}));
  await assertFails(updateDoc(doc(db,'perritos','private-b'),{refugioId:'a'}));
  for(const c of privateCollections) await assertFails(setDoc(doc(db,c,'nuevo'),{refugioId:'a',usuarioUid:'a'}));
  await assertFails(updateDoc(doc(db,'refugios','a'),{activo:true}));
});
test('colecciones desconocidas permanecen bloqueadas',async()=>{
  await assertFails(setDoc(doc(as('a'),'admins','a'),{rol:'admin'}));
  await assertFails(getDoc(doc(as('a'),'admins','a')));
});
