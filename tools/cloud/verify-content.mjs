import {readFileSync} from 'node:fs';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
const base='https://firestore.googleapis.com/v1/projects/huellitas-chalco/databases/(default)/documents';
const source=JSON.parse(readFileSync('tools/cloud/content/catalogo.json','utf8'));
const bad=/fictici|demostraci|\bdemo\b|\bpruebas?\b|escolar|académic|lorem ipsum|placeholder visible|texto de muestra|simulad/i;
function decode(v){if('stringValue'in v)return v.stringValue;if('booleanValue'in v)return v.booleanValue;if('integerValue'in v)return Number(v.integerValue);if('arrayValue'in v)return(v.arrayValue.values??[]).map(decode);if('mapValue'in v)return Object.fromEntries(Object.entries(v.mapValue.fields??{}).map(([k,x])=>[k,decode(x)]));return null;}
const data=d=>Object.fromEntries(Object.entries(d.fields??{}).map(([k,x])=>[k,decode(x)]));
async function get(path){const r=await fetch(base+'/'+path);assert.equal(r.status,200,path);return data(await r.json());}
const r=await fetch(base+':runQuery',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({structuredQuery:{from:[{collectionId:'perritos'}],where:{compositeFilter:{op:'AND',filters:[{fieldFilter:{field:{fieldPath:'publicado'},op:'EQUAL',value:{booleanValue:true}}},{fieldFilter:{field:{fieldPath:'estado'},op:'IN',value:{arrayValue:{values:[{stringValue:'Disponible'},{stringValue:'En proceso'}]}}}}]}}}})});
assert.equal(r.status,200,'Consulta pública');const rows=await r.json();const dogs=rows.filter(x=>x.document).map(x=>({id:x.document.name.split('/').at(-1),...data(x.document)})).filter(d=>d.eliminado!==true);
assert.equal(dogs.length,17);
for(const dog of dogs){for(const k of ['nombre','descripcion','estado','vacunacion','desparasitacion','esterilizacion'])assert(!bad.test(dog[k]),dog.nombre+':'+k);assert(dog.descripcion.length<=2000);}
for(const wanted of source.nuevos){const dog=dogs.find(d=>d.id===wanted.id);assert(dog,wanted.id);assert.equal(dog.nombre,wanted.nombre);assert.equal(dog.descripcion,wanted.descripcion);const image=await get('imagenes/'+createHash('sha256').update(dog.fotos[0].ruta).digest('hex'));const bytes=Buffer.from(image.datos,'base64');assert(bytes.length<=102400);assert.equal(bytes[0],255);assert.equal(bytes[1],216);assert(bytes.equals(readFileSync('tools/cloud/content/'+wanted.foto)));}
const shelter=await get('refugios/'+source.refugioId);for(const [k,v]of Object.entries(source.refugio))assert.deepEqual(shelter[k],v);
console.log('PASS lectura pública sin cuenta: 17 perritos, 10 altas con JPEG íntegro <=100 KiB, fichas y refugio sin avisos técnicos.');
console.log(JSON.stringify({total:dogs.length,chicos:dogs.filter(d=>d.tamano==='Chico').length,medianos:dogs.filter(d=>d.tamano==='Mediano').length,grandes:dogs.filter(d=>d.tamano==='Grande').length,cachorros:dogs.filter(d=>d.edadMeses<12).length,adultos:dogs.filter(d=>d.edadMeses>=12).length,hembras:dogs.filter(d=>d.sexo==='Hembra').length,machos:dogs.filter(d=>d.sexo==='Macho').length}));
