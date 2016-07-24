/**
 *
 */
package es.um.josecefe.rueda;

/**
 *
 * SQL: CREATE TABLE "dia" ( "id" INTEGER PRIMARY KEY NOT NULL, "descripcion"
 * TEXT );
 *
 * @author josec
 *
 */
public class Dia implements Comparable<Dia> {

    private final int id;
    private final String descripcion;

    /**
     * @param id
     * @param descripcion
     */
    public Dia(int id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the descripcion
     */
    public String getDescripcion() {
        return descripcion;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id;
    }

    /*
	 * (non-Javadoc)
	 * 
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
        Dia other = (Dia) obj;
        return id == other.id;
    }

    @Override
    public int compareTo(Dia d) {
        return Integer.compare(id, d.id);
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return descripcion;
    }

}
