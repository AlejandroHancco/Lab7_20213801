package com.example.dineroapp.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dineroapp.R;
import com.example.dineroapp.adapter.IngresoAdapter;
import com.example.dineroapp.model.Ingreso;
import com.example.dineroapp.model.ServicioAlmacenamiento;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class IngresoActivity extends AppCompatActivity implements IngresoAdapter.IngresoEditListener {

    private static final int REQUEST_IMAGE_PICK = 101;

    RecyclerView recyclerView;
    FloatingActionButton addBtn;
    FirebaseFirestore db;
    FirebaseAuth auth;
    ArrayList<Ingreso> ingresos;
    IngresoAdapter adapter;
    TextView txtVacio;

    private Uri comprobanteUriGlobal;
    private Ingreso ingresoEnEdicion;
    private ImageView ivComprobanteActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingresos);

        recyclerView = findViewById(R.id.recycler_ingresos);
        addBtn = findViewById(R.id.btn_add_ingreso);
        txtVacio = findViewById(R.id.txt_vacio);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        ingresos = new ArrayList<>();
        adapter = new IngresoAdapter(this, ingresos, db);
        adapter.setIngresoEditListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadIngresos();

        addBtn.setOnClickListener(v -> mostrarDialogo(null));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_ingresos);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_ingresos) return true;
            if (id == R.id.nav_egresos) {
                startActivity(new Intent(this, EgresoActivity.class));
                return true;
            }
            if (id == R.id.nav_resumen) {
                startActivity(new Intent(this, ResumenActivity.class));
                return true;
            }
            if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                return true;
            }
            return false;
        });
    }

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
                    txtVacio.setVisibility(ingresos.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void mostrarDialogoEditar(Ingreso ingreso) {
        mostrarDialogo(ingreso);
    }

    private void mostrarDialogo(Ingreso ingreso) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_ingreso_editar, null);

        EditText edtTitulo = dialogView.findViewById(R.id.edt_titulo);
        TextView txtTitulo = dialogView.findViewById(R.id.txt_titulo_display);
        EditText edtMonto = dialogView.findViewById(R.id.edt_monto);
        EditText edtDescripcion = dialogView.findViewById(R.id.edt_descripcion);
        TextView txtFecha = dialogView.findViewById(R.id.txt_fecha_display);
        ImageView ivComprobante = dialogView.findViewById(R.id.iv_comprobante);
        Button btnSeleccionarFoto = dialogView.findViewById(R.id.btn_seleccionar_foto);

        comprobanteUriGlobal = null;
        ingresoEnEdicion = ingreso;
        ivComprobanteActual = ivComprobante;

        if (ingreso != null) {
            // Modo edición: mostrar el título en TextView (no editable)
            edtTitulo.setVisibility(View.GONE);
            txtTitulo.setVisibility(View.VISIBLE);
            txtTitulo.setText(ingreso.titulo);

            edtMonto.setText(String.valueOf(ingreso.monto));
            edtDescripcion.setText(ingreso.descripcion != null ? ingreso.descripcion : "");
            if (txtFecha != null) txtFecha.setText(ingreso.fecha);
            if (ingreso.urlComprobante != null)
                Glide.with(this).load(ingreso.urlComprobante).into(ivComprobante);
        } else {
            // Modo creación: mostrar EditText para escribir el título
            edtTitulo.setVisibility(View.VISIBLE);
            txtTitulo.setVisibility(View.GONE);
        }

        if (txtFecha != null) {
            txtFecha.setOnClickListener(v -> mostrarDatePicker(txtFecha));
        }

        btnSeleccionarFoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(ingreso == null ? "Nuevo Ingreso" : "Editar Ingreso")
                .setView(dialogView)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnGuardar.setOnClickListener(view -> {
                String titulo = ingreso == null
                        ? edtTitulo.getText().toString().trim()
                        : txtTitulo.getText().toString().trim();
                String montoStr = edtMonto.getText().toString().trim();
                String descripcion = edtDescripcion.getText().toString().trim();
                String fecha = (txtFecha != null) ? txtFecha.getText().toString().trim() : "";

                if (titulo.isEmpty()) {
                    Toast.makeText(this, "Ingresa un título", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (montoStr.isEmpty()) {
                    Toast.makeText(this, "Ingresa el monto", Toast.LENGTH_SHORT).show();
                    return;
                }

                double monto;
                try {
                    monto = Double.parseDouble(montoStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
                    return;
                }

                Ingreso ingresoFinal = (ingreso == null) ? new Ingreso() : ingreso;
                if (ingreso == null) {
                    ingresoFinal.idUsuario = auth.getCurrentUser().getUid();
                }

                ingresoFinal.titulo = titulo;
                ingresoFinal.monto = monto;
                ingresoFinal.descripcion = descripcion;
                ingresoFinal.fecha = fecha;

                if (comprobanteUriGlobal != null) {
                    ServicioAlmacenamiento servicio = new ServicioAlmacenamiento(this);
                    String nombreArchivo = System.currentTimeMillis() + ".jpg";
                    servicio.guardarArchivo(nombreArchivo, comprobanteUriGlobal, new ServicioAlmacenamiento.UploadCallback() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            ingresoFinal.urlComprobante = imageUrl;
                            runOnUiThread(() -> {
                                if (ingreso == null) {
                                    crearIngreso(ingresoFinal);
                                } else {
                                    actualizarIngreso(ingresoFinal);
                                }
                                if (!isFinishing() && dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(IngresoActivity.this, "Error al subir archivo", Toast.LENGTH_SHORT).show());
                        }
                    });
                } else {
                    if (ingreso == null) {
                        crearIngreso(ingresoFinal);
                    } else {
                        actualizarIngreso(ingresoFinal);
                    }
                    if (!isFinishing() && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }
            });
        });

        dialog.show();
    }



    private void crearIngreso(Ingreso ingreso) {
        db.collection("ingreso")
                .add(ingreso)
                .addOnSuccessListener(docRef ->
                        Toast.makeText(this, "Ingreso creado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al crear ingreso", Toast.LENGTH_SHORT).show());
    }

    private void actualizarIngreso(Ingreso ingreso) {
        db.collection("ingreso").document(ingreso.id)
                .set(ingreso)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Ingreso actualizado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar ingreso", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            comprobanteUriGlobal = data.getData();
            if (ivComprobanteActual != null && comprobanteUriGlobal != null) {
                ivComprobanteActual.setImageURI(comprobanteUriGlobal);
            }
        }
    }

    private void mostrarDatePicker(TextView txtFecha) {
        final Calendar calendario = Calendar.getInstance();
        int anio = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);
        int dia = calendario.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    txtFecha.setText(fechaSeleccionada);
                }, anio, mes, dia);
        datePickerDialog.show();
    }
}
