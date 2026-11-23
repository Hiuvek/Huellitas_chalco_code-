// Actualización editorial de campos existentes. No modifica estados ni relaciones.
import {createRequire} from 'node:module';
import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
const require=createRequire(import.meta.url);
const {getGlobalDefaultAccount,getAccessToken}=require('../firebase/node_modules/firebase-tools/lib/auth.js');
const {configstore}=require('../firebase/node_modules/firebase-tools/lib/configstore.js');
const project='huellitas-chalco',root=`projects/${project}/databases/(default)/documents`;
const base=`https://firestore.googleapis.com/v1/${root}`;
const content=JSON.parse(readFileSync(new URL('./content/catalogo.json',import.meta.url),'utf8'));
const account=getGlobalDefaultAccount();if(!account)throw Error('Falta la sesión autorizada de Firebase CLI.');
const token=await getAccessToken(account.tokens.refresh_token,configstore.get('loginScopes'));
const headers={Authorization:`Bearer ${token.access_token}`,'Content-Type':'application/json'};
const forbidden=/fictici|demostraci|\bdemo\b|\bpruebas?\b|escolar|académic|lorem ipsum|placeholder visible|texto de muestra|simulad/i;
function encode(v){if(v===null)return{nullValue:null};if(typeof v==='string')return{stringValue:v};if(typeof v==='boolean')return{booleanValue:v};if(typeof v==='number')return{integerValue:String(v)};if(Array.isArray(v))return{arrayValue:{values:v.map(encode)}};return{mapValue:{fields:fields(v)}};}
const fields=v=>Object.fromEntries(Object.entries(v).map(([k,x])=>[k,encode(x)]));
const decode=v=>'stringValue'in v?v.stringValue:'booleanValue'in v?v.booleanValue:'integerValue'in v?Number(v.integerValue):'nullValue'in v?null:'arrayValue'in v?(v.arrayValue.values??[]).map(decode):'mapValue'in v?Object.fromEntries(Object.entries(v.mapValue.fields??{}).map(([k,x])=>[k,decode(x)])):v;
const data=d=>Object.fromEntries(Object.entries(d.fields??{}).map(([k,x])=>[k,decode(x)]));
async function read(path){const r=await fetch(`${base}/${path}`,{headers});if(r.status===404)return null;if(!r.ok)throw Error(`Lectura rechazada: ${path} (${r.status})`);return r.json();}
const writes=[],before=[],report={refugio:content.refugio.nombre,actuales:[],nuevos:[],textosHistoricos:[]};
function patch(doc,values,mask=Object.keys(values),timestamp=true){before.push(doc);writes.push({update:{name:doc.name,fields:fields(values)},updateMask:{fieldPaths:mask},currentDocument:{updateTime:doc.updateTime},...(timestamp?{updateTransforms:[{fieldPath:'actualizadoEn',setToServerValue:'REQUEST_TIME'}]}:{})});}
function create(path,values,timestamp=false){writes.push({update:{name:`${root}/${path}`,fields:fields(values)},currentDocument:{exists:false},...(timestamp?{updateTransforms:[{fieldPath:'creadoEn',setToServerValue:'REQUEST_TIME'},{fieldPath:'actualizadoEn',setToServerValue:'REQUEST_TIME'}]}:{})});}
function validateText(v,min,max){if(typeof v!=='string'||v.trim()!==v||v.length<min||v.length>max||forbidden.test(v))throw Error('Contenido inválido o fuera de los límites.');}
const refuge=await read(`refugios/${content.refugioId}`);if(!refuge||data(refuge).activo!==true)throw Error('Refugio no disponible.');
validateText(content.refugio.nombre,1,100);validateText(content.refugio.zonaPublica,1,250);validateText(content.refugio.horarioPublico,1,250);
for(const key of ['requisitos','pasos']){const list=content.refugio[key];if(!Array.isArray(list)||list.length<1||list.length>10)throw Error('Lista no válida');list.forEach(v=>validateText(v,1,300));}
patch(refuge,content.refugio);
for(const entry of content.actuales){const doc=await read(`perritos/${entry.id}`);if(!doc)throw Error('Falta un registro actual.');const d=data(doc);if(d.refugioId!==content.refugioId)throw Error('Refugio diferente.');
 const health=`Vacunación: ${d.vacunacion??'Sin información'}. Desparasitación: ${d.desparasitacion??'Sin información'}. Esterilización: ${d.esterilizacion??'Sin información'}. El refugio confirmará los cuidados y pendientes durante la entrevista.`;
 const descripcion=`${entry.perfil}\n\nConvivencia\n${entry.convivencia}\n\nSalud\n${health}\n\nHogar ideal\n${entry.hogar}`;
 validateText(entry.nombre,1,60);validateText(descripcion,40,2000);
 patch(doc,{nombre:entry.nombre,descripcion});report.actuales.push({id:entry.id,nombre:entry.nombre,estado:d.estado,retirado:d.eliminado===true});
}
if(content.nuevos.length!==10||new Set(content.nuevos.map(d=>d.id)).size!==10)throw Error('Se requieren diez registros distintos.');
for(const entry of content.nuevos){validateText(entry.nombre,1,60);validateText(entry.descripcion,40,2000);if(!['Chico','Mediano','Grande'].includes(entry.tamano)||!['Hembra','Macho'].includes(entry.sexo)||entry.edadMeses<0||entry.edadMeses>360)throw Error('Ficha no válida.');
 for(const k of ['vacunacion','desparasitacion','esterilizacion'])if(!['Sí','No','Sin información'].includes(entry[k]))throw Error('Salud no válida.');
 if(await read(`perritos/${entry.id}`))throw Error(`${entry.id} ya existe; no se reemplaza.`);
 const photo=readFileSync(new URL('./content/'+entry.foto,import.meta.url));if(photo.length>102400||photo[0]!==255||photo[1]!==216)throw Error('Se requiere JPEG de hasta 100 KiB.');
 const ruta=`perritos/${content.refugioId}/catalogo/${entry.id}/portada.jpg`,imageId=createHash('sha256').update(ruta).digest('hex');
 for(const path of [`perritos_control/${entry.id}`,`imagenes/${imageId}`])if(await read(path))throw Error('Existe un documento relacionado; no se reemplaza.');
 const {foto,id,...dog}=entry;
 create(`perritos/${id}`,{...dog,refugioId:content.refugioId,estado:'Disponible',publicado:true,esDemostracion:false,fotos:[{archivoId:'portada',ruta,orden:0}]},true);
 create(`perritos_control/${id}`,{refugioId:content.refugioId,solicitudAprobadaId:null,adopcionId:null});
 create(`imagenes/${imageId}`,{ruta,refugioId:content.refugioId,perritoId:id,revisionId:null,datos:photo.toString('base64')});
 report.nuevos.push({id,nombre:dog.nombre,fotoBytes:photo.length});
}
// Únicamente textos de los registros técnicos conocidos; nunca cambia respuestas ajenas.
const request=await read('solicitudes/507899cc-6f67-4bf2-af73-bddd2c7fb005');
if(request){const d=data(request);if(d.contacto?.nombre==='Adoptante demostración escolar'&&d.motivos==='Demostración escolar ficticia para verificar la solicitud y el seguimiento de una adopción en la nube.'){
 patch(request,{contacto:{...d.contacto,nombre:'Alex Rivera'},motivos:'Busco un compañero para compartir paseos diarios y una rutina tranquila. En casa contamos con un espacio seguro y tiempo para acompañar su adaptación.'},['contacto.nombre','motivos'],false);report.textosHistoricos.push('Nombre y motivos de una solicitud');}}
