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
import com.example.dineroapp.adapter.EgresoAdapter;
import com.example.dineroapp.model.Egreso;
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

public class EgresoActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    FloatingActionButton addBtn;
    FirebaseFirestore db;
    FirebaseAuth auth;
    ArrayList<Egreso> egresos;
    EgresoAdapter adapter;
    TextView txtVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_egresos);

        // Referencias a vistas
        recyclerView = findViewById(R.id.recycler_egresos);
        addBtn = findViewById(R.id.btn_add_egreso);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Configurar RecyclerView
        egresos = new ArrayList<>();
        adapter = new EgresoAdapter(this, egresos, db);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        txtVacio = findViewById(R.id.txt_vacio);

        // Cargar egresos desde Firestore
        loadEgresos();

        // Botón para agregar nuevo egreso
        addBtn.setOnClickListener(v -> showDialog(null));

        // Configurar navegación inferior
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_egresos);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_egresos) return true;
            if (itemId == R.id.nav_ingresos) startActivity(new Intent(this, IngresoActivity.class));
            else if (itemId == R.id.nav_resumen) startActivity(new Intent(this, ResumenActivity.class));
            else if (itemId == R.id.nav_perfil) startActivity(new Intent(this, PerfilActivity.class));
            return true;
        });
    }

    // Cargar egresos del usuario actual
    private void loadEgresos() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("egreso")
                .whereEqualTo("idUsuario", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    egresos.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Egreso i = doc.toObject(Egreso.class);
                        if (i != null) {
                            i.setId(doc.getId());
                            egresos.add(i);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    // Mostrar mensaje si no hay egresos
                    txtVacio.setVisibility(egresos.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // Mostrar diálogo para crear o editar un egreso
    private void showDialog(Egreso existing) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView;

        // Usar layout diferente según sea nuevo o edición
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
            // Campos para crear
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
            // Campos para edición (solo se puede cambiar monto y descripción)
            edtTitulo = null;
            edtFechaEditable = null;
            txtTituloDisplay = dialogView.findViewById(R.id.txt_titulo_display);
            txtTituloDisplay.setText(existing.titulo);
            txtFechaDisplay = dialogView.findViewById(R.id.txt_fecha_display);
            txtFechaDisplay.setText(existing.fecha);
            edtMonto.setText(String.valueOf(existing.monto));
            edtDescripcion.setText(existing.descripcion != null ? existing.descripcion : "");
        }

        // Crear diálogo
        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Nuevo Egreso" : "Editar Egreso")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String titulo = (existing == null) ? edtTitulo.getText().toString().trim() : existing.titulo;
                    String montoStr = edtMonto.getText().toString().trim();
                    String descripcion = edtDescripcion.getText().toString().trim();

                    // Validar campos
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

                    if (existing == null) {
                        // Guardar nuevo egreso
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

                        db.collection("egreso")
                                .add(data)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(this, "Egreso guardado", Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        // Actualizar egreso existente
                        existing.monto = monto;
                        existing.descripcion = descripcion;

                        db.collection("egreso").document(existing.id)
                                .set(existing)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Egreso actualizado", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}

