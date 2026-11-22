import {initializeApp} from 'firebase-admin/app';
import {getFirestore} from 'firebase-admin/firestore';
import {onCall} from 'firebase-functions/v2/https';
import {programReviewOperation} from './program-review.js';
import {recordAdoptionOperation} from './record-adoption.js';
initializeApp();
export const recordAdoption = onCall({region:'us-central1',maxInstances:1},request=>recordAdoptionOperation(getFirestore(),request.auth?.uid,request.data));

export const programReview = onCall({region:"us-central1",maxInstances:1},request=>programReviewOperation(getFirestore(),request.auth?.uid,request.data));
