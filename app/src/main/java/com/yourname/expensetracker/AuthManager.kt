package com.yourname.expensetracker

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Helper object managing Firebase Authentication and Google Sign-In state.
 */
object AuthManager {

    // =========================================================================
    // TODO: PASTE YOUR FIREBASE WEB CLIENT ID HERE
    // Found in Firebase Console -> Project Settings -> General -> Web Client ID
    // or inside google-services.json under oauth_client (client_type 3)
    // =========================================================================
    const val WEB_CLIENT_ID = "753392886200-ckrdo9pd1k1ia7n0qvve8d56kb3til6b.apps.googleusercontent.com"

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    /**
     * Currently authenticated Firebase user, or null if signed out.
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * True if user is currently signed in.
     */
    val isUserSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Display name of the signed-in user.
     */
    val userDisplayName: String
        get() = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "User"

    /**
     * Email address of the signed-in user.
     */
    val userEmail: String
        get() = currentUser?.email ?: "No email provided"

    /**
     * Prepares Google One Tap Sign-In intent sender.
     */
    suspend fun getGoogleSignInIntent(context: Context): IntentSender? {
        return try {
            val oneTapClient: SignInClient = Identity.getSignInClient(context)
            val signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(WEB_CLIENT_ID)
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .setAutoSelectEnabled(false)
                .build()

            val result = oneTapClient.beginSignIn(signInRequest).await()
            result.pendingIntent.intentSender
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Handles Google Sign-In intent result and authenticates with Firebase.
     */
    suspend fun handleGoogleSignInResult(context: Context, data: Intent?): Result<FirebaseUser> {
        return try {
            if (data == null) {
                return Result.failure(Exception("Sign-In was cancelled."))
            }

            val oneTapClient = Identity.getSignInClient(context)
            val credential = oneTapClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken
                ?: return Result.failure(Exception("Google ID Token is missing."))

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Firebase authentication failed."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user from Firebase and Google One Tap.
     */
    fun signOut(context: Context) {
        try {
            auth.signOut()
            Identity.getSignInClient(context).signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
