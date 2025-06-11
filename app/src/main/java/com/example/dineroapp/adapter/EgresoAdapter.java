package com.example.dineroapp.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dineroapp.R;
import com.example.dineroapp.model.Egreso;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class EgresoAdapter extends RecyclerView.Adapter<EgresoAdapter.EgresoViewHolder> {

    Context context;
    ArrayList<Egreso> egresos;
    FirebaseFirestore db;

    public EgresoAdapter(Context context, ArrayList<Egreso> egresos, FirebaseFirestore db) {
        this.context = context;
        this.egresos = egresos;
        this.db = db;
    }

    @NonNull
    @Override
    public EgresoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_egreso, parent, false);
        return new EgresoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EgresoViewHolder holder, int position) {
        Egreso egreso = egresos.get(position);

        holder.txtTitulo.setText(egreso.titulo);
        holder.txtMonto.setText("-S/ " + egreso.monto);
        holder.txtMonto.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        holder.txtFecha.setText(formatearFecha(egreso.fecha));
        holder.txtDescripcion.setText(
                egreso.descripcion == null || egreso.descripcion.trim().isEmpty()
                        ? "Sin descripción"
                        : egreso.descripcion
        );

        holder.btnEditar.setOnClickListener(v -> showEditDialog(egreso));
        holder.btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar")
                    .setMessage("¿Deseas eliminar este egreso?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        db.collection("egreso").document(egreso.id)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    int currentPosition = holder.getAdapterPosition();
                                    if (currentPosition != RecyclerView.NO_POSITION && currentPosition < egresos.size()) {
                                        egresos.remove(currentPosition);
                                        notifyItemRemoved(currentPosition);
                                        Toast.makeText(context, "Egreso eliminado", Toast.LENGTH_SHORT).show();
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
        return egresos.size();
    }

    public static class EgresoViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitulo, txtMonto, txtFecha, txtDescripcion;
        ImageButton btnEditar, btnEliminar;

        public EgresoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitulo = itemView.findViewById(R.id.txt_titulo);
            txtMonto = itemView.findViewById(R.id.txt_monto);
            txtFecha = itemView.findViewById(R.id.txt_fecha);
            txtDescripcion = itemView.findViewById(R.id.txt_descripcion);
            btnEditar = itemView.findViewById(R.id.btn_editar);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar);
        }
    }

    private void showEditDialog(Egreso egreso) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_ingreso_editar, null); // Asegúrate de tener este layout

        TextView txtTituloDisplay = dialogView.findViewById(R.id.txt_titulo_display);
        TextView txtFechaDisplay = dialogView.findViewById(R.id.txt_fecha_display);
        EditText edtMonto = dialogView.findViewById(R.id.edt_monto);
        EditText edtDescripcion = dialogView.findViewById(R.id.edt_descripcion);

        txtTituloDisplay.setText(egreso.titulo);
        txtFechaDisplay.setText(formatearFecha(egreso.fecha));
        edtMonto.setText(String.valueOf(egreso.monto));
        edtDescripcion.setText(egreso.descripcion != null ? egreso.descripcion : "");

        new AlertDialog.Builder(context)
                .setTitle("Editar Egreso")
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

                    egreso.monto = monto;
                    egreso.descripcion = descripcion;

                    db.collection("egreso").document(egreso.id)
                            .set(egreso)
                            .addOnSuccessListener(unused -> {
                                notifyDataSetChanged();
                                Toast.makeText(context, "Egreso actualizado", Toast.LENGTH_SHORT).show();
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
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("d/M/yyyy");
            Date fecha = formatoEntrada.parse(fechaOriginal);
            SimpleDateFormat formatoSalida = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
            return formatoSalida.format(fecha);
        } catch (Exception e) {
            return fechaOriginal;
        }
    }
}
