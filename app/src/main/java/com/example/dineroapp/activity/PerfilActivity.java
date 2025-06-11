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

    // Vistas
    private TextView tvCorreo;
    private Button btnCerrarSesion;

    // Firebase y Google
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil); // Cargar el layout XML

        // Inicializar vistas
        tvCorreo = findViewById(R.id.tvCorreo);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Obtener usuario actual de Firebase
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvCorreo.setText(user.getEmail()); // Mostrar correo del usuario autenticado
        } else {
            tvCorreo.setText("Usuario no autenticado");
        }

        // Acción del botón de cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        // Configurar GoogleSignInClient (necesario para cerrar sesión si se usó Google)
        googleSignInClient = GoogleSignIn.getClient(this,
                new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .build()
        );

        // Configurar la barra de navegación inferior
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_perfil); // Marcar el ítem actual

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Evita volver a cargar la misma actividad
            if (itemId == R.id.nav_perfil) return true;

            // Navegar a las otras actividades según el ítem seleccionado
            if (itemId == R.id.nav_ingresos) {
                startActivity(new Intent(this, IngresoActivity.class));
            } else if (itemId == R.id.nav_resumen) {
                startActivity(new Intent(this, ResumenActivity.class));
            } else if (itemId == R.id.nav_egresos) {
                startActivity(new Intent(this, EgresoActivity.class));
            }

            return true;
        });
    }

    // Metodo para cerrar sesión de Firebase, Google y Facebook
    private void cerrarSesion() {
        // Cerrar sesión en Firebase
        mAuth.signOut();

        // Cerrar sesión en Google
        googleSignInClient.signOut();

        // Cerrar sesión en Facebook
        LoginManager.getInstance().logOut();

        // Redirigir al login
        startActivity(new Intent(this, LoginActivity.class));
        finish(); // Finaliza esta actividad para que no se pueda volver atrás
    }
}
