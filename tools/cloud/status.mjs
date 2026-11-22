// Consulta solo lectura. Reutiliza la sesión autorizada de la CLI fijada en tools/firebase.
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
const require=createRequire(import.meta.url);
const project='huellitas-chalco';
const {getGlobalDefaultAccount,getAccessToken}=require('../firebase/node_modules/firebase-tools/lib/auth.js');
const {configstore}=require('../firebase/node_modules/firebase-tools/lib/configstore.js');
const account=getGlobalDefaultAccount();
if(!account) throw new Error('Inicia sesión con Firebase CLI en esta computadora.');
const tokens=await getAccessToken(account.tokens.refresh_token,configstore.get('loginScopes'));
const headers={Authorization:`Bearer ${tokens.access_token}`};
async function get(url) {
  const response=await fetch(url,{headers});
  return {ok:response.ok,status:response.status,data:await response.json()};
}
const billing=await get(`https://cloudbilling.googleapis.com/v1/projects/${project}/billingInfo`);
const indexes=await get(`https://firestore.googleapis.com/v1/projects/${project}/databases/(default)/collectionGroups/-/indexes`);
if(!billing.ok||!indexes.ok) throw new Error('No se pudo consultar la facturación o los índices con esta sesión.');
const config=JSON.parse(readFileSync(new URL('../../app/google-services.json',import.meta.url),'utf8'));
if(config.project_info?.project_id!==project) throw new Error('La configuración no corresponde a huellitas-chalco.');
const state={
  project,
  billingEnabled:billing.data.billingEnabled===true,
  services:['Authentication','Firestore'],
  indexes:(indexes.data.indexes??[]).map(i=>({collection:i.name.split('/collectionGroups/')[1]?.split('/')[0],state:i.state})),
};
console.log(JSON.stringify(state,null,2));
if(process.argv.includes('--require-ready')&&!state.indexes.every(i=>i.state==='READY')) {
  console.error('Espera a que los índices estén listos.'); process.exitCode=1;
}
if(state.billingEnabled) console.warn('El proyecto tiene facturación vinculada; la app no la requiere.');
