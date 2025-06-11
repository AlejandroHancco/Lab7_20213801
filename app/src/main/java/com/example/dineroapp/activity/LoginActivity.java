package com.example.dineroapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dineroapp.R;
import com.facebook.*;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    // Declaración de vistas
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView linkToRegister;
    private SignInButton btnGoogle;
    private com.facebook.login.widget.LoginButton btnFacebook;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private CallbackManager callbackManager;

    private static final int RC_GOOGLE_SIGN_IN = 1001; // Código para Google Sign-In

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Carga el layout

        // Referencias a las vistas del layout
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.btn_login);
        linkToRegister = findViewById(R.id.link_register);
        btnGoogle = findViewById(R.id.btn_google);
        btnFacebook = findViewById(R.id.btn_facebook);

        mAuth = FirebaseAuth.getInstance(); // Inicializa FirebaseAuth

        // Inicio de sesión con correo y contraseña
        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString();
            String password = inputPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            goToMain(); // Ir a pantalla principal
                        } else {
                            Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Redirige al registro si el usuario no tiene cuenta
        linkToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        // Botón de Google: inicia el flujo de inicio de sesión
        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        // Configuración de Facebook Login
        callbackManager = CallbackManager.Factory.create(); // Necesario para manejar el resultado
        btnFacebook.setPermissions(Arrays.asList("email", "public_profile")); // Solicita permisos
        btnFacebook.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // Si Facebook devuelve un token, lo pasamos a Firebase
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Inicio de Facebook cancelado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(LoginActivity.this, "Error de Facebook: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Redirige al usuario a la actividad principal
    private void goToMain() {
        startActivity(new Intent(this, IngresoActivity.class));
        finish();
    }

    // Lanza el intent para iniciar sesión con Google
    private void signInWithGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // ID del cliente OAuth 2.0
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        startActivityForResult(googleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN);
    }

    // Maneja el resultado de Google y Facebook
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Facebook SDK necesita este callback
        callbackManager.onActivityResult(requestCode, resultCode, data);

        // Resultado del intent de Google
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                // Autenticación con Firebase usando el token de Google
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e("GoogleSignIn", "Error: " + e.getStatusCode(), e);
                Toast.makeText(this, "Error: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Autenticación con Firebase usando el token de Google
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        goToMain(); // Éxito
                    } else {
                        Toast.makeText(this, "Autenticación fallida", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Autenticación con Firebase usando el token de Facebook
    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        goToMain(); // Éxito
                    } else {
                        Toast.makeText(this, "Error de autenticación con Facebook", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
