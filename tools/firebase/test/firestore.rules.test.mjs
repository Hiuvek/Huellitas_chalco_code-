import { before, after, beforeEach, test } from 'node:test';
import { readFile } from 'node:fs/promises';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, getDocs, collection, query, where, updateDoc, deleteDoc, writeBatch, serverTimestamp } from 'firebase/firestore';
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

// HU-08: alta atómica, datos completos y referencias de fotografías propias.
const newDog = (uid='a', shelter='a', id='nuevo', count=1) => ({
  refugioId:shelter,nombre:'Luna',edadMeses:24,tamano:'Mediano',sexo:'Hembra',
  descripcion:'Perrita tranquila y sociable que disfruta salir a caminar acompañada.',
  vacunacion:'Sin información',desparasitacion:'Sin información',esterilizacion:'Sin información',
  estado:'Disponible',publicado:true,esDemostracion:true,
  fotos:Array.from({length:count},(_,i)=>({archivoId:`foto-${i}`,ruta:`perritos/${shelter}/${uid}/${id}/foto-${i}.jpg`,orden:i})),
  creadoEn:serverTimestamp(),actualizadoEn:serverTimestamp(),
});
const publish = (uid='a', shelter='a', id='nuevo', change={}) => {
  const db=as(uid); const batch=writeBatch(db);
  batch.set(doc(db,'perritos',id),{...newDog(uid,shelter,id),...change});
  batch.set(doc(db,'perritos_control',id),{refugioId:shelter,solicitudAprobadaId:null,adopcionId:null});
  return batch.commit();
};
test('HU08 responsable publica ficha completa y control en una operación',async()=>{
  await assertSucceeds(publish());
  const snapshot=await assertSucceeds(getDoc(doc(as(),'perritos','nuevo')));
  if(snapshot.data().nombre!=='Luna') throw Error('Registro inesperado');
  await assertSucceeds(getDoc(doc(as('a'),'perritos_control','nuevo')));
  await assertFails(getDoc(doc(as('b'),'perritos_control','nuevo')));
});
test('HU08 visitante y adoptante no publican, ni responsable en otro refugio',async()=>{
  await assertFails(publish(null,'a','sin-cuenta'));
  await assertFails(publish('adopter','a','adoptante'));
  await assertFails(publish('a','b','otro-refugio'));
});
test('HU08 sin membresía o perfil activo no se permite publicar',async()=>{
  await adminUpdate('membresias/a__a',{activo:false});
  await assertFails(publish());
  await adminUpdate('membresias/a__a',{activo:true});
  await adminUpdate('usuarios/a',{activo:false});
  await assertFails(publish());
});
test('HU08 no se permite publicar sin control, ni control sin ficha',async()=>{
  await assertFails(setDoc(doc(as('a'),'perritos','nuevo'),newDog()));
  await assertFails(setDoc(doc(as('a'),'perritos_control','nuevo'),{refugioId:'a',solicitudAprobadaId:null,adopcionId:null}));
});
test('HU08 datos incompletos, estados adelantados y campos ajenos se rechazan',async()=>{
  for(const patch of [
    {nombre:''},{nombre:'   '},{nombre:'x'.repeat(61)},{edadMeses:-1},{edadMeses:361},{edadMeses:1.5},
    {tamano:'Gigante'},{sexo:'Otro'},{descripcion:'Corta'},{descripcion:' '.repeat(40)},{descripcion:'x'.repeat(2001)},
    {publicado:false},{estado:'Adoptado'},{vacunacion:'Tal vez'},{rol:'admin'},
    {creadoEn:new Date(0)},{fotos:[]},
  ]) await assertFails(publish('a','a','nuevo',patch));
  const {sexo,...missing}=newDog();
  await assertFails(setDoc(doc(as('a'),'perritos','nuevo'),missing));
});
test('Spark admite una foto y rechaza múltiples fotos, orden o rutas ajenas',async()=>{
  await assertFails(publish('a','a','seis',{fotos:newDog('a','a','seis',6).fotos}));
  for(const photos of [
    newDog('a','a','nuevo',7).fotos,
    newDog('b','a','nuevo').fotos,
    newDog('a','b','nuevo').fotos,
    [{...newDog().fotos[0],orden:1}],
    [{...newDog().fotos[0],ruta:'https://ejemplo.test/foto.jpg'}],
    [newDog().fotos[0],{...newDog().fotos[0],orden:1}],
  ]) await assertFails(publish('a','a','nuevo',{fotos:photos}));
});
test('HU08 una publicación existente no se sobrescribe ni se elimina',async()=>{
  await assertSucceeds(publish());
  await assertFails(publish('a','a','nuevo',{nombre:'Otro'}));
  await assertFails(updateDoc(doc(as('a'),'perritos','nuevo'),{estado:'Adoptado'}));
  await assertFails(deleteDoc(doc(as('a'),'perritos','nuevo')));
  await assertFails(deleteDoc(doc(as('a'),'perritos_control','nuevo')));
});
test('HU08 la lectura de un ID nuevo requiere perfil activo y no revela registros privados',async()=>{
  await assertSucceeds(getDoc(doc(as('a'),'perritos','inexistente')));
  await assertFails(getDoc(doc(as(),'perritos','inexistente')));
  await assertFails(getDoc(doc(as('a'),'perritos','private-b')));
});

