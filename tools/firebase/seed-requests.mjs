// Escenario escolar exclusivamente en demo-huellitas-chalco, emuladores locales.
import {initializeTestEnvironment} from '@firebase/rules-unit-testing';
import {initializeApp,deleteApp} from 'firebase/app';
import {getAuth,connectAuthEmulator,createUserWithEmailAndPassword,signInWithEmailAndPassword} from 'firebase/auth';
import {doc,setDoc,Timestamp,collection,query,where,getDocs,deleteDoc} from 'firebase/firestore';
import {ref,uploadBytes} from 'firebase/storage';
import {readFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
const projectId='demo-huellitas-chalco';
const app=initializeApp({apiKey:'demo-firebase-auth-emulator-key',projectId},'hu12-seed');
const auth=getAuth(app);connectAuthEmulator(auth,'http://127.0.0.1:9099',{disableWarnings:true});
const env=await initializeTestEnvironment({projectId,firestore:{host:'127.0.0.1',port:8180},storage:{host:'127.0.0.1',port:9199}});
try {
  const owners=[];
  for(const email of ['hu07-adopter@example.test','hu12-second@example.test']) {
    let result;
    try {result=await createUserWithEmailAndPassword(auth,email,'PruebaHU07!');}
    catch(error) {if(error.code!=='auth/email-already-in-use') throw error;result=await signInWithEmailAndPassword(auth,email,'PruebaHU07!');}
    owners.push({uid:result.user.uid,email});
  }
  const image=await readFile(new URL('./fixtures/catalog-demo.jpg',import.meta.url));
  await env.withSecurityRulesDisabled(async ctx=>{
    const db=ctx.firestore();
    for(const code of ['a','b']) {
      const dogId=`hu12-dog-${code}`,refugioId=`hu07-${code}`,ruta=`perritos/${refugioId}/fixture/${dogId}/foto.jpg`,stamp=Timestamp.now();
      for(const name of ['revisiones','seguimientos']) {
        const old=await getDocs(query(collection(db,name),where('adopcionId','==',dogId)));
        for(const item of old.docs) await deleteDoc(item.ref);
      }
      await deleteDoc(doc(db,'adopciones',dogId));
      await uploadBytes(ref(ctx.storage('gs://demo-huellitas-chalco.appspot.com'),ruta),image,{contentType:'image/jpeg'});
      await setDoc(doc(db,'perritos',dogId),{refugioId,nombre:`Perrito solicitudes ${code.toUpperCase()}`,edadMeses:12,tamano:'Mediano',sexo:'Macho',descripcion:'Perrito ficticio del escenario de solicitudes escolares. No representa un animal real.',vacunacion:'Sin información',desparasitacion:'Sin información',esterilizacion:'Sin información',estado:'Disponible',publicado:true,esDemostracion:true,fotos:[{archivoId:'foto',ruta,orden:0}],creadoEn:stamp,actualizadoEn:stamp});
      await setDoc(doc(db,'perritos_control',dogId),{refugioId,solicitudAprobadaId:null,adopcionId:null});
      for(const [index,owner] of owners.entries()) {
        if(code==='b'&&index===1) continue;
        const id=`12000000-0000-4000-8000-0000000000${code}${index}`,folio=`HC-${id}`;
        const oldHistory=await getDocs(query(collection(db,'historial_solicitudes'),where('solicitudId','==',id)));
        for(const event of oldHistory.docs) await deleteDoc(event.ref);
        await setDoc(doc(db,'solicitudes',id),{folio,usuarioUid:owner.uid,perritoId:dogId,refugioId,estado:'Enviada',contacto:{nombre:`Solicitante demo ${index+1}`,correo:owner.email,telefono:`550000001${index}`},hogar:{tipoVivienda:'CASA',tenencia:'PROPIA',espacioExterior:true,personas:3,horasSolo:'DE_4_A_8'},otrasMascotas:{tiene:false,detalle:null},motivos:'Quiero darle cuidados y compañía en un hogar seguro durante toda su vida.',motivoNoAprobacion:null,creadoEn:stamp,actualizadoEn:stamp});
        const hash=createHash('sha256').update(`${owner.uid}\0${dogId}`).digest('hex');
        await setDoc(doc(db,'solicitudes_claves',hash),{usuarioUid:owner.uid,perritoId:dogId,solicitudId:id});
        await setDoc(doc(db,'historial_solicitudes',`${id}-1`),{solicitudId:id,usuarioUid:owner.uid,refugioId,estadoAnterior:null,estadoNuevo:'Enviada',motivo:null,actorUid:owner.uid,creadoEn:stamp});
      }
    }
  });
  console.log('HU-12: dos solicitudes ficticias para A y una para B; ejecutar seed:local primero.');
} finally {await env.cleanup();await deleteApp(app);}
