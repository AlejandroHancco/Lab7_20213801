package com.example.dineroapp.model;

public class Ingreso {
    public String id;
    public String idUsuario;
    public String titulo;
    public double monto;
    public String descripcion;
    public String fecha;

    public Ingreso() {}
    public Ingreso(String titulo, double monto, String fecha, String descripcion, String idUsuario) {
        this.titulo = titulo;
        this.monto = monto;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.idUsuario = idUsuario;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}