// HU-09: salud con tres estados y actualización limitada al refugio propietario.
const healthPatch = (extra={}) => ({vacunacion:'Sí',desparasitacion:'No',esterilizacion:'Sin información',actualizadoEn:serverTimestamp(),...extra});
test('HU09 admite las 27 combinaciones de salud al registrar',async()=>{
  let id=0;
  for(const vacunacion of ['Sí','No','Sin información'])
    for(const desparasitacion of ['Sí','No','Sin información'])
      for(const esterilizacion of ['Sí','No','Sin información'])
        await assertSucceeds(publish('a','a',`salud-${id++}`,{vacunacion,desparasitacion,esterilizacion}));
});
test('HU09 responsable guarda salud y visitante consulta el resultado',async()=>{
  await publish();
  await assertSucceeds(updateDoc(doc(as('a'),'perritos','nuevo'),healthPatch()));
  const result=await assertSucceeds(getDoc(doc(as(),'perritos','nuevo')));
  if(result.data().vacunacion!=='Sí'||result.data().desparasitacion!=='No'||result.data().esterilizacion!=='Sin información') throw Error('Salud inesperada');
});
test('HU09 visitante, adoptante y otro refugio no cambian salud',async()=>{
  await publish();
  for(const uid of [null,'adopter','b']) await assertFails(updateDoc(doc(as(uid),'perritos','nuevo'),healthPatch()));
});
test('HU09 rechaza estados inválidos y campos de salud ausentes',async()=>{
  await publish();
  for(const value of ['Tal vez',true,null,'']) {
    await assertFails(updateDoc(doc(as('a'),'perritos','nuevo'),healthPatch({vacunacion:value})));
    await assertFails(publish('a','a','invalido',{desparasitacion:value}));
  }
  const {vacunacion,...missing}=newDog('a','a','incompleto');
  const db=as('a'); const batch=writeBatch(db);
  batch.set(doc(db,'perritos','incompleto'),missing);
  batch.set(doc(db,'perritos_control','incompleto'),{refugioId:'a',solicitudAprobadaId:null,adopcionId:null});
  await assertFails(batch.commit());
});
test('HU09 salud no permite editar otros datos ni falsificar fecha',async()=>{
  await publish();
  for(const patch of [{nombre:'Otro'}, {refugioId:'b'}, {estado:'Adoptado'}, {publicado:false}, {fotos:[]}, {creadoEn:serverTimestamp()}, {actualizadoEn:new Date(0)}])
    await assertFails(updateDoc(doc(as('a'),'perritos','nuevo'),healthPatch(patch)));
});
test('HU09 revocar membresía, perfil o refugio impide guardar salud',async()=>{
  await publish();
  for(const path of ['membresias/a__a','usuarios/a','refugios/a']) {
    await adminUpdate(path,{activo:false});
    await assertFails(updateDoc(doc(as('a'),'perritos','nuevo'),healthPatch()));
    await adminUpdate(path,{activo:true});
  }
});

