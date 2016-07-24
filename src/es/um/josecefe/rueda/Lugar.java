package es.um.josecefe.rueda;

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

    private final int id;
    private final String nombre;
    private final Double latitud;
    private final Double longitud;
    private final String direccion;
    private final String poblacion;
    private final String cp;

    /**
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
