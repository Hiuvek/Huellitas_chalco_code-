import {before,after,test} from 'node:test';
import {readFile} from 'node:fs/promises';
import {initializeTestEnvironment,assertSucceeds,assertFails} from '@firebase/rules-unit-testing';
import {doc,setDoc,getDoc,updateDoc} from 'firebase/firestore';
let env;
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-huellitas-chalco',firestore:{host:'127.0.0.1',port:8180,rules:await readFile(new URL('../../../firestore.rules',import.meta.url),'utf8')}});await env.clearFirestore();});
after(async()=>{await env?.cleanup();});
test('HU14 la ficha adoptada solo sigue accesible al adoptante de la entrega y no admite escrituras directas',async()=>{
 await env.withSecurityRulesDisabled(async ctx=>{
  const db=ctx.firestore();
  await setDoc(doc(db,'perritos','delivered'),{refugioId:'a',estado:'Adoptado',publicado:true});
  await setDoc(doc(db,'adopciones','delivered'),{refugioId:'a',perritoId:'delivered',usuarioUid:'owner'});
 });
 const owner=env.authenticatedContext('owner').firestore(),other=env.authenticatedContext('other').firestore(),guest=env.unauthenticatedContext().firestore();
 await assertSucceeds(getDoc(doc(owner,'perritos','delivered')));
 for(const db of [other,guest]) {await assertFails(getDoc(doc(db,'perritos','delivered')));await assertFails(getDoc(doc(db,'adopciones','delivered')));}
 for(const db of [owner,other,guest]) await assertFails(setDoc(doc(db,'adopciones','fake'),{usuarioUid:'owner'}));
 await env.withSecurityRulesDisabled(ctx=>setDoc(doc(ctx.firestore(),'usuarios','owner'),{activo:false}));
 await assertFails(getDoc(doc(owner,'perritos','delivered')));
});
