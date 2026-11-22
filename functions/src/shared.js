import {Timestamp} from 'firebase-admin/firestore';
import {HttpsError} from 'firebase-functions/v2/https';
export function requireId(value) {
 if(typeof value!=='string'||!/^[A-Za-z0-9_-]{1,100}$/.test(value)) throw new HttpsError('invalid-argument','El identificador no es válido.');
}
export function civilTimestamp(value) {
 if(typeof value!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(value)) throw new HttpsError('invalid-argument','Escribe una fecha válida con formato AAAA-MM-DD.');
 const milliseconds=Date.parse(`${value}T00:00:00-06:00`);
 if(!Number.isFinite(milliseconds)||new Date(milliseconds-6*3600_000).toISOString().slice(0,10)!==value||milliseconds<=0) throw new HttpsError('invalid-argument','La fecha no es válida.');
 return Timestamp.fromMillis(milliseconds);
}
export async function requireMembership(tx,db,uid,shelterId) {
 const [user,member,refuge]=await tx.getAll(db.doc(`usuarios/${uid}`),db.doc(`membresias/${uid}__${shelterId}`),db.doc(`refugios/${shelterId}`));
 if(user.get('activo')!==true||member.get('activo')!==true||member.get('usuarioUid')!==uid||member.get('refugioId')!==shelterId||refuge.get('activo')!==true) throw new HttpsError('permission-denied','No tienes una membresía activa para este refugio.');
}
