/**
 *
 */
package es.um.josecefe.rueda.modelo;

import java.util.List;

/**
 * @author josec
 *
 * SQL: CREATE TABLE participante ( "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT
 * NULL, "nombre" TEXT NOT NULL, "plazasCoche" INTEGER NOT NULL, "residencia"
 * INTEGER REFERENCES lugar(id) )
 */
public class Participante implements Comparable<Participante> {

    private int id;
    private String nombre;
    private int plazasCoche;
    private Lugar residencia;
    /*
	 * SQL: CREATE TABLE punto_encuentro ( "participante" INTEGER NOT
	 * NULL REFERENCES participante(id) ON DELETE CASCADE, "lugar" INTEGER NOT
	 * NULL REFERENCES lugar(id) ON DELETE CASCADE, "orden" INTEGER NOT NULL,
	 * PRIMARY KEY (participante, lugar, orden) );
	 * 
     */
    private List<Lugar> puntosEncuentro;

    /**
     * Constructor por defecto para labores de peristencia
     */
    public Participante() {
    }

    /**
     * Constructor de la clase que debe usarse cuando se crea un objeto de este
     * tipo, reservando el constructor por defecto para labores de persistencia
     *
     * @param id Identificador del participante, un número único a partir de 1
     * @param nombre Nombre a mostrar del participante
     * @param plazasCoche Nº de ocupantes del vehículo, incluido el conductor
     * @param residencia (Opcional) Lugar de residencia
     * @param puntosEncuentro Lista con orden que contiene los lugares admitidos
     * por el participante por orden de preferencia
     */
    public Participante(int id, String nombre, int plazasCoche, Lugar residencia, List<Lugar> puntosEncuentro) {
        this.id = id;
        this.nombre = nombre;
        this.plazasCoche = plazasCoche; // Incluido el conductor
        this.residencia = residencia;
        this.puntosEncuentro = puntosEncuentro;
    }

    /**
     * Devuelve el id del participante.
     *
     * @return el id
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
     * @return the plazasCoche
     */
    public int getPlazasCoche() {
        return plazasCoche;
    }

    /**
     * @return the residencia
     */
    public Lugar getResidencia() {
        return residencia;
    }

    /**
     * @return the puntosEncuentro
     */
    public List<Lugar> getPuntosEncuentro() {
        return puntosEncuentro;
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

    public void setPlazasCoche(int plazasCoche) {
        this.plazasCoche = plazasCoche;
    }

    public void setResidencia(Lugar residencia) {
        this.residencia = residencia;
    }

    public void setPuntosEncuentro(List<Lugar> puntosEncuentro) {
        this.puntosEncuentro = puntosEncuentro;
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return nombre;
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id;
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
     */
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
        Participante other = (Participante) obj;
        return id == other.id;
    }

    @Override
    public int compareTo(Participante o) {
        return Integer.compare(id, o.getId());
    }

}
