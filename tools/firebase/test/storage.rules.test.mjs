import {before,after,beforeEach,test} from 'node:test';
import {readFile} from 'node:fs/promises';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,setDoc,updateDoc} from 'firebase/firestore';
import {ref,uploadBytes,getBytes,deleteObject,listAll} from 'firebase/storage';
let env;
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-huellitas-chalco',
  firestore:{host:'127.0.0.1',port:8180,rules:await readFile(new URL('../../../firestore.rules',import.meta.url),'utf8')},
  storage:{host:'127.0.0.1',port:9199,rules:await readFile(new URL('../../../storage.rules',import.meta.url),'utf8')},
});});
after(async()=>{await env?.cleanup();});
beforeEach(async()=>{
  await env.clearStorage(); await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context=>{
    const db=context.firestore();
    for(const uid of ['a','b','adopter']) await setDoc(doc(db,'usuarios',uid),{activo:true});
    for(const id of ['a','b']) {
      await setDoc(doc(db,'refugios',id),{activo:true});
      await setDoc(doc(db,'membresias',`${id}__${id}`),{usuarioUid:id,refugioId:id,activo:true});
    }
  });
});
const storage=uid=>uid ? env.authenticatedContext(uid).storage() : env.unauthenticatedContext().storage();
const path='perritos/a/a/nuevo/foto.jpg';
const image=new Uint8Array([255,216,255,224,0,16,74,70,73,70]);
const put=(uid='a',key=path,bytes=image,type='image/jpeg')=>uploadBytes(ref(storage(uid),key),bytes,{contentType:type});
const adminSet=(collection,id,data)=>env.withSecurityRulesDisabled(ctx=>setDoc(doc(ctx.firestore(),collection,id),data));
test('HU08 Storage permite preparar y consultar fotos propias, aisladas de otro refugio',async()=>{
  await assertSucceeds(put());
  await assertSucceeds(getBytes(ref(storage('a'),path)));
  await assertFails(getBytes(ref(storage('b'),path)));
  await assertFails(getBytes(ref(storage(),path)));
});
test('HU08 Storage rechaza visitante, adoptante y rutas de otra cuenta o refugio',async()=>{
  await assertFails(put(null)); await assertFails(put('adopter'));
  await assertFails(put('b')); await assertFails(put('a','perritos/b/a/nuevo/foto.jpg'));
  await assertFails(put('a','perritos/a/b/nuevo/foto.jpg'));
});
test('HU08 Storage limita JPEG de 2 MB y bloquea tamaños, tipos y nombres inválidos',async()=>{
  await assertFails(put('a',path,new Uint8Array(0)));
  await assertFails(put('a',path,new Uint8Array(2*1024*1024+1)));
  await assertFails(put('a',path,image,'text/plain'));
  await assertFails(put('a','perritos/a/a/nuevo/foto.exe'));
  await assertFails(put('a','otras/foto.jpg'));
});
test('HU08 Storage permite reintentar antes de publicar y borrar una foto pendiente propia',async()=>{
  await assertSucceeds(put()); await assertSucceeds(put());
  await assertFails(deleteObject(ref(storage('b'),path)));
  await assertSucceeds(deleteObject(ref(storage('a'),path)));
});
test('HU08 Storage publicación habilita lectura pública, pero impide sobrescribir y borrar fotos',async()=>{
  await assertSucceeds(put());
  await adminSet('perritos','nuevo',{refugioId:'a',publicado:true,estado:'Disponible',fotos:[{archivoId:'foto',ruta:path,orden:0}]});
  await assertSucceeds(getBytes(ref(storage(),path)));
  await assertFails(put()); await assertFails(deleteObject(ref(storage('a'),path)));
  await assertFails(listAll(ref(storage(),'perritos/a')));
});
test('HU08 Storage revocación retira el acceso a fotos no publicadas',async()=>{
  await assertSucceeds(put());
  await env.withSecurityRulesDisabled(ctx=>updateDoc(doc(ctx.firestore(),'membresias','a__a'),{activo:false}));
  await assertFails(put()); await assertFails(getBytes(ref(storage('a'),path)));
});
test('HU08 Storage adopción o refugio ajeno no habilita la lectura pública',async()=>{
  await assertSucceeds(put());
  await adminSet('perritos','nuevo',{refugioId:'a',publicado:true,estado:'Adoptado'});
  await assertFails(getBytes(ref(storage(),path)));
  await adminSet('perritos','nuevo',{refugioId:'b',publicado:true,estado:'Disponible'});
  await assertFails(getBytes(ref(storage(),path)));
});

// HU-10: fotos nuevas en fichas existentes sin sobrescribir referencias publicadas.
test('HU10 Storage permite preparar fotos nuevas sin publicarlas antes del guardado',async()=>{
  await put();
  await adminSet('perritos','nuevo',{refugioId:'a',publicado:true,estado:'Disponible',fotos:[{archivoId:'foto',ruta:path,orden:0}]});
  const fresh='perritos/a/a/nuevo/nueva.jpg';
  await assertSucceeds(put('a',fresh));
  await assertSucceeds(put('a',fresh));
  await assertFails(getBytes(ref(storage(),fresh)));
  await assertFails(put());
  await assertFails(deleteObject(ref(storage('a'),path)));
  await adminSet('perritos','nuevo',{refugioId:'a',publicado:true,estado:'Disponible',fotos:[{archivoId:'nueva',ruta:fresh,orden:0}]});
  await assertSucceeds(getBytes(ref(storage(),fresh)));
  await assertFails(getBytes(ref(storage(),path)));
});
test('HU10 Storage no permite subir usando ID de un perrito de otro refugio',async()=>{
  await adminSet('perritos','nuevo',{refugioId:'b',publicado:true,estado:'Disponible',fotos:[]});
  await assertFails(put());
});
test('HU10 Storage otra cuenta del mismo refugio no sobrescribe una imagen usada',async()=>{
  await put();
  await adminSet('membresias','b__a',{usuarioUid:'b',refugioId:'a',activo:true});
  await adminSet('perritos','nuevo',{refugioId:'a',publicado:true,estado:'Disponible',fotos:[{archivoId:'foto',ruta:path,orden:0}]});
  await assertFails(put('b',path));
  await assertSucceeds(put('b','perritos/a/b/nuevo/nueva.jpg'));
});
