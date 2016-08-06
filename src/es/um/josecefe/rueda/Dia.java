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

    private int id;
    private String descripcion;

    /**
     * Constructor por defecto para la persistencia
     */
    public Dia() {
    }
    
    /**
     * Constructor preferido. El Identificador debe ser un número único a partir
     * de 1
     *
     * @param id Número único de identificación a partir del 1
     * @param descripcion Descripción del día en texto
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

    /**
     * El 0 no es un valor valido de ID
     *
     * @param id Valor de Identificación del día, empezando por el 1
     */
    public void setId(int id) {
        if (this.id == 0) {
            this.id = id;
        }
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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
