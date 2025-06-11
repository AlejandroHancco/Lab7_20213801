package com.example.dineroapp.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dineroapp.R;
import com.example.dineroapp.model.Ingreso;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class IngresoAdapter extends RecyclerView.Adapter<IngresoAdapter.IngresoViewHolder> {

    Context context;
    ArrayList<Ingreso> ingresos;
    FirebaseFirestore db;

    public IngresoAdapter(Context context, ArrayList<Ingreso> ingresos, FirebaseFirestore db) {
        this.context = context;
        this.ingresos = ingresos;
        this.db = db;
    }

    @NonNull
    @Override
    public IngresoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_ingreso, parent, false);
        return new IngresoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull IngresoViewHolder holder, int position) {
        Ingreso ingreso = ingresos.get(position);

        holder.txtTitulo.setText(ingreso.titulo);
        holder.txtMonto.setText("S/ " + ingreso.monto);
        holder.txtMonto.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        holder.txtFecha.setText(formatearFecha(ingreso.fecha));
        holder.txtDescripcion.setText(
                ingreso.descripcion == null || ingreso.descripcion.trim().isEmpty()
                        ? "Sin descripción"
                        : ingreso.descripcion
        );

        holder.btnEditar.setOnClickListener(v -> showEditDialog(ingreso));
        holder.btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar")
                    .setMessage("¿Deseas eliminar este ingreso?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        db.collection("ingreso").document(ingreso.id)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    int currentPosition = holder.getAdapterPosition();
                                    if (currentPosition != RecyclerView.NO_POSITION && currentPosition < ingresos.size()) {
                                        ingresos.remove(currentPosition);
                                        notifyItemRemoved(currentPosition);
                                        Toast.makeText(context, "Ingreso eliminado", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

    }

    @Override
    public int getItemCount() {
        return ingresos.size();
    }

    // ✅ Add this inner class to fix the "Cannot resolve symbol 'IngresoViewHolder'" error
    public static class IngresoViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitulo, txtMonto, txtFecha, txtDescripcion;
        ImageButton btnEditar, btnEliminar;

        public IngresoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitulo = itemView.findViewById(R.id.txt_titulo);
            txtMonto = itemView.findViewById(R.id.txt_monto);
            txtFecha = itemView.findViewById(R.id.txt_fecha);
            txtDescripcion = itemView.findViewById(R.id.txt_descripcion);
            btnEditar = itemView.findViewById(R.id.btn_editar);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar);
        }
    }

    // Optional: Implement this if you want editing functionality
    private void showEditDialog(Ingreso ingreso) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_ingreso_editar, null);

        // Obtener referencias a vistas
        TextView txtTituloDisplay = dialogView.findViewById(R.id.txt_titulo_display);
        TextView txtFechaDisplay = dialogView.findViewById(R.id.txt_fecha_display);
        EditText edtMonto = dialogView.findViewById(R.id.edt_monto);
        EditText edtDescripcion = dialogView.findViewById(R.id.edt_descripcion);

        // Setear valores actuales
        txtTituloDisplay.setText(ingreso.titulo);
        txtFechaDisplay.setText(formatearFecha(ingreso.fecha));
        edtMonto.setText(String.valueOf(ingreso.monto));
        edtDescripcion.setText(ingreso.descripcion != null ? ingreso.descripcion : "");

        new AlertDialog.Builder(context)
                .setTitle("Editar Ingreso")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String montoStr = edtMonto.getText().toString().trim();
                    String descripcion = edtDescripcion.getText().toString().trim();

                    if (montoStr.isEmpty()) {
                        Toast.makeText(context, "Por favor ingresa un monto", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double monto;
                    try {
                        monto = Double.parseDouble(montoStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Monto inválido", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Actualizar en Firestore
                    ingreso.monto = monto;
                    ingreso.descripcion = descripcion;

                    db.collection("ingreso").document(ingreso.id)
                            .set(ingreso)
                            .addOnSuccessListener(unused -> {
                                notifyDataSetChanged();
                                Toast.makeText(context, "Ingreso actualizado", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String formatearFecha(String fechaOriginal) {
        try {
            // Intenta analizar la fecha original, por ejemplo "2/2/2025"
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("d/M/yyyy");
            Date fecha = formatoEntrada.parse(fechaOriginal);

            // Establece el formato de salida en español
            SimpleDateFormat formatoSalida = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));

            return formatoSalida.format(fecha);
        } catch (Exception e) {
            return fechaOriginal; // Si hay error, muestra la original
        }
    }


}
