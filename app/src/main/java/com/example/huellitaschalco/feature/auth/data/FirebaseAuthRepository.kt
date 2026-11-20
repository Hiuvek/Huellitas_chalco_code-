package com.example.huellitaschalco.feature.auth.data

import com.example.huellitaschalco.feature.auth.domain.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth?,
    override val usesEmulator: Boolean = false,
) : AuthRepository {
    private val current = MutableStateFlow(auth?.currentUser?.toAuthUser())
    override val user = current.asStateFlow()
    override val isConfigured
        get() = auth != null

    private var pendingProfile: FirebaseUser? = null

    init {
        auth?.addAuthStateListener { current.value = it.currentUser?.toAuthUser() }
    }

    override suspend fun signUp(name: String, email: String, password: String) {
        val client = requireClient()
        val created =
            pendingProfile?.takeIf {
                it.uid == client.currentUser?.uid && it.email.equals(email, ignoreCase = true)
            }
                ?: firebaseCall {
                    client.createUserWithEmailAndPassword(email, password).await().user
                }
                ?: throw AuthFailure(AuthProblem.Unknown)
        pendingProfile = created
        try {
            created
                .updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                .await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw AuthFailure(AuthProblem.ProfileIncomplete)
        }
        pendingProfile = null
        current.value = created.toAuthUser()
    }

    override suspend fun signIn(email: String, password: String) {
        val result = firebaseCall {
            requireClient().signInWithEmailAndPassword(email, password).await()
        }
        pendingProfile = null
        current.value = result.user?.toAuthUser()
    }

    override fun signOut() {
        auth?.signOut()
        pendingProfile = null
        current.value = null
    }

    private fun requireClient() = auth ?: throw AuthFailure(AuthProblem.Unavailable)

    private suspend fun <T> firebaseCall(block: suspend () -> T): T =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (known: AuthFailure) {
            throw known
        } catch (error: Exception) {
            throw AuthFailure(
                when (error) {
                    is FirebaseNetworkException -> AuthProblem.Network
                    is FirebaseTooManyRequestsException -> AuthProblem.TooManyAttempts
                    is FirebaseAuthUserCollisionException -> AuthProblem.EmailInUse
                    is FirebaseAuthWeakPasswordException -> AuthProblem.WeakPassword
                    is FirebaseAuthException ->
                        when (error.errorCode) {
                            "ERROR_INVALID_EMAIL" -> AuthProblem.InvalidEmail
                            "ERROR_USER_DISABLED" -> AuthProblem.AccountDisabled
                            "ERROR_OPERATION_NOT_ALLOWED",
                            "ERROR_APP_NOT_AUTHORIZED",
                            "ERROR_INVALID_API_KEY" -> AuthProblem.Unavailable
                            "ERROR_WRONG_PASSWORD",
                            "ERROR_USER_NOT_FOUND",
                            "ERROR_INVALID_CREDENTIAL",
                            "ERROR_INVALID_LOGIN_CREDENTIALS" -> AuthProblem.InvalidCredentials
                            else -> AuthProblem.Unknown
                        }
                    else -> AuthProblem.Unknown
                }
            )
        }

    private fun FirebaseUser.toAuthUser() = AuthUser(uid, email.orEmpty(), displayName.orEmpty())
}
