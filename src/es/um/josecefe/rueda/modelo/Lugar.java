package es.um.josecefe.rueda.modelo;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Representa un lugar
 *
 * SQL: CREATE TABLE "lugar" ( "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
 * "nombre" TEXT NOT NULL, "latitud" REAL, "longitud" REAL, "direccion" TEXT,
 * "poblacion" TEXT, "cp" TEXT );
 *
 * @author josec
 *
 */
public class Lugar implements Comparable<Lugar> {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty nombre;
    private Double latitud;
    private Double longitud;
    private String direccion;
    private String poblacion;
    private String cp;

    /**
     * Constructor por defecto para la persistencia
     */
    public Lugar() {
        this.id = new SimpleIntegerProperty();
        this.nombre = new SimpleStringProperty();
    }

    /**
     * Crea un lugar. Este construtor es el que debe usarse para crear un nuevo
     * objeto del tipo Lugar. Se debe reservar el constructor por defecto para
     * labores de persistencia.
     *
     * @param id
     * @param nombre
     * @param latitud
     * @param longitud
     * @param direccion
     * @param poblacion
     * @param cp
     */
    public Lugar(int id, String nombre, Double latitud, Double longitud, String direccion, String poblacion,
            String cp) {
        this.id = new SimpleIntegerProperty(id);
        this.nombre = new SimpleStringProperty(nombre);
        this.latitud = latitud;
        this.longitud = longitud;
        this.direccion = direccion;
        this.poblacion = poblacion;
        this.cp = cp;
    }

    /**
     *
     * @return the id
     */
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        // Solo se puede establecer el id en un objeto nuevo
        if (this.getId() == 0) {
            this.id.set(id);
        }
    }
    
    public SimpleIntegerProperty idProperty() {
        return id;
    }

    /**
     * @return the nombre
     */
    public String getNombre() {
        return nombre.get();
    }
    
    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }
    
    public SimpleStringProperty nombreProperty() {
        return nombre;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setPoblacion(String poblacion) {
        this.poblacion = poblacion;
    }

    public void setCp(String cp) {
        this.cp = cp;
    }


    /**
     * @return the latitud
     */
    public Double getLatitud() {
        return latitud;
    }

    /**
     * @return the longitud
     */
    public Double getLongitud() {
        return longitud;
    }

    /**
     * @return the direccion
     */
    public String getDireccion() {
        return direccion;
    }

    /**
     * @return the poblacion
     */
    public String getPoblacion() {
        return poblacion;
    }

    /**
     * @return the cp
     */
    public String getCp() {
        return cp;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + id.get();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Lugar other = (Lugar) obj;
        return this.id.get() == other.id.get();
    }

    @Override
    public String toString() {
        return nombre.get();
    }

    @Override
    public int compareTo(Lugar l) {
        return Integer.compare(getId(), l.getId());
    }

}
