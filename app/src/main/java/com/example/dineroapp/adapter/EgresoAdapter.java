package com.example.dineroapp.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dineroapp.R;
import com.example.dineroapp.model.Egreso;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class EgresoAdapter extends RecyclerView.Adapter<EgresoAdapter.EgresoViewHolder> {

    public interface EgresoEditListener {
        void mostrarDialogoEditar(Egreso egreso);
    }

    private EgresoEditListener editListener;

    public void setEgresoEditListener(EgresoEditListener listener) {
        this.editListener = listener;
    }

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
        holder.txtMonto.setText("S/ " + egreso.monto);
        holder.txtMonto.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        holder.txtFecha.setText(formatearFecha(egreso.fecha));
        holder.txtDescripcion.setText(
                egreso.descripcion == null || egreso.descripcion.trim().isEmpty()
                        ? "Sin descripción"
                        : egreso.descripcion
        );

        holder.btnEditar.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.mostrarDialogoEditar(egreso);
            }
        });

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
