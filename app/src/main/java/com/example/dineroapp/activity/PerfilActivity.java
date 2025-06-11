package com.example.dineroapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dineroapp.R;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PerfilActivity extends AppCompatActivity {

    private TextView tvCorreo;
    private Button btnCerrarSesion;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        tvCorreo = findViewById(R.id.tvCorreo);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        mAuth = FirebaseAuth.getInstance();

        // Mostrar correo del usuario autenticado
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvCorreo.setText(user.getEmail());
        } else {
            tvCorreo.setText("Usuario no autenticado");
        }

        // Cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        // Configurar GoogleSignInClient (por si se logueó con Google)
        googleSignInClient = GoogleSignIn.getClient(this,
                new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .build()
        );
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_perfil);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_perfil) return true;
            if (itemId == R.id.nav_ingresos) startActivity(new Intent(this, IngresoActivity.class));
            else if (itemId == R.id.nav_resumen) startActivity(new Intent(this, ResumenActivity.class));
            else if (itemId == R.id.nav_egresos) startActivity(new Intent(this, EgresoActivity.class));


            return true;
        });
    }

    private void cerrarSesion() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        googleSignInClient.signOut();

        // Facebook sign out
        LoginManager.getInstance().logOut();

        // Redirigir a Login
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
