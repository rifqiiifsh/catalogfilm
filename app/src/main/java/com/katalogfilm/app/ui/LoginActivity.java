package com.katalogfilm.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.katalogfilm.app.BuildConfig;
import com.katalogfilm.app.R;
import com.katalogfilm.app.data.Repository;
import com.katalogfilm.app.databinding.ActivityLoginBinding;
import com.katalogfilm.app.util.Session;
import com.katalogfilm.app.util.SoundManager;
import com.katalogfilm.app.util.Utils;

import java.util.concurrent.Executor;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding b;
    private Repository repo;
    private CredentialManager credentialManager;
    private boolean register = false;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        Utils.insets(b.scroll, true, true, true);
        repo = Repository.get(this);
        credentialManager = CredentialManager.create(this);

        b.btnSubmit.setOnClickListener(v -> { SoundManager.tap(v); submit(); });
        b.btnToggle.setOnClickListener(v -> { SoundManager.tap(v); toggle(); });
        b.btnGuest.setOnClickListener(v -> { SoundManager.tap(v); enter(0); });
        b.btnGoogle.setOnClickListener(v -> { SoundManager.tap(v); signInWithGoogle(); });
        b.btnForgot.setOnClickListener(v -> { SoundManager.tap(v); showForgotPassword(); });
    }

    private void toggle() {
        register = !register;
        b.tilName.setVisibility(register ? View.VISIBLE : View.GONE);
        b.btnForgot.setVisibility(register ? View.GONE : View.VISIBLE);
        b.heading.setText(register ? R.string.auth_register_heading : R.string.auth_welcome_back);
        b.subtitle.setText(register ? R.string.auth_register_subtitle : R.string.auth_login_subtitle);
        b.btnSubmit.setText(register ? R.string.auth_register : R.string.auth_login);
        b.btnToggle.setText(register ? R.string.auth_have_account : R.string.auth_no_account);
        b.tilName.setError(null); b.tilEmail.setError(null); b.tilPass.setError(null);
    }

    private void submit() {
        String name = String.valueOf(b.etName.getText()).trim();
        String email = String.valueOf(b.etEmail.getText()).trim();
        String pass = String.valueOf(b.etPass.getText());
        b.tilName.setError(null); b.tilEmail.setError(null); b.tilPass.setError(null);

        boolean ok = true;
        if (register && name.isEmpty()) { b.tilName.setError(getString(R.string.error_name_required)); ok = false; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.tilEmail.setError(getString(R.string.error_email_invalid)); ok = false; }
        if (pass.length() < 8) { b.tilPass.setError(getString(R.string.error_password_min)); ok = false; }
        if (!ok) return;

        busy(true);
        Repository.Result<Long> cb = new Repository.Result<Long>() {
            @Override public void ok(Long id) { enter(id); }
            @Override public void fail(String m) { busy(false); b.tilEmail.setError(m); }
        };
        if (register) repo.register(name, email, pass, cb);
        else repo.login(email, pass, cb);
    }

    private void signInWithGoogle() {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.trim().isEmpty()) {
            Snackbar.make(b.getRoot(), R.string.google_client_id_missing, Snackbar.LENGTH_LONG).show();
            return;
        }
        b.btnGoogle.setEnabled(false);
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                BuildConfig.GOOGLE_WEB_CLIENT_ID).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();
        Executor mainExecutor = command -> runOnUiThread(command);
        credentialManager.getCredentialAsync(this, request, new CancellationSignal(), mainExecutor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            try {
                                GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(
                                        ((CustomCredential) credential).getData());
                                repo.loginWithGoogle(google.getIdToken(), authResult());
                            } catch (IllegalArgumentException e) {
                                b.btnGoogle.setEnabled(true);
                                Snackbar.make(b.getRoot(), R.string.google_token_invalid, Snackbar.LENGTH_LONG).show();
                            }
                        } else {
                            b.btnGoogle.setEnabled(true);
                            Snackbar.make(b.getRoot(), R.string.google_credential_invalid, Snackbar.LENGTH_LONG).show();
                        }
                    }
                    @Override public void onError(GetCredentialException e) {
                        b.btnGoogle.setEnabled(true);
                        Snackbar.make(b.getRoot(), getString(R.string.google_signin_failed, e.getMessage()), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private Repository.Result<Long> authResult() {
        return new Repository.Result<Long>() {
            @Override public void ok(Long id) { enter(id); }
            @Override public void fail(String message) {
                b.btnGoogle.setEnabled(true);
                Snackbar.make(b.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        };
    }

    private void showForgotPassword() {
        EditText emailInput = new EditText(this);
        emailInput.setSingleLine(true);
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setHint(R.string.auth_email_hint);
        emailInput.setText(b.etEmail.getText());
        int pad = Utils.dp(this, 20);
        emailInput.setPadding(pad, pad, pad, pad);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.forgot_title)
                .setMessage(R.string.forgot_message)
                .setView(emailInput)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.send_reset_link, (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Snackbar.make(b.getRoot(), R.string.error_email_invalid, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    b.btnForgot.setEnabled(false);
                    repo.forgotPassword(email, new Repository.Result<String>() {
                        @Override public void ok(String message) {
                            b.btnForgot.setEnabled(true);
                            Snackbar.make(b.getRoot(), message, Snackbar.LENGTH_LONG).show();
                        }
                        @Override public void fail(String message) {
                            b.btnForgot.setEnabled(true);
                            Snackbar.make(b.getRoot(), message, Snackbar.LENGTH_LONG).show();
                        }
                    });
                }).show();
    }

    private void busy(boolean on) {
        b.btnSubmit.setEnabled(!on);
        b.btnSubmit.setText(on ? R.string.processing : (register ? R.string.auth_register : R.string.auth_login));
    }

    private void enter(long id) {
        Session session = Session.get(this);
        session.login(id);
        SoundManager.success(this);
        Class<?> next = session.genres().isEmpty() ? GenrePickerActivity.class : MainActivity.class;
        startActivity(new Intent(this, next).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
