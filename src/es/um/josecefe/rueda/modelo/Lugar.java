package es.um.josecefe.rueda.modelo;

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
public class Lugar {

    private int id;
    private String nombre;
    private Double latitud;
    private Double longitud;
    private String direccion;
    private String poblacion;
    private String cp;

    /**
     * Constructor por defecto para la persistencia
     */
    public Lugar() {
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
        this.id = id;
        this.nombre = nombre;
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
        return id;
    }

    public void setId(int id) {
        // Solo se puede establecer el id en un objeto nuevo
        if (this.id == 0) {
            this.id = id;
        }
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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
     * @return the nombre
     */
    public String getNombre() {
        return nombre;
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
        hash = 97 * hash + this.id;
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
        return this.id == other.id;
    }

    @Override
    public String toString() {
        return nombre;
    }

}
