package com.example.dineroapp.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dineroapp.R;
import com.example.dineroapp.model.Ingreso;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import android.view.ViewGroup;

public class IngresoAdapter extends RecyclerView.Adapter<IngresoAdapter.IngresoViewHolder> {

    public interface IngresoEditListener {
        void mostrarDialogoEditar(Ingreso ingreso);
    }

    private IngresoEditListener editListener;

    public void setIngresoEditListener(IngresoEditListener listener) {
        this.editListener = listener;
    }

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

        holder.btnEditar.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.mostrarDialogoEditar(ingreso);
            }
        });

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
