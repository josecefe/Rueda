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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * SQL: CREATE TABLE "dia" ( "id" INTEGER PRIMARY KEY NOT NULL, "descripcion"
 * TEXT );
 *
 * @author josec
 */
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
     * @param id          Número único de identificación a partir del 1
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
     * El 0 no es un valor valido de ID
     *
     * @param id Valor de Identificación del día, empezando por el 1
     */
    public void setId(int id) {
        if (getId() == 0) {
            this.id.set(id);
        }
    }

    /**
     * @return the descripcion
     */
    public String getDescripcion() {
        return descripcion.get();
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
