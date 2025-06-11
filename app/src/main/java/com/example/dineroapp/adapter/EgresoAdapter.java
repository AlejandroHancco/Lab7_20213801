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

    // Constructor para inicializar contexto, lista de egresos y base de datos
    public EgresoAdapter(Context context, ArrayList<Egreso> egresos, FirebaseFirestore db) {
        this.context = context;
        this.egresos = egresos;
        this.db = db;
    }

    // Infla el layout del ítem de egreso y crea el ViewHolder
    @NonNull
    @Override
    public EgresoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_egreso, parent, false);
        return new EgresoViewHolder(v);
    }

    // Asocia los datos de un egreso a su vista correspondiente
    @Override
    public void onBindViewHolder(@NonNull EgresoViewHolder holder, int position) {
        Egreso egreso = egresos.get(position);

        // Asigna valores a los TextViews del ítem
        holder.txtTitulo.setText(egreso.titulo);
        holder.txtMonto.setText("-S/ " + egreso.monto);
        holder.txtMonto.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        holder.txtFecha.setText(formatearFecha(egreso.fecha));
        holder.txtDescripcion.setText(
                egreso.descripcion == null || egreso.descripcion.trim().isEmpty()
                        ? "Sin descripción"
                        : egreso.descripcion
        );

        // Listener para botón de editar
        holder.btnEditar.setOnClickListener(v -> showEditDialog(egreso));

        // Listener para botón de eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar")
                    .setMessage("¿Deseas eliminar este egreso?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        db.collection("egreso").document(egreso.id)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    // Si se elimina correctamente, actualiza la lista
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
        return egresos.size(); // Devuelve cuántos egresos hay en la lista
    }

    // ViewHolder que contiene referencias a los elementos del layout
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

    // Muestra un diálogo para editar un egreso existente
    private void showEditDialog(Egreso egreso) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_ingreso_editar, null); // Layout personalizado

        // Referencias a los campos del diálogo
        TextView txtTituloDisplay = dialogView.findViewById(R.id.txt_titulo_display);
        TextView txtFechaDisplay = dialogView.findViewById(R.id.txt_fecha_display);
        EditText edtMonto = dialogView.findViewById(R.id.edt_monto);
        EditText edtDescripcion = dialogView.findViewById(R.id.edt_descripcion);

        // Prellenar los campos con datos actuales
        txtTituloDisplay.setText(egreso.titulo);
        txtFechaDisplay.setText(formatearFecha(egreso.fecha));
        edtMonto.setText(String.valueOf(egreso.monto));
        edtDescripcion.setText(egreso.descripcion != null ? egreso.descripcion : "");

        // Construcción del AlertDialog para edición
        new AlertDialog.Builder(context)
                .setTitle("Editar Egreso")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String montoStr = edtMonto.getText().toString().trim();
                    String descripcion = edtDescripcion.getText().toString().trim();

                    // Validación del monto ingresado
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

                    // Actualizar objeto y enviar a Firestore
                    egreso.monto = monto;
                    egreso.descripcion = descripcion;

                    db.collection("egreso").document(egreso.id)
                            .set(egreso)
                            .addOnSuccessListener(unused -> {
                                notifyDataSetChanged(); // Refrescar lista
                                Toast.makeText(context, "Egreso actualizado", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Formatea fecha de formato corto (ej: 1/6/2025) a largo (ej: 1 de junio de 2025)
    private String formatearFecha(String fechaOriginal) {
        try {
            SimpleDateFormat formatoEntrada = new SimpleDateFormat("d/M/yyyy");
            Date fecha = formatoEntrada.parse(fechaOriginal);
            SimpleDateFormat formatoSalida = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
            return formatoSalida.format(fecha);
        } catch (Exception e) {
            return fechaOriginal; // Si falla el parseo, devuelve la original
        }
    }
}
