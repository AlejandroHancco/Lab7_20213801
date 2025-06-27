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

        addBtn.setOnClickListener(v -> showDialog(null));

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
                    txtVacio.setVisibility(egresos.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void mostrarDialogoEditar(Egreso egreso) {
        showDialog(egreso);
    }

    private void showDialog(Egreso existing) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(
                existing == null ? R.layout.dialog_ingreso_crear : R.layout.dialog_ingreso_editar,
                null
        );

        EditText edtMonto = dialogView.findViewById(R.id.edt_monto);
        EditText edtDescripcion = dialogView.findViewById(R.id.edt_descripcion);
        ImageView ivComprobante = dialogView.findViewById(R.id.iv_comprobante);
        Button btnSeleccionarFoto = dialogView.findViewById(R.id.btn_seleccionar_foto);
        TextView txtTitulo = dialogView.findViewById(R.id.txt_titulo_display);
        TextView txtFecha = dialogView.findViewById(R.id.txt_fecha_display);

        comprobanteUriGlobal = null;
        egresoEnEdicion = existing;
        ivComprobanteActual = ivComprobante;

        if (existing != null) {
            edtMonto.setText(String.valueOf(existing.monto));
            edtDescripcion.setText(existing.descripcion != null ? existing.descripcion : "");
            txtTitulo.setText(existing.titulo);
            txtFecha.setText(existing.fecha);
            if (existing.urlComprobante != null)
                Glide.with(this).load(existing.urlComprobante).into(ivComprobante);
        }

        btnSeleccionarFoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Nuevo Egreso" : "Editar Egreso")
                .setView(dialogView)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnGuardar.setOnClickListener(view -> {
                String montoStr = edtMonto.getText().toString().trim();
                String descripcion = edtDescripcion.getText().toString().trim();

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

                existing.monto = monto;
                existing.descripcion = descripcion;

                if (comprobanteUriGlobal != null) {
                    ServicioAlmacenamiento servicio = new ServicioAlmacenamiento(this);
                    String nombreArchivo = System.currentTimeMillis() + ".jpg";
                    servicio.guardarArchivo(nombreArchivo, comprobanteUriGlobal, new ServicioAlmacenamiento.UploadCallback() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            existing.urlComprobante = imageUrl;
                            actualizarEgreso(existing);
                            dialog.dismiss();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(EgresoActivity.this, "Error al subir archivo", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    actualizarEgreso(existing);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
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
}
