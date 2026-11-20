package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.LiquidGlassBox
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.liquidGlass
import com.example.ui.theme.LocalGlassPalette
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val glass = LocalGlassPalette.current

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    LiquidGlassSurface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .liquidGlass(
                            shape = RoundedCornerShape(28.dp),
                            glowHighlighted = true,
                            elevated = true
                        )
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_glasstube_icon_src),
                        contentDescription = "GlassTube emblem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "GlassTube",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.sign_in_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.sign_in_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                LiquidGlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 26.dp,
                    contentPadding = 18.dp,
                    elevated = true
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        FeatureRow(
                            icon = Icons.Rounded.DownloadForOffline,
                            tint = MaterialTheme.colorScheme.secondary,
                            title = "4K Offline Vault",
                            subtitle = "Download videos in 2160p & watch without internet"
                        )
                        FeatureRow(
                            icon = Icons.Rounded.MusicNote,
                            tint = MaterialTheme.colorScheme.primary,
                            title = "Background & Dynamic Island",
                            subtitle = "Keep listening on home screen with live pill controls"
                        )
                        FeatureRow(
                            icon = Icons.Rounded.CloudSync,
                            tint = MaterialTheme.colorScheme.tertiary,
                            title = "Connection-Aware Sync",
                            subtitle = "Likes & watch feed sync automatically once online"
                        )
                        FeatureRow(
                            icon = Icons.Rounded.Shield,
                            tint = Color(0xFF10B981),
                            title = "100% Ad-Free Liquid Glass",
                            subtitle = "Zero ads, glowing touch feedback, Dark / White / System"
                        )
                    }
                }

                if (errorMessage != null) {
                    LiquidGlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        contentPadding = 12.dp,
                        glowHighlighted = true
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            glowHighlighted = true,
                            elevated = true
                        )
                ) {
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                    errorMessage = "Sign-in was cancelled. Tap Sign in with Google and select your account again (if you previously installed an older APK build, uninstall it once before installing the latest APK)."
                                }
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.sign_in_button),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "Powered by Google Sign-In • Safe & Encrypted",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.tapGlowPrimary.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(tint.copy(alpha = 0.28f), tint.copy(alpha = 0.10f))
                    )
                )
                .border(1.dp, tint.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val AUTH_TAG = "Auth"

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val existingUser = Firebase.auth.currentUser
    if (existingUser != null) {
        Log.d(AUTH_TAG, "attemptAutoSignIn: Firebase user already signed in (uid=${existingUser.uid})")
        onAuthSuccess()
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        Log.w(AUTH_TAG, "attemptAutoSignIn: Unable to resolve Activity from context")
        onUnauthenticated()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id).trim()
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "attemptAutoSignIn: Missing R.string.default_web_client_id", e)
        onUnauthenticated()
        return
    }
    if (clientId.isBlank()) {
        Log.e(AUTH_TAG, "attemptAutoSignIn: R.string.default_web_client_id is blank")
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            Log.d(AUTH_TAG, "attemptAutoSignIn: Requesting authorized Google credential (clientId=${clientId.take(18)}...)")
            val result = credentialManager.getCredential(activity, request)
            val cred = result.credential
            Log.d(AUTH_TAG, "attemptAutoSignIn: Credential received class=${cred::class.java.simpleName}, type=${cred.type}")
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                val token = googleCred.idToken
                if (token.isBlank()) {
                    Log.e(AUTH_TAG, "attemptAutoSignIn: Parsed GoogleIdTokenCredential has blank idToken")
                    onUnauthenticated()
                    return@launch
                }
                Log.d(AUTH_TAG, "attemptAutoSignIn: Signing into Firebase with Google ID token (len=${token.length}, id=${googleCred.id})")
                val authResult = Firebase.auth
                    .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .await()
                Log.i(AUTH_TAG, "attemptAutoSignIn: Firebase sign-in succeeded (uid=${authResult.user?.uid})")
                onAuthSuccess()
            } else {
                Log.w(AUTH_TAG, "attemptAutoSignIn: Unexpected credential type=${cred.type}")
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.d(AUTH_TAG, "attemptAutoSignIn: No previously authorized Google credentials found (${e.message})")
            onUnauthenticated()
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "attemptAutoSignIn: Cancelled (${e.message})", e)
            onUnauthenticated()
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(AUTH_TAG, "attemptAutoSignIn: Invalid Google ID token payload", e)
            onUnauthenticated()
        } catch (e: FirebaseAuthException) {
            Log.e(AUTH_TAG, "attemptAutoSignIn: FirebaseAuth failed code=${e.errorCode}, msg=${e.message}", e)
            onUnauthenticated()
        } catch (e: GetCredentialException) {
            Log.w(AUTH_TAG, "attemptAutoSignIn: Credential Manager error type=${e.type}, msg=${e.message}", e)
            onUnauthenticated()
        } catch (e: Exception) {
            Log.w(AUTH_TAG, "attemptAutoSignIn: Unexpected failure (${e.message})", e)
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    Log.d(AUTH_TAG, "onGoogleSignInClicked: Starting interactive Google Sign-In flow")
    val activity = context.findActivity()
    if (activity == null) {
        Log.e(AUTH_TAG, "onGoogleSignInClicked: Context does not host an Activity (${context::class.java.name})")
        onAuthError("Unable to open Google Sign-In prompt from this context.")
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id).trim()
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "onGoogleSignInClicked: Failed to read R.string.default_web_client_id", e)
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    if (clientId.isBlank()) {
        Log.e(AUTH_TAG, "onGoogleSignInClicked: R.string.default_web_client_id is blank")
        onAuthError("Google Sign-In configuration error: Web Client ID is empty.")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            Log.d(
                AUTH_TAG,
                "onGoogleSignInClicked: Launching CredentialManager sheet (activity=${activity::class.java.simpleName}, pkg=${activity.packageName}, clientId=${clientId.take(18)}...)"
            )
            val response = credentialManager.getCredential(activity, request)
            val cred = response.credential
            Log.d(AUTH_TAG, "onGoogleSignInClicked: Credential returned class=${cred::class.java.simpleName}, type=${cred.type}")

            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = try {
                    GoogleIdTokenCredential.createFrom(cred.data)
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e(AUTH_TAG, "onGoogleSignInClicked: Failed to parse GoogleIdTokenCredential", e)
                    onAuthError("Received an invalid Google ID token from Play Services.")
                    return@launch
                }

                val token = googleCred.idToken
                if (token.isBlank()) {
                    Log.e(AUTH_TAG, "onGoogleSignInClicked: GoogleIdTokenCredential.idToken is blank (id=${googleCred.id})")
                    onAuthError("Google Sign-In returned an empty ID token. Please try again.")
                    return@launch
                }

                Log.d(
                    AUTH_TAG,
                    "onGoogleSignInClicked: Parsed Google ID token (len=${token.length}, account=${googleCred.id}); exchanging with Firebase Auth..."
                )
                val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
                val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
                val signedInUser = authResult.user
                if (signedInUser == null) {
                    Log.e(AUTH_TAG, "onGoogleSignInClicked: Firebase signInWithCredential completed but user is null")
                    onAuthError("Firebase authentication succeeded without an active user session.")
                    return@launch
                }
                Log.i(
                    AUTH_TAG,
                    "onGoogleSignInClicked: Firebase Auth success (uid=${signedInUser.uid}, email=${signedInUser.email}, isNew=${authResult.additionalUserInfo?.isNewUser})"
                )
                onAuthSuccess()
            } else {
                Log.e(AUTH_TAG, "onGoogleSignInClicked: Unexpected credential class=${cred::class.java.name}, type=${cred.type}")
                onAuthError("Unexpected credential type (${cred.type}) received from Google Sign-In.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(
                AUTH_TAG,
                "Google Sign-In flow cancelled or dismissed (type=${e.type}, message=${e.message}). " +
                    "If user selected an email and this fired immediately, verify APK signing SHA-1 matches Firebase google-services.json.",
                e
            )
            onAuthCancelled()
        } catch (e: NoCredentialException) {
            Log.w(AUTH_TAG, "onGoogleSignInClicked: NoCredentialException (type=${e.type}, msg=${e.message})", e)
            onAuthError("No Google account found on this device. Please add your Google/YouTube account in Android Settings -> Accounts and try again.")
        } catch (e: GetCredentialInterruptedException) {
            Log.w(AUTH_TAG, "onGoogleSignInClicked: Interrupted (type=${e.type}, msg=${e.message})", e)
            onAuthError("Google Sign-In was interrupted. Please tap Sign in with Google again.")
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(AUTH_TAG, "onGoogleSignInClicked: Provider configuration error (type=${e.type}, msg=${e.message})", e)
            onAuthError("Google Play Services credential provider is unavailable or misconfigured on this device.")
        } catch (e: GetCredentialUnsupportedException) {
            Log.e(AUTH_TAG, "onGoogleSignInClicked: Credential Manager unsupported (type=${e.type}, msg=${e.message})", e)
            onAuthError("Credential Manager Google Sign-In is not supported on this device/Play Services version.")
        } catch (e: GetCredentialException) {
            Log.e(AUTH_TAG, "onGoogleSignInClicked: CredentialManager failure type=${e.type}, msg=${e.message}", e)
            onAuthError(e.localizedMessage ?: "Google Sign-In failed (${e.type}).")
        } catch (e: FirebaseAuthException) {
            Log.e(AUTH_TAG, "onGoogleSignInClicked: FirebaseAuthException code=${e.errorCode}, msg=${e.message}", e)
            onAuthError("Firebase authentication failed (${e.errorCode}): ${e.localizedMessage ?: "Please try again."}")
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "onGoogleSignInClicked: Unexpected sign-in error (${e::class.java.simpleName})", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. Please check internet connection and try again.")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                Log.d(AUTH_TAG, "signOutUser: Clearing Credential Manager state")
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "signOutUser: Failed to clear credential state: ${e.message}", e)
            } finally {
                Log.i(AUTH_TAG, "signOutUser: Signing out from FirebaseAuth")
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
