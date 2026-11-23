import {before,after,beforeEach,test} from 'node:test';
import {readFile} from 'node:fs/promises';
import assert from 'node:assert/strict';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,setDoc,getDoc,updateDoc,deleteDoc,writeBatch,serverTimestamp,Timestamp} from 'firebase/firestore';
let env;
const as=uid=>uid?env.authenticatedContext(uid).firestore():env.unauthenticatedContext().firestore();
const ref=(db,p)=>doc(db,p);
const admin=f=>env.withSecurityRulesDisabled(c=>f(c.firestore()));
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-huellitas-chalco',firestore:{host:'127.0.0.1',port:8180,rules:await readFile(new URL('../../../firestore.rules',import.meta.url),'utf8')}})});
after(async()=>env?.cleanup());
beforeEach(async()=>{
 await env.clearFirestore();
 await admin(async db=>{
  const fixture={
   'usuarios/a':{activo:true},'usuarios/b':{activo:true},'usuarios/owner':{activo:true},
   'refugios/a':{activo:true},'refugios/b':{activo:true},
   'membresias/a__a':{usuarioUid:'a',refugioId:'a',activo:true},'membresias/b__b':{usuarioUid:'b',refugioId:'b',activo:true},
   'perritos/dog':{refugioId:'a',nombre:'Prueba',edadMeses:12,tamano:'Mediano',sexo:'Macho',descripcion:'Descripción de prueba suficientemente larga para validar la ficha.',vacunacion:'No',desparasitacion:'No',esterilizacion:'No',estado:'Disponible',publicado:true,esDemostracion:true,fotos:[{archivoId:'photo',ruta:'perritos/a/a/dog/photo.jpg',orden:0}],creadoEn:Timestamp.now(),actualizadoEn:Timestamp.now()},
   'perritos_control/dog':{refugioId:'a',solicitudAprobadaId:null,adopcionId:null},
   'solicitudes/request':{refugioId:'a',perritoId:'dog',usuarioUid:'owner',estado:'Enviada',motivoNoAprobacion:null},
  };
  for(const [p,d] of Object.entries(fixture))await setDoc(ref(db,p),d);
 });
});
const fields=(uid='a',patch={})=>({eliminado:true,eliminadoEn:serverTimestamp(),eliminadoPorUid:uid,publicado:false,actualizadoEn:serverTimestamp(),...patch});
const remove=(uid='a',patch={})=>updateDoc(ref(as(uid),'perritos/dog'),fields(uid,patch));
test('Eliminar retira del catálogo y conserva ficha, control y solicitudes',async()=>{
 await assertSucceeds(remove());
 const dog=await getDoc(ref(as('a'),'perritos/dog'));assert.equal(dog.get('eliminado'),true);assert.equal(dog.get('estado'),'Disponible');
 await assertFails(getDoc(ref(as(null),'perritos/dog')));
 assert.equal((await getDoc(ref(as('a'),'perritos_control/dog'))).get('adopcionId'),null);
 assert.equal((await getDoc(ref(as('owner'),'solicitudes/request'))).get('estado'),'Enviada');
});
test('Solo el responsable del mismo refugio puede eliminar de la lista',async()=>{
 for(const uid of ['b','owner',null])await assertFails(remove(uid));await assertSucceeds(remove());
});
test('Eliminar no permite falsificar autor, fecha, publicación ni modificar otros datos',async()=>{
 for(const patch of [{eliminadoPorUid:'b'},{eliminadoEn:Timestamp.fromMillis(1)},{actualizadoEn:Timestamp.fromMillis(1)},{publicado:true},{eliminado:false},{nombre:'Otro'},{estado:'Adoptado'},{refugioId:'b'}])await assertFails(remove('a',patch));
 await assertSucceeds(remove());
});
test('Una adopción en proceso bloquea la eliminación incluso desde un cliente modificado',async()=>{
 await admin(async db=>{await updateDoc(ref(db,'perritos/dog'),{estado:'En proceso'});await updateDoc(ref(db,'perritos_control/dog'),{solicitudAprobadaId:'request'});});
 await assertFails(remove());
});
test('El control bloquea la eliminación si una aprobación ya fue guardada',async()=>{
 await admin(db=>updateDoc(ref(db,'perritos_control/dog'),{solicitudAprobadaId:'request'}));await assertFails(remove());
});
test('Un adoptado se oculta de la administración conservando el acceso de su adoptante',async()=>{
 await admin(async db=>{await updateDoc(ref(db,'perritos/dog'),{estado:'Adoptado'});await updateDoc(ref(db,'perritos_control/dog'),{solicitudAprobadaId:'request',adopcionId:'dog'});await setDoc(ref(db,'adopciones/dog'),{usuarioUid:'owner',refugioId:'a',perritoId:'dog'});});
 await assertFails(remove());
 await assertSucceeds(remove('a',{publicado:true}));
 await assertSucceeds(getDoc(ref(as('owner'),'perritos/dog')));
 await assertFails(getDoc(ref(as(null),'perritos/dog')));
 await assertFails(getDoc(ref(as('b'),'perritos/dog')));
});
test('No se puede restaurar, editar ni borrar físicamente una ficha retirada desde la app',async()=>{
 await assertSucceeds(remove());
 for(const patch of [{eliminado:false,publicado:true},{vacunacion:'Sí'},{nombre:'Otro nombre'},{eliminadoPorUid:'b'}])await assertFails(updateDoc(ref(as('a'),'perritos/dog'),{...patch,actualizadoEn:serverTimestamp()}));
 await assertFails(deleteDoc(ref(as('a'),'perritos/dog')));
});
test('Una membresía revocada no elimina fichas',async()=>{
 await admin(db=>updateDoc(ref(db,'membresias/a__a'),{activo:false}));await assertFails(remove());
});
test('Las solicitudes conservadas se pueden revisar y no aprobar después de retirar al perrito',async()=>{
 await assertSucceeds(remove());
 const decide=async(before,after,reason,suffix)=>{
  const db=as('a'),batch=writeBatch(db);
  batch.update(ref(db,'solicitudes/request'),{estado:after,motivoNoAprobacion:reason,actualizadoEn:serverTimestamp()});
  batch.set(ref(db,`historial_solicitudes/request-${suffix}`),{solicitudId:'request',usuarioUid:'owner',refugioId:'a',estadoAnterior:before,estadoNuevo:after,motivo:reason,actorUid:'a',creadoEn:serverTimestamp()});
  return batch.commit();
 };
 await assertSucceeds(decide('Enviada','En revisión',null,'revision'));
 await assertFails(decide('En revisión','Aprobada',null,'aprobada'));
 await assertSucceeds(decide('En revisión','No aprobada','El refugio retiró al perrito del catálogo.','no-aprobada'));
 await assertSucceeds(getDoc(ref(as('owner'),'historial_solicitudes/request-no-aprobada')));
});
