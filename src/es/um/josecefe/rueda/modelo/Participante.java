/**
 *
 */
package es.um.josecefe.rueda.modelo;

import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author josec
 *
 * SQL: CREATE TABLE participante ( "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT
 * NULL, "nombre" TEXT NOT NULL, "plazasCoche" INTEGER NOT NULL, "residencia"
 * INTEGER REFERENCES lugar(id) )
 */
@XmlRootElement(name = "participante")
public class Participante implements Comparable<Participante> {

    private final IntegerProperty id;
    private final StringProperty nombre;
    private final IntegerProperty plazasCoche;
    private final ObjectProperty<Lugar> residencia;
    /*
	 * SQL: CREATE TABLE punto_encuentro ( "participante" INTEGER NOT
	 * NULL REFERENCES participante(id) ON DELETE CASCADE, "lugar" INTEGER NOT
	 * NULL REFERENCES lugar(id) ON DELETE CASCADE, "orden" INTEGER NOT NULL,
	 * PRIMARY KEY (participante, lugar, orden) );
	 * 
     */
    private final ListProperty<Lugar> puntosEncuentro;

    /**
     * Constructor por defecto para labores de peristencia
     */
    public Participante() {
        this.id = new SimpleIntegerProperty();
        this.nombre = new SimpleStringProperty();
        this.plazasCoche = new SimpleIntegerProperty();
        this.residencia = new SimpleObjectProperty<>();
        this.puntosEncuentro = new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    /**
     * Constructor de la clase que debe usarse cuando se crea un objeto de este
     * tipo, reservando el constructor por defecto para labores de persistencia
     *
     * @param id Identificador del participante, un número único a partir de 1
     * @param nombre Nombre a mostrar del participante
     * @param plazasCoche Nº de ocupantes del vehículo, incluido el conductor, 0 significa que no tiene coche
     * @param residencia (Opcional) Lugar de residencia - de momento sin uso
     * @param puntosEncuentro Lista con orden que contiene los lugares de encuentro admitidos
     * por el participante por orden de preferencia
     */
    public Participante(int id, String nombre, int plazasCoche, Lugar residencia, List<Lugar> puntosEncuentro) {
        this.id = new SimpleIntegerProperty(id);
        this.nombre = new SimpleStringProperty(nombre);
        this.plazasCoche = new SimpleIntegerProperty(plazasCoche); // Incluido el conductor
        this.residencia = new SimpleObjectProperty<>(residencia);
        this.puntosEncuentro = new SimpleListProperty<>(FXCollections.observableArrayList(puntosEncuentro));
    }

    /**
     * Devuelve el id del participante.
     *
     * @return el id
     */
    public int getId() {
        return id.get();
    }

    /**
     * @return the nombre
     */
    public String getNombre() {
        return nombre.get();
    }

    /**
     * @return the plazasCoche
     */
    public int getPlazasCoche() {
        return plazasCoche.get();
    }

    /**
     * @return the residencia
     */
    public Lugar getResidencia() {
        return residencia.get();
    }

    /**
     * @return the puntosEncuentro
     */
    public List<Lugar> getPuntosEncuentro() {
        return puntosEncuentro;
    }

    public void setId(int id) {
        // Solo se puede establecer el id en un objeto nuevo
        if (this.id.get() == 0) {
            this.id.set(id);
        }
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    public void setPlazasCoche(int plazasCoche) {
        this.plazasCoche.set(plazasCoche);
    }

    public void setResidencia(Lugar residencia) {
        this.residencia.set(residencia);
    }

    public void setPuntosEncuentro(List<Lugar> puntosEncuentro) {
        this.puntosEncuentro.clear();
        this.puntosEncuentro.addAll(puntosEncuentro);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public IntegerProperty plazasCocheProperty() {
        return plazasCoche;
    }

    public ObjectProperty<Lugar> residenciaProperty() {
        return residencia;
    }

    public ListProperty<Lugar> puntosEncuentroProperty() {
        return puntosEncuentro;
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return nombre.get();
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id.get();
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
        return getId() == other.getId();
    }

    @Override
    public int compareTo(Participante o) {
        return Integer.compare(getId(), o.getId());
    }
}