// HU-10: datos generales y fotos, sin modificar salud ni ciclo de adopción.
const editPatch=(extra={})=>({nombre:'Luna editada',edadMeses:30,tamano:'Grande',sexo:'Hembra',descripcion:'Perrita ficticia con descripción actualizada, tranquila y acostumbrada a pasear.',actualizadoEn:serverTimestamp(),...extra});
test('HU10 edita datos generales y conserva identidad, salud y control',async()=>{
  await publish();
  await assertSucceeds(updateDoc(doc(as('a'),'perritos','nuevo'),editPatch()));
  const snapshot=await assertSucceeds(getDoc(doc(as(),'perritos','nuevo')));
  if(snapshot.data().nombre!=='Luna editada'||snapshot.data().vacunacion!=='Sin información') throw Error('Datos inesperados');
  await assertSucceeds(getDoc(doc(as('a'),'perritos_control','nuevo')));
});
test('HU10 visitante, adoptante y otro refugio no editan',async()=>{
  await publish();
  for(const uid of [null,'adopter','b']) await assertFails(updateDoc(doc(as(uid),'perritos','nuevo'),editPatch()));
});
test('HU10 rechaza campos vacíos, edad inválida y pérdida de fotografías',async()=>{
  await publish();
  for(const extra of [{nombre:''},{nombre:'   '},{nombre:'x'.repeat(61)},{edadMeses:-1},{edadMeses:361},{edadMeses:2.5},{tamano:'Gigante'},{sexo:'Otro'},{descripcion:'Corta'},{descripcion:' '.repeat(40)},{fotos:[]},{fotos:newDog('a','a','nuevo',7).fotos}])
    await assertFails(updateDoc(doc(as('a'),'perritos','nuevo'),editPatch(extra)));
});
test('HU10 no cambia propietario, creación, publicación, estado ni salud en la misma edición',async()=>{
  await publish();
  for(const extra of [{refugioId:'b'},{creadoEn:serverTimestamp()},{publicado:false},{estado:'Adoptado'},{vacunacion:'Sí'},{esDemostracion:false},{rol:'admin'},{actualizadoEn:new Date(0)}])
    await assertFails(updateDoc(doc(as('a'),'perritos','nuevo'),editPatch(extra)));
});
test('HU10 admite nuevas fotos propias, portada y eliminación de referencias antiguas',async()=>{
  await publish();
  const fresh={archivoId:'nueva',ruta:'perritos/a/a/nuevo/nueva.jpg',orden:0};
  await assertFails(updateDoc(doc(as('a'),'perritos','nuevo'),editPatch({fotos:[fresh,{...newDog().fotos[0],orden:1}]})));
  await assertSucceeds(updateDoc(doc(as('a'),'perritos','nuevo'),editPatch({fotos:[fresh]})));
});
test('HU10 otro miembro del mismo refugio conserva las fotos originales y agrega propias',async()=>{
  await publish();
  await env.withSecurityRulesDisabled(ctx=>setDoc(doc(ctx.firestore(),'membresias','b__a'),{usuarioUid:'b',refugioId:'a',activo:true}));
  await assertSucceeds(updateDoc(doc(as('b'),'perritos','nuevo'),editPatch({fotos:[newDog().fotos[0]]})));
});
test('HU10 no permite inyectar nuevas rutas ajenas ni identidades alteradas o repetidas',async()=>{
  await publish();
  for(const fotos of [
    [{archivoId:'nueva',ruta:'perritos/a/b/nuevo/nueva.jpg',orden:0}],
    [{...newDog().fotos[0],archivoId:'mentira'}],
    [{...newDog().fotos[0],orden:1}],
    [newDog().fotos[0],{...newDog().fotos[0],orden:1}],
    [{archivoId:'nueva',ruta:'perritos/b/a/nuevo/nueva.jpg',orden:0}],
  ]) await assertFails(updateDoc(doc(as('a'),'perritos','nuevo'),editPatch({fotos})));
});
test('HU10 revocación de membresía o perfil bloquea la edición',async()=>{
  await publish();
  for(const path of ['membresias/a__a','usuarios/a','refugios/a']) {
    await adminUpdate(path,{activo:false});
    await assertFails(updateDoc(doc(as('a'),'perritos','nuevo'),editPatch()));
    await adminUpdate(path,{activo:true});
  }
});

// HU-02: consultas públicas no amplían permisos de escritura o de lectura privada.
test('HU02 visitante consulta catálogo publicado disponible o en proceso',async()=>{
  const db=as();
  await publish();
  await env.withSecurityRulesDisabled(ctx=>setDoc(doc(ctx.firestore(),'perritos','proceso'),{...newDog(),estado:'En proceso',creadoEn:new Date(),actualizadoEn:new Date()}));
  await env.withSecurityRulesDisabled(ctx=>setDoc(doc(ctx.firestore(),'perritos','adoptado'),{...newDog(),estado:'Adoptado',creadoEn:new Date(),actualizadoEn:new Date()}));
  const result=await assertSucceeds(getDocs(query(collection(db,'perritos'),where('publicado','==',true),where('estado','in',['Disponible','En proceso']))));
  const ids=result.docs.map(d=>d.id);
  if(!ids.includes('nuevo')||!ids.includes('proceso')||ids.includes('adoptado')||ids.includes('private-a')) throw Error('Catálogo inesperado');
});
test('HU02 consulta sin restricciones públicas o incluyendo adoptados se rechaza',async()=>{
  await assertFails(getDocs(collection(as(),'perritos')));
  await assertFails(getDocs(query(collection(as(),'perritos'),where('publicado','==',true))));
  await assertFails(getDocs(query(collection(as(),'perritos'),where('publicado','==',true),where('estado','in',['Disponible','Adoptado']))));
});
test('HU02 consulta refugios activos de Chalco sin permisos administrativos',async()=>{
  await assertSucceeds(getDocs(query(collection(as(),'refugios'),where('activo','==',true),where('municipio','==','Chalco'),where('entidad','==','Estado de México'))));
  await assertFails(getDocs(collection(as(),'refugios')));
});
test('HU02 adoptar o consultar catálogo no concede permiso de publicar o editar',async()=>{
  const db=as('adopter');
  await assertSucceeds(getDocs(query(collection(db,'perritos'),where('publicado','==',true),where('estado','in',['Disponible','En proceso']))));
  await assertFails(publish('adopter','a','intento-catalogo'));
  await assertFails(updateDoc(doc(db,'perritos','public-a'),{nombre:'Cambio ajeno',actualizadoEn:serverTimestamp()}));
});