const follow=await read('seguimientos/d33605d7-28df-4a66-91cc-ac6fae7f3e36');
if(follow&&data(follow).observaciones==='Resultado ficticio de prueba escolar: se comprueba la persistencia del seguimiento en la nube.'){
 patch(follow,{observaciones:'Se adaptó a sus horarios de comida y descanso. Disfruta los paseos con su familia y busca compañía al volver a casa. Se mantendrá comunicación para el siguiente seguimiento.'},['observaciones'],false);report.textosHistoricos.push('Observaciones de un seguimiento');}
if(process.argv.includes('--write')){
 const backup=process.env.HUELLITAS_CONTENT_BACKUP;if(!backup)throw Error('Indica una ruta privada de respaldo en HUELLITAS_CONTENT_BACKUP.');
 mkdirSync(new URL('.',`file://${backup}`).pathname,{recursive:true});writeFileSync(backup,JSON.stringify({before,created:writes.filter(w=>w.currentDocument.exists===false).map(w=>w.update.name)},null,2),{mode:0o600,flag:'wx'});
 const r=await fetch(base+':commit',{method:'POST',headers,body:JSON.stringify({writes})});if(!r.ok)throw Error(`Actualización rechazada (${r.status}); el commit es atómico.`);
 console.log('Contenido actualizado en Firebase.');
}else console.log('Vista previa; no se ha escrito en Firebase.');
console.log(JSON.stringify({...report,escrituras:writes.length},null,2));
