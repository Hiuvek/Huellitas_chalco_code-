// Datos exclusivamente del emulador; jamás se ejecuta contra el proyecto real.
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { initializeApp, deleteApp } from 'firebase/app';
import { getAuth, connectAuthEmulator, createUserWithEmailAndPassword, signInWithEmailAndPassword } from 'firebase/auth';
import { doc, setDoc, Timestamp } from 'firebase/firestore';
const projectId='demo-huellitas-chalco';
const app=initializeApp({apiKey:'demo-firebase-auth-emulator-key',projectId},'hu07-seed');
const auth=getAuth(app); connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});
const env=await initializeTestEnvironment({projectId,firestore:{host:'127.0.0.1',port:8180}});
try {
  for (const id of ['a','b','adopter']) {
    const email=`hu07-${id}@example.test`, password='PruebaHU07!';
    let result;
    try { result=await createUserWithEmailAndPassword(auth,email,password); }
    catch(e) { if(e.code!=='auth/email-already-in-use') throw e; result=await signInWithEmailAndPassword(auth,email,password); }
    const uid=result.user.uid;
    await env.withSecurityRulesDisabled(async ctx=>{
      const db=ctx.firestore();
      await setDoc(doc(db,'usuarios',uid),{uid,nombre:`Prueba ${id}`,correo:email,activo:true,creadoEn:Timestamp.now()});
      if(id!=='adopter') {
        await setDoc(doc(db,'refugios',`hu07-${id}`),{nombre:`Refugio de prueba ${id.toUpperCase()}`,activo:true,municipio:'Chalco',entidad:'Estado de México',zonaPublica:'Centro de Chalco',horarioPublico:'10 a 17 h',esDemostracion:true,requisitos:[],pasos:[],actualizadoEn:Timestamp.now()});
        await setDoc(doc(db,'membresias',`${uid}__hu07-${id}`),{usuarioUid:uid,refugioId:`hu07-${id}`,activo:true,creadoEn:Timestamp.now()});
        await setDoc(doc(db,'seguimientos',`hu07-${id}`),{refugioId:`hu07-${id}`,observaciones:`Privado ${id}`});
      }
    });
  }
  console.log('Emulador preparado: hu07-a@example.test, hu07-b@example.test y hu07-adopter@example.test. Contraseña local: PruebaHU07!');
} finally { await env.cleanup(); await deleteApp(app); }
