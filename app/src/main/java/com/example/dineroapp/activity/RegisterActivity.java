package com.example.dineroapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dineroapp.R;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    // Declaración de vistas y Firebase
    private EditText inputEmail, inputPassword, inputRepeatPassword;
    private Button btnRegistrar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Cargar diseño XML

        // Inicializar vistas
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        inputRepeatPassword = findViewById(R.id.input_repeat_password);
        btnRegistrar = findViewById(R.id.btn_registrar);

        // Inicializar FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Manejar clic en el botón de registrar
        btnRegistrar.setOnClickListener(v -> registrarUsuario());
    }

    // Metodo para registrar un nuevo usuario
    private void registrarUsuario() {
        // Obtener datos del formulario
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String repeatPassword = inputRepeatPassword.getText().toString().trim();

        // Validaciones básicas
        if (email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(repeatPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear cuenta en Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registro exitoso
                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        // Redirigir al login
                        startActivity(new Intent(this, LoginActivity.class));
                        finish(); // Cierra esta actividad
                    } else {
                        // Mostrar error
                        Toast.makeText(this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
