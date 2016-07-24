/**
 *
 */
package es.um.josecefe.rueda;

import java.util.List;

/**
 * @author josec
 *
 * SQL: CREATE TABLE participante ( "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT
 * NULL, "nombre" TEXT NOT NULL, "plazasCoche" INTEGER NOT NULL, "residencia"
 * INTEGER REFERENCES lugar(id) )
 */
public class Participante implements Comparable<Participante> {

    private final int id;
    private final String nombre;
    private final int plazasCoche;
    private final Lugar residencia;
    /*
	 * SQL: CREATE TABLE punto_encuentro ( "participante" INTEGER NOT
	 * NULL REFERENCES participante(id) ON DELETE CASCADE, "lugar" INTEGER NOT
	 * NULL REFERENCES lugar(id) ON DELETE CASCADE, "orden" INTEGER NOT NULL,
	 * PRIMARY KEY (participante, lugar, orden) );
	 * 
     */
    private final List<Lugar> puntosEncuentro;

    /**
     * @param id
     * @param nombre
     * @param plazasCoche Nº de ocupantes del vehículo, incluido el conductor
     * @param residencia
     * @param puntosEncuentro
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
