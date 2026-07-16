package com.vadimtoptunov.chaosbank_android.ui.auth

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vadimtoptunov.chaosbank_android.app.AuthFlow
import com.vadimtoptunov.chaosbank_android.app.AuthStage
import com.vadimtoptunov.chaosbank_android.app.LaunchOptions
import com.vadimtoptunov.chaosbank_android.core.A11y
import com.vadimtoptunov.chaosbank_android.ui.components.PrimaryButton
import com.vadimtoptunov.chaosbank_android.ui.theme.Palette

@Composable
fun AuthContainer(auth: AuthFlow, options: LaunchOptions) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.bg)
            .testTag(A11y.Auth.gate),
        contentAlignment = Alignment.Center,
    ) {
        when (auth.stage) {
            AuthStage.login -> LoginScreen(auth, options)
            AuthStage.otp -> OtpScreen(auth)
            AuthStage.passcodeSetup -> PasscodeScreen(auth, setup = true)
            AuthStage.passcodeEntry -> PasscodeScreen(auth, setup = false)
            AuthStage.unlocked -> {}
        }
    }
}

@Composable
private fun LoginScreen(auth: AuthFlow, options: LaunchOptions) {
    var showWeb by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (options.showWebLogin) showWeb = true }

    Column(
        Modifier.fillMaxSize().padding(24.dp).testTag(A11y.Auth.loginRoot),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🏛", fontSize = 44.sp)
        Text("ChaosBank", color = Palette.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Welcome back", color = Palette.muted, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))
        auth.loginError?.let {
            Text(it, color = Palette.loss, fontSize = 13.sp, modifier = Modifier.testTag(A11y.Auth.loginError))
            Spacer(Modifier.height(8.dp))
        }
        PrimaryButton("Log in", modifier = Modifier.testTag(A11y.Auth.webLoginButton)) { showWeb = true }
        Spacer(Modifier.height(8.dp))
        Text("Sign in opens a secure web page", color = Palette.muted, fontSize = 11.sp)
    }

    if (showWeb) {
        WebLoginView(
            onCancel = { showWeb = false },
            onSubmit = { u, p -> showWeb = false; auth.completeWebLogin(u, p) },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebLoginView(onCancel: () -> Unit, onSubmit: (String, String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Palette.bg).testTag(A11y.Auth.webSheet)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel, modifier = Modifier.testTag(A11y.Auth.webCancel)) {
                Text("Cancel", color = Palette.sand)
            }
            Text("Sign in", color = Palette.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.padding(end = 60.dp))
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    setBackgroundColor(0)
                    val main = Handler(Looper.getMainLooper())
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun submit(user: String, pass: String) { main.post { onSubmit(user, pass) } }
                    }, "ChaosBridge")
                    loadDataWithBaseURL(null, WEB_LOGIN_HTML, "text/html", "utf-8", null)
                }
            },
        )
    }
}

@Composable
private fun OtpScreen(auth: AuthFlow) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).testTag(A11y.Auth.otpRoot),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Enter the code", color = Palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("We sent a 6-digit code to your device.", color = Palette.muted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        AuthField(
            value = auth.otpEntry,
            tag = A11y.Auth.otpField,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(6)
                auth.otpEntry = digits
                if (digits.length == 6) auth.verifyOtp()
            },
            center = true,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (auth.otpSecondsLeft > 0) "Code expires in ${auth.otpSecondsLeft}s" else "Code expired",
            color = if (auth.otpSecondsLeft > 0) Palette.muted else Palette.loss,
            fontSize = 12.sp, modifier = Modifier.testTag(A11y.Auth.otpExpiry),
        )
        auth.otpError?.let {
            Text(it, color = Palette.loss, fontSize = 13.sp, modifier = Modifier.testTag(A11y.Auth.otpError))
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Verify", modifier = Modifier.testTag(A11y.Auth.otpSubmit)) { auth.verifyOtp() }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { auth.resendOtp() }, enabled = auth.canResend, modifier = Modifier.testTag(A11y.Auth.otpResend)) {
            Text(if (auth.canResend) "Resend code" else "Resend in ${auth.resendCooldown}s", color = if (auth.canResend) Palette.sand else Palette.muted)
        }
        Text("Dev code: ${auth.mockOtp}", color = Palette.muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.testTag(A11y.Auth.otpHint))
    }
}

@Composable
private fun PasscodeScreen(auth: AuthFlow, setup: Boolean) {
    val target = if (setup) auth.requiredPasscodeLength else (auth.storedPasscode?.length ?: auth.passcodeLength)
    Column(
        Modifier.fillMaxSize().padding(24.dp).testTag(A11y.Auth.passcodeRoot),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (setup) "Create a passcode" else "Enter passcode", color = Palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        AuthField(
            value = auth.passcodeEntry,
            tag = A11y.Auth.passcodeField,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(maxOf(target, 6))
                auth.passcodeEntry = digits
                if (digits.length >= target) if (setup) auth.setPasscode() else auth.submitPasscode()
            },
            center = true,
        )
        auth.passcodeError?.let {
            Text(it, color = Palette.loss, fontSize = 13.sp, modifier = Modifier.testTag(A11y.Auth.passcodeError))
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(if (setup) "Set passcode" else "Unlock", modifier = Modifier.testTag(A11y.Auth.passcodeSubmit)) {
            if (setup) auth.setPasscode() else auth.submitPasscode()
        }
        if (!setup) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { auth.unlockWithBiometrics() }, modifier = Modifier.testTag(A11y.Auth.biometricButton)) {
                Text("Unlock with fingerprint", color = Palette.sand)
            }
        }
    }
}

@Composable
private fun AuthField(value: String, tag: String, onValueChange: (String) -> Unit, center: Boolean) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.Monospace, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            textAlign = if (center) TextAlign.Center else TextAlign.Start, color = Palette.text,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Palette.surface, unfocusedContainerColor = Palette.surface,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

private const val WEB_LOGIN_HTML = """
<!doctype html><html><head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  body { margin:0; padding:28px 22px; background:#0E1218; color:#F4F1EA; font-family:-apple-system,system-ui,sans-serif; }
  .brand { text-align:center; margin:24px 0 28px; }
  .brand h1 { font-size:26px; margin:8px 0 4px; }
  .brand p { color:#8B95A6; font-size:13px; margin:0; }
  label { display:block; font-size:12px; color:#8B95A6; margin:0 0 6px; }
  .field { margin-bottom:16px; }
  input { width:100%; padding:14px; font-size:16px; background:#171C25; color:#F4F1EA; border:1px solid #262F3D; border-radius:12px; outline:none; box-sizing:border-box; }
  input:focus { border-color:#E9B45E; }
  button { width:100%; padding:15px; font-size:16px; font-weight:600; background:#E9B45E; color:#0E1218; border:none; border-radius:14px; margin-top:6px; }
  .hint { text-align:center; color:#8B95A6; font-size:11px; margin-top:16px; }
</style></head><body>
  <div class="brand"><div style="font-size:40px">🏦</div><h1>ChaosBank</h1><p>Secure web sign-in</p></div>
  <form onsubmit="return doSubmit(event)">
    <div class="field"><label for="web-username">Username</label><input id="web-username" type="text" autocapitalize="none"/></div>
    <div class="field"><label for="web-password">Password</label><input id="web-password" type="password"/></div>
    <button id="web-submit" type="submit">Log in</button>
  </form>
  <p class="hint">Demo: any username + password</p>
  <script>
    function doSubmit(e){ e.preventDefault();
      ChaosBridge.submit(document.getElementById('web-username').value, document.getElementById('web-password').value);
      return false; }
  </script>
</body></html>
"""
