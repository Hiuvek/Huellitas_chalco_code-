// Datos escolares explícitos en el proyecto real. Nunca reemplaza registros existentes.
import {createRequire} from 'node:module';
import {spawnSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
const require=createRequire(import.meta.url);
const project='huellitas-chalco',shelter='refugio-prueba-chalco';
const {getGlobalDefaultAccount,getAccessToken}=require('../firebase/node_modules/firebase-tools/lib/auth.js');
const {configstore}=require('../firebase/node_modules/firebase-tools/lib/configstore.js');
const account=getGlobalDefaultAccount();
if(!account)throw new Error('Se necesita la sesión Firebase CLI autorizada.');
const token=await getAccessToken(account.tokens.refresh_token,configstore.get('loginScopes'));
const headers={Authorization:`Bearer ${token.access_token}`,'Content-Type':'application/json'};
const base=`https://firestore.googleapis.com/v1/projects/${project}/databases/(default)/documents`;
async function read(path) {
 const response=await fetch(`${base}/${path}`,{headers});
 if(response.status===404)return null;
 if(!response.ok)throw new Error(`No se pudo consultar ${path}.`);
 return response.json();
}
const refuge=await read(`refugios/${shelter}`);
if(refuge?.fields?.esDemostracion?.booleanValue!==true||refuge?.fields?.activo?.booleanValue!==true)throw new Error('Se requiere el refugio de demostración activo.');
const fixtures=[
 ['luna','Luna',6,'Chico','Hembra'],['max','Max',18,'Grande','Macho'],
 ['nala','Nala',24,'Mediano','Hembra'],['rocky','Rocky',36,'Grande','Macho'],
 ['kira','Kira',12,'Chico','Hembra'],['toby','Toby',48,'Mediano','Macho'],
 ['maya','Maya',8,'Mediano','Hembra'],['bruno','Bruno',72,'Grande','Macho'],
];
function encode(value) {
 if(value===null)return {nullValue:null};
 if(typeof value==='string')return {stringValue:value};
 if(typeof value==='boolean')return {booleanValue:value};
 if(typeof value==='number')return {integerValue:String(value)};
 if(Array.isArray(value))return {arrayValue:{values:value.map(encode)}};
 return {mapValue:{fields:fields(value)}};
}
const fields=value=>Object.fromEntries(Object.entries(value).map(([key,v])=>[key,encode(v)]));
const writes=[],report=[];
for(const [index,[key,nombre,edadMeses,tamano,sexo]]of fixtures.entries()) {
 const id=`demo-escolar-${key}`;
 if(await read(`perritos/${id}`))throw new Error(`${id} ya existe; se conserva sin modificaciones.`);
 const source=fileURLToPath(new URL(`../../app/src/main/res/drawable-nodpi/dog_${index+1}.webp`,import.meta.url));
 const result=spawnSync('magick',[source,'-strip','-resize','640x640>','-quality','72','jpg:-'],{maxBuffer:2*1024*1024});
 if(result.status!==0||result.stdout.length<4||result.stdout.length>102400||result.stdout[0]!==255||result.stdout[1]!==216)throw new Error(`No se preparó la foto de ${nombre}.`);
 const ruta=`perritos/${shelter}/demostracion/${id}/portada.jpg`;
 const dog={refugioId:shelter,nombre,edadMeses,tamano,sexo,
  descripcion:`Ejemplo escolar: ${nombre} es un registro ficticio para probar el catálogo, los filtros y el proceso de adopción de Huellitas Chalco. Los datos y la fotografía se usan únicamente para demostración; no representa una adopción real.`,
  vacunacion:'Sin información',desparasitacion:'Sin información',esterilizacion:'Sin información',estado:'Disponible',publicado:true,esDemostracion:true,
  fotos:[{archivoId:'portada',ruta,orden:0}]};
 const records=[
  [`perritos/${id}`,dog],
  [`perritos_control/${id}`,{refugioId:shelter,solicitudAprobadaId:null,adopcionId:null}],
  [`imagenes/${createHash('sha256').update(ruta).digest('hex')}`,{ruta,refugioId:shelter,perritoId:id,revisionId:null,datos:result.stdout.toString('base64')}],
 ];
 for(const [path,data]of records)writes.push({update:{name:`projects/${project}/databases/(default)/documents/${path}`,fields:fields(data)},currentDocument:{exists:false},
  ...(path.startsWith('perritos/')?{updateTransforms:[{fieldPath:'creadoEn',setToServerValue:'REQUEST_TIME'},{fieldPath:'actualizadoEn',setToServerValue:'REQUEST_TIME'}]}:{})});
 report.push({id,nombre,edadMeses,tamano,sexo,fotoBytes:result.stdout.length});
}
if(process.argv.includes('--write')) {
 const response=await fetch(base+':commit',{method:'POST',headers,body:JSON.stringify({writes})});
 if(!response.ok)throw new Error(`No se crearon los datos; commit rechazado (${response.status}). No se reemplazaron documentos existentes.`);
 console.log('Se crearon ocho perritos de demostración, sus controles e imágenes en Firebase real.');
}else console.log('Vista previa; utiliza --write para crear estos datos en Firebase real.');
console.log(JSON.stringify(report,null,2));
