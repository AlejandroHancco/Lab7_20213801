package com.example.dineroapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dineroapp.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.kal.rackmonthpicker.RackMonthPicker;
import com.kal.rackmonthpicker.listener.DateMonthDialogListener;
import com.kal.rackmonthpicker.listener.OnCancelMonthDialogListener;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ResumenActivity extends AppCompatActivity {

    private TextView txtSelectedMonth, txtResumenPie, txtResumenBar;
    private PieChart pieChart;
    private BarChart barChart;
    private Button btnSelectMonth;

    private Calendar selectedMonth;
    private FirebaseFirestore db;

    private float ingresosTotal = 0;
    private float egresosTotal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resumen);

        txtSelectedMonth = findViewById(R.id.txt_selected_month);
        txtResumenPie = findViewById(R.id.txt_resumen_pie);
        txtResumenBar = findViewById(R.id.txt_resumen_bar);
        pieChart = findViewById(R.id.pie_chart);
        barChart = findViewById(R.id.bar_chart);
        btnSelectMonth = findViewById(R.id.btn_select_month);

        selectedMonth = Calendar.getInstance();
        db = FirebaseFirestore.getInstance();

        updateMonthText();
        loadChartData();

        btnSelectMonth.setOnClickListener(v -> showMonthYearPicker());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_resumen);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_resumen) return true;
            if (itemId == R.id.nav_egresos) startActivity(new Intent(this, EgresoActivity.class));
            else if (itemId == R.id.nav_ingresos) startActivity(new Intent(this, IngresoActivity.class));
            else if (itemId == R.id.nav_perfil) startActivity(new Intent(this, PerfilActivity.class));

            return true;
        });
    }

    private void updateMonthText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        txtSelectedMonth.setText("Mes: " + sdf.format(selectedMonth.getTime()));
    }

    //Función en GitHub de un buen samaritano
    private void showMonthYearPicker() {
        new RackMonthPicker(this)
                .setColorTheme(R.color.primary)
                .setLocale(new Locale("es", "ES"))
                .setPositiveText("Aceptar") // Cambia texto del botón "OK"
                .setNegativeText("Cancelar") // Cambia texto del botón "Cancelar"
                .setPositiveButton(new DateMonthDialogListener() {
                    @Override
                    public void onDateMonth(int month, int startDate, int endDate, int year, String monthLabel) {
                        selectedMonth.set(Calendar.YEAR, year);
                        selectedMonth.set(Calendar.MONTH, month - 1); // Calendar es 0-based
                        selectedMonth.set(Calendar.DAY_OF_MONTH, 1);

                        updateMonthText();
                        loadChartData();
                    }
                })
                .setNegativeButton(new OnCancelMonthDialogListener() {
                    @Override
                    public void onCancel(AlertDialog dialog) {
                        dialog.dismiss();
                    }
                })
                .show();
    }




    private void loadChartData() {
        ingresosTotal = 0;
        egresosTotal = 0;

        SimpleDateFormat sdfInput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String mesSeleccionado = new SimpleDateFormat("MM/yyyy", Locale.getDefault())
                .format(selectedMonth.getTime());

        db.collection("ingreso").get().addOnSuccessListener(snapshot -> {
            for (QueryDocumentSnapshot doc : snapshot) {
                String fecha = doc.getString("fecha");
                Double monto = doc.getDouble("monto");

                if (fecha != null && monto != null) {
                    try {
                        Calendar fechaCal = Calendar.getInstance();
                        fechaCal.setTime(sdfInput.parse(fecha));
                        String mesDoc = new SimpleDateFormat("MM/yyyy", Locale.getDefault())
                                .format(fechaCal.getTime());

                        if (mesDoc.equals(mesSeleccionado)) {
                            ingresosTotal += monto;
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }

            db.collection("egreso").get().addOnSuccessListener(snapshot2 -> {
                for (QueryDocumentSnapshot doc : snapshot2) {
                    String fecha = doc.getString("fecha");
                    Double monto = doc.getDouble("monto");

                    if (fecha != null && monto != null) {
                        try {
                            Calendar fechaCal = Calendar.getInstance();
                            fechaCal.setTime(sdfInput.parse(fecha));
                            String mesDoc = new SimpleDateFormat("MM/yyyy", Locale.getDefault())
                                    .format(fechaCal.getTime());

                            if (mesDoc.equals(mesSeleccionado)) {
                                egresosTotal += monto;
                            }

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }

                renderPieChart(ingresosTotal, egresosTotal);
                renderBarChart(ingresosTotal, egresosTotal);
            });
        });
    }

    private void renderPieChart(float ingresos, float egresos) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        PieDataSet dataSet;
        PieData data;

        if (ingresos > 0) {
            float porcentajeEgreso = (egresos / ingresos) * 100;

            if (porcentajeEgreso <= 100) {
                entries.add(new PieEntry(porcentajeEgreso, "Egresos"));
                entries.add(new PieEntry(100 - porcentajeEgreso, "Ingresos"));
                dataSet = new PieDataSet(entries, "");
                dataSet.setColors(new int[]{android.graphics.Color.RED, android.graphics.Color.GREEN});
            } else {
                entries.add(new PieEntry(100, "Egresos excedidos"));
                dataSet = new PieDataSet(entries, "");
                dataSet.setColors(new int[]{android.graphics.Color.RED});
            }

            txtResumenPie.setText(String.format(Locale.getDefault(),
                    "Egresos = %.1f%% de los Ingresos", porcentajeEgreso));

        } else {
            entries.add(new PieEntry(1, "Sin ingresos"));
            dataSet = new PieDataSet(entries, "");
            dataSet.setColors(new int[]{android.graphics.Color.GRAY});
            txtResumenPie.setText("Sin ingresos registrados este mes.");
        }

        data = new PieData(dataSet);
        pieChart.setData(data);

        Description desc = new Description();
        desc.setText(String.format(Locale.getDefault(),
                "Ingresos: S/%.2f | Egresos: S/%.2f", ingresos, egresos));
        pieChart.setDescription(desc);
        pieChart.invalidate();
    }

    private void renderBarChart(float ingresos, float egresos) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, ingresos));
        entries.add(new BarEntry(1, egresos));
        entries.add(new BarEntry(2, ingresos - egresos)); // Neto

        BarDataSet dataSet = new BarDataSet(entries, "Movimientos");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        barChart.setData(data);
        barChart.setFitBars(true);

        Description desc = new Description();
        desc.setText("Ingresos, Egresos y Consolidado");
        barChart.setDescription(desc);
        barChart.invalidate();

        txtResumenBar.setText(String.format(Locale.getDefault(),
                "Ingresos: S/%.2f | Egresos: S/%.2f | Neto: S/%.2f",
                ingresos, egresos, ingresos - egresos));
    }
}
