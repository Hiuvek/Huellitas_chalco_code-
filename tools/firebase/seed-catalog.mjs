// Solo emuladores. Fichas ficticias para revisar HU-02 sin alterar registros personales.
import {initializeTestEnvironment} from '@firebase/rules-unit-testing';
import {doc,setDoc,Timestamp} from 'firebase/firestore';
import {ref,uploadBytes} from 'firebase/storage';
import {readFile} from 'node:fs/promises';
const env=await initializeTestEnvironment({projectId:'demo-huellitas-chalco',firestore:{host:'127.0.0.1',port:8180},storage:{host:'127.0.0.1',port:9199}});
try {
  const image=await readFile(new URL('./fixtures/catalog-demo.jpg',import.meta.url));
  await env.withSecurityRulesDisabled(async ctx=>{
    const db=ctx.firestore();
    for(const [id,activo,municipio] of [['hu02-chalco',true,'Chalco'],['hu02-otro',true,'Ixtapaluca'],['hu02-inactivo',false,'Chalco']])
      await setDoc(doc(db,'refugios',id),{nombre:`Refugio catálogo ${id}`,activo,municipio,entidad:'Estado de México',zonaPublica:'Zona de demostración',horarioPublico:'10 a 17 h',esDemostracion:true,requisitos:['Ser mayor de edad y presentar identificación.','Contar con un espacio seguro y agua limpia.','Aceptar entrevista y seguimiento.'],pasos:['Solicitud','Revisión','Entrevista','Entrega y carta','Seguimiento']});
    const fixtures=[
      ['available','Lunita HU02','hu02-chalco','Disponible',true,8,'Chico','Hembra'],
      ['process','Óscar HU02','hu02-chalco','En proceso',true,24,'Grande','Macho'],
      ['adopted','Adoptado HU02','hu02-chalco','Adoptado',true,24,'Mediano','Macho'],
      ['draft','Borrador HU02','hu02-chalco','Disponible',false,24,'Mediano','Hembra'],
      ['outside','Otro municipio HU02','hu02-otro','Disponible',true,24,'Mediano','Hembra'],
      ['inactive','Refugio inactivo HU02','hu02-inactivo','Disponible',true,24,'Mediano','Hembra'],
    ];
    for(const [key,nombre,refugioId,estado,publicado,edadMeses,tamano,sexo] of fixtures) {
      const id=`hu02-catalog-${key}`,ruta=`perritos/${refugioId}/fixture/${id}/foto.jpg`;
      await uploadBytes(ref(ctx.storage('gs://demo-huellitas-chalco.appspot.com'),ruta),image,{contentType:'image/jpeg'});
      if(key==='available') await uploadBytes(ref(ctx.storage('gs://demo-huellitas-chalco.appspot.com'),`perritos/${refugioId}/fixture/${id}/foto2.jpg`),image,{contentType:'image/jpeg'});
      await setDoc(doc(db,'perritos_control',id),{refugioId,solicitudAprobadaId:estado==='Disponible' ? null : 'fixture-approved',adopcionId:estado==='Adoptado' ? id : null});
      await setDoc(doc(db,'perritos',id),{refugioId,nombre,edadMeses,tamano,sexo,descripcion:'Perrito ficticio para comprobar el catálogo escolar; esta ficha no representa un animal real.',vacunacion:'Sin información',desparasitacion:'Sin información',esterilizacion:'Sin información',estado,publicado,esDemostracion:true,fotos:key==='available' ? [{archivoId:'foto',ruta,orden:0},{archivoId:'foto2',ruta:`perritos/${refugioId}/fixture/${id}/foto2.jpg`,orden:1}] : [{archivoId:'foto',ruta,orden:0}],creadoEn:Timestamp.now(),actualizadoEn:Timestamp.now()});
    }
  });
  console.log('HU-02: seis fichas ficticias preparadas; de estas seis solo Lunita y Óscar deben aparecer.');
} finally {await env.cleanup();}
