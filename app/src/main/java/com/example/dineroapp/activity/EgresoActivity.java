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
import com.example.dineroapp.adapter.EgresoAdapter;
import com.example.dineroapp.model.Egreso;
import com.example.dineroapp.model.ServicioAlmacenamiento;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class EgresoActivity extends AppCompatActivity implements EgresoAdapter.EgresoEditListener {

    private static final int REQUEST_IMAGE_PICK = 102;

    RecyclerView recyclerView;
    FloatingActionButton addBtn;
    FirebaseFirestore db;
    FirebaseAuth auth;
    ArrayList<Egreso> egresos;
    EgresoAdapter adapter;
    TextView txtVacio;

    private Uri comprobanteUriGlobal;
    private Egreso egresoEnEdicion;
    private ImageView ivComprobanteActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_egresos);

        recyclerView = findViewById(R.id.recycler_egresos);
        addBtn = findViewById(R.id.btn_add_egreso);
        txtVacio = findViewById(R.id.txt_vacio);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        egresos = new ArrayList<>();
        adapter = new EgresoAdapter(this, egresos, db);
        adapter.setEgresoEditListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadEgresos();

        addBtn.setOnClickListener(v -> mostrarDialogo(null));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_egresos);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_egresos) return true;
            if (id == R.id.nav_ingresos) startActivity(new Intent(this, IngresoActivity.class));
            if (id == R.id.nav_resumen) startActivity(new Intent(this, ResumenActivity.class));
            if (id == R.id.nav_perfil) startActivity(new Intent(this, PerfilActivity.class));
            return true;
        });
    }

    private void loadEgresos() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("egreso")
                .whereEqualTo("idUsuario", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    egresos.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Egreso egr = doc.toObject(Egreso.class);
                        if (egr != null) {
                            egr.setId(doc.getId());
                            egresos.add(egr);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    txtVacio.setVisibility(egresos.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void mostrarDialogoEditar(Egreso egreso) {
        mostrarDialogo(egreso);
    }

    private void mostrarDialogo(Egreso egreso) {
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
        egresoEnEdicion = egreso;
        ivComprobanteActual = ivComprobante;

        if (egreso != null) {
            // Modo solo lectura para el título
            txtTitulo.setText(egreso.titulo);
            edtTitulo.setVisibility(View.GONE);
            txtTitulo.setVisibility(View.VISIBLE);

            edtMonto.setText(String.valueOf(egreso.monto));
            edtDescripcion.setText(egreso.descripcion != null ? egreso.descripcion : "");
            if (txtFecha != null) txtFecha.setText(egreso.fecha);
            if (egreso.urlComprobante != null)
                Glide.with(this).load(egreso.urlComprobante).into(ivComprobante);
        } else {
            // Modo edición para el título
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
                .setTitle(egreso == null ? "Nuevo Egreso" : "Editar Egreso")
                .setView(dialogView)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnGuardar.setOnClickListener(view -> {
                // Si estamos creando, tomamos el texto del EditText.
                String titulo = (egreso == null)
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

                Egreso egresoFinal = (egreso == null) ? new Egreso() : egreso;
                if (egreso == null) {
                    egresoFinal.idUsuario = auth.getCurrentUser().getUid();
                }

                egresoFinal.titulo = titulo;
                egresoFinal.monto = monto;
                egresoFinal.descripcion = descripcion;
                egresoFinal.fecha = fecha;

                if (comprobanteUriGlobal != null) {
                    ServicioAlmacenamiento servicio = new ServicioAlmacenamiento(this);
                    String nombreArchivo = System.currentTimeMillis() + ".jpg";
                    servicio.guardarArchivo(nombreArchivo, comprobanteUriGlobal, new ServicioAlmacenamiento.UploadCallback() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            egresoFinal.urlComprobante = imageUrl;
                            runOnUiThread(() -> {
                                if (egreso == null) {
                                    crearEgreso(egresoFinal);
                                } else {
                                    actualizarEgreso(egresoFinal);
                                }
                                if (!isFinishing() && dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(EgresoActivity.this, "Error al subir archivo", Toast.LENGTH_SHORT).show());
                        }
                    });
                } else {
                    if (egreso == null) {
                        crearEgreso(egresoFinal);
                    } else {
                        actualizarEgreso(egresoFinal);
                    }
                    if (!isFinishing() && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }
            });
        });

        dialog.show();
    }


    private void crearEgreso(Egreso egreso) {
        db.collection("egreso")
                .add(egreso)
                .addOnSuccessListener(docRef ->
                        Toast.makeText(this, "Egreso creado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al crear egreso", Toast.LENGTH_SHORT).show());
    }

    private void actualizarEgreso(Egreso egreso) {
        db.collection("egreso").document(egreso.id)
                .set(egreso)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Egreso actualizado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al actualizar egreso", Toast.LENGTH_SHORT).show());
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
