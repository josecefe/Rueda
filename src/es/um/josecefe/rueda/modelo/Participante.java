/*
 * Copyright (C) 2016 José Ceferino Ortega
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.modelo;

import javafx.beans.property.*;
import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.List;

/**
 * @author josec
 * <p>
 * SQL: CREATE TABLE participante ( "id" INTEGER PRIMARY KEY AUTOINCREMENT NOT
 * NULL, "nombre" TEXT NOT NULL, "plazasCoche" INTEGER NOT NULL, "residencia"
 * INTEGER REFERENCES lugar(id) )
 */
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
     * @param id              Identificador del participante, un número único a partir de 1
     * @param nombre          Nombre a mostrar del participante
     * @param plazasCoche     Nº de ocupantes del vehículo, incluido el conductor, 0 significa que no tiene coche
     * @param residencia      (Opcional) Lugar de residencia - de momento sin uso
     * @param puntosEncuentro Lista con orden que contiene los lugares de encuentro admitidos
     *                        por el participante por orden de preferencia
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

    public void setId(int id) {
        // Solo se puede establecer el id en un objeto nuevo
        if (this.id.get() == 0) {
            this.id.set(id);
        }
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

    /**
     * @return the plazasCoche
     */
    public int getPlazasCoche() {
        return plazasCoche.get();
    }

    public void setPlazasCoche(int plazasCoche) {
        this.plazasCoche.set(plazasCoche);
    }

    /**
     * @return the residencia
     */
    public Lugar getResidencia() {
        return residencia.get();
    }

    public void setResidencia(Lugar residencia) {
        this.residencia.set(residencia);
    }

    /**
     * @return the puntosEncuentro
     */
    public List<Lugar> getPuntosEncuentro() {
        return new ArrayList<>(puntosEncuentro);
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
        return id.get() == other.id.get();
    }

    @Override
    public int compareTo(Participante o) {
        return Integer.compare(getId(), o.getId());
    }
}
