package com.example.dineroapp.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dineroapp.R;
import com.example.dineroapp.adapter.IngresoAdapter;
import com.example.dineroapp.model.Ingreso;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class IngresoActivity extends AppCompatActivity {

    // Elementos de UI y Firebase
    RecyclerView recyclerView;
    FloatingActionButton addBtn;
    FirebaseFirestore db;
    FirebaseAuth auth;
    ArrayList<Ingreso> ingresos;
    IngresoAdapter adapter;
    TextView txtVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingresos);

        // Referencias a vistas
        recyclerView = findViewById(R.id.recycler_ingresos);
        addBtn = findViewById(R.id.btn_add_ingreso);
        txtVacio = findViewById(R.id.txt_vacio);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Configurar RecyclerView
        ingresos = new ArrayList<>();
        adapter = new IngresoAdapter(this, ingresos, db);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Cargar ingresos del usuario
        loadIngresos();

        // Abrir formulario de nuevo ingreso
        addBtn.setOnClickListener(v -> showDialog(null));

        // Navegación inferior
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_ingresos);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_ingresos) return true;
            if (itemId == R.id.nav_egresos) startActivity(new Intent(this, EgresoActivity.class));
            else if (itemId == R.id.nav_resumen) startActivity(new Intent(this, ResumenActivity.class));
            else if (itemId == R.id.nav_perfil) startActivity(new Intent(this, PerfilActivity.class));

            return true;
        });
    }

    // Cargar ingresos desde Firebase
    private void loadIngresos() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("ingreso")
                .whereEqualTo("idUsuario", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    ingresos.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Ingreso i = doc.toObject(Ingreso.class);
                        if (i != null) {
                            i.setId(doc.getId());
                            ingresos.add(i);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    // Mostrar mensaje si no hay ingresos
                    txtVacio.setVisibility(ingresos.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // Mostrar formulario de nuevo ingreso o edición
    private void showDialog(Ingreso existing) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView;

        // Elegir layout según si es nuevo o editar
        if (existing == null) {
            dialogView = inflater.inflate(R.layout.dialog_ingreso_crear, null);
        } else {
            dialogView = inflater.inflate(R.layout.dialog_ingreso_editar, null);
        }

        // Campos comunes
        EditText edtMonto = dialogView.findViewById(R.id.edt_monto);
        EditText edtDescripcion = dialogView.findViewById(R.id.edt_descripcion);

        EditText edtTitulo;
        EditText edtFechaEditable;
        TextView txtTituloDisplay = null;
        TextView txtFechaDisplay = null;
        final String[] fecha = new String[1];

        if (existing == null) {
            edtTitulo = dialogView.findViewById(R.id.edt_titulo);
            edtFechaEditable = dialogView.findViewById(R.id.edt_fecha);

            // Selección de fecha
            edtFechaEditable.setOnClickListener(v -> {
                Calendar calendar = Calendar.getInstance();
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {
                            String selectedFecha = dayOfMonth + "/" + (month + 1) + "/" + year;
                            edtFechaEditable.setText(selectedFecha);
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                datePickerDialog.show();
            });

        } else {
            // Mostrar datos existentes (no se editan título ni fecha)
            edtTitulo = null;
            edtFechaEditable = null;
            txtTituloDisplay = dialogView.findViewById(R.id.txt_titulo_display);
            txtTituloDisplay.setText(existing.titulo);

            txtFechaDisplay = dialogView.findViewById(R.id.txt_fecha_display);
            txtFechaDisplay.setText(existing.fecha);

            edtMonto.setText(String.valueOf(existing.monto));
            edtDescripcion.setText(existing.descripcion != null ? existing.descripcion : "");
        }

        // Crear el diálogo
        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Nuevo Ingreso" : "Editar Ingreso")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    // Obtener datos del formulario
                    String titulo = (existing == null) ? edtTitulo.getText().toString().trim() : existing.titulo;
                    String montoStr = edtMonto.getText().toString().trim();
                    String descripcion = edtDescripcion.getText().toString().trim();

                    if (titulo.isEmpty() || montoStr.isEmpty()) {
                        Toast.makeText(this, "Por favor, completa los campos obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double monto;
                    try {
                        monto = Double.parseDouble(montoStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Guardar en Firebase
                    if (existing == null) {
                        fecha[0] = edtFechaEditable.getText().toString().trim();
                        if (fecha[0].isEmpty()) {
                            Toast.makeText(this, "Por favor, selecciona una fecha", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = auth.getCurrentUser().getUid();
                        Map<String, Object> data = new HashMap<>();
                        data.put("titulo", titulo);
                        data.put("monto", monto);
                        data.put("fecha", fecha[0]);
                        data.put("descripcion", descripcion);
                        data.put("idUsuario", uid);

                        db.collection("ingreso")
                                .add(data)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(this, "Ingreso guardado", Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        // Actualizar monto y descripción
                        existing.monto = monto;
                        existing.descripcion = descripcion;

                        db.collection("ingreso").document(existing.id)
                                .set(existing)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Ingreso actualizado", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
