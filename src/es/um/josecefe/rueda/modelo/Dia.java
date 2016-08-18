/**
 *
 */
package es.um.josecefe.rueda.modelo;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * SQL: CREATE TABLE "dia" ( "id" INTEGER PRIMARY KEY NOT NULL, "descripcion"
 * TEXT );
 *
 * @author josec
 *
 */
@XmlRootElement(name = "dia")
public class Dia implements Comparable<Dia> {

    private final IntegerProperty id;
    private final StringProperty descripcion;

    /**
     * Constructor por defecto para la persistencia
     */
    public Dia() {
        this.id = new SimpleIntegerProperty();
        this.descripcion = new SimpleStringProperty();
    }

    /**
     * Constructor preferido. El Identificador debe ser un número único a partir
     * de 1
     *
     * @param id Número único de identificación a partir del 1
     * @param descripcion Descripción del día en texto
     */
    public Dia(int id, String descripcion) {
        this.id = new SimpleIntegerProperty(id);
        this.descripcion = new SimpleStringProperty(descripcion);
    }

    /**
     * @return the id
     */
    public int getId() {
        return id.get();
    }

    /**
     * @return the descripcion
     */
    public String getDescripcion() {
        return descripcion.get();
    }

    /**
     * El 0 no es un valor valido de ID
     *
     * @param id Valor de Identificación del día, empezando por el 1
     */
    public void setId(int id) {
        if (getId() == 0) {
            this.id.set(id);
        }
    }

    public void setDescripcion(String descripcion) {
        this.descripcion.set(descripcion);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty descripcionProperty() {
        return descripcion;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id.get();
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
        return getId() == other.getId();
    }

    @Override
    public int compareTo(Dia d) {
        return Integer.compare(getId(), d.getId());
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return descripcion.get();
    }
}
