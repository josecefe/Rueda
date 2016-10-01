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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * SQL: CREATE TABLE "horario" ( "participante" INTEGER NOT NULL REFERENCES
 * participante(id) ON DELETE CASCADE, "dia" INTEGER NOT NULL REFERENCES dia(id)
 * ON DELETE CASCADE, "entrada" INTEGER NOT NULL, "salida" INTEGER NOT NULL,
 * "coche" INTEGER NOT NULL DEFAULT (1), PRIMARY KEY (participante,dia) );
 *
 * @author josec
 *
 */
public class Horario implements Comparable<Horario> {

    private final ObjectProperty<Participante> participanteProperty;
    private final ObjectProperty<Dia> diaProperty;
    private final IntegerProperty entradaProperty;
    private final IntegerProperty salidaProperty;
    private final BooleanProperty cocheProperty;

    /**
     * Constructor por defecto para cumplir el estandar de JavaBean. No debería
     * usarse directametne, solo debe usarse para la persistencia.
     */
    public Horario() {
        this.participanteProperty = new SimpleObjectProperty<>();
        this.diaProperty = new SimpleObjectProperty<>();
        this.entradaProperty = new SimpleIntegerProperty();
        this.salidaProperty = new SimpleIntegerProperty();
        this.cocheProperty = new SimpleBooleanProperty();
    }

    /**
     * Crea una porción de horario que corresponde un participante y un día
     * dados
     *
     * @param participante Participante al que pertenece esta parte del horario
     * @param dia Día al que se refiere esta parte
     * @param entrada Hora de entrada del participante ese dia
     * @param salida Hora de salida del participante ese dia
     * @param coche Indica si ese día podría compartir su coche
     */
    public Horario(Participante participante, Dia dia, int entrada, int salida, boolean coche) {
        this.participanteProperty = new SimpleObjectProperty<>(participante);
        this.diaProperty = new SimpleObjectProperty<>(dia);
        this.entradaProperty = new SimpleIntegerProperty(entrada);
        this.salidaProperty = new SimpleIntegerProperty(salida);
        this.cocheProperty = new SimpleBooleanProperty(coche);
    }

    /**
     * @return el participante
     */
    public Participante getParticipante() {
        return participanteProperty.get();
    }

    /**
     * @return el dia
     */
    public Dia getDia() {
        return diaProperty.get();
    }

    /**
     * @return la hora de entrada
     */
    public int getEntrada() {
        return entradaProperty.get();
    }

    /**
     * @return la hora de salida
     */
    public int getSalida() {
        return salidaProperty.get();
    }

    /**
     * @return the coche
     */
    public boolean isCoche() {
        return cocheProperty.get();
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Horario [participante=" + getParticipante() + ", dia=" + getDia() + ", entrada=" + getEntrada() + ", salida=" + getSalida()
                + ", coche=" + isCoche() + "]";
    }

    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getDia() == null) ? 0 : getDia().hashCode());
        result = prime * result + ((getParticipante() == null) ? 0 : getParticipante().hashCode());
        return result;
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
        Horario other = (Horario) obj;
        if (getDia() == null) {
            if (other.getDia() != null) {
                return false;
            }
        } else if (!getDia().equals(other.getDia())) {
            return false;
        }
        if (getParticipante() == null) {
            if (other.getParticipante() != null) {
                return false;
            }
        } else if (!getParticipante().equals(other.getParticipante())) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
	 * @see java.lang.comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Horario o) {
        int res = getDia().compareTo(o.getDia());
        if (res == 0) {
            res = getParticipante().compareTo(o.getParticipante());
        }

        return res;
    }

    /**
     * Permite fijar el participante de esta entrada en el horario Nota: Este
     * método se incluye para la persistencia
     *
     * @param participante Participante al que pertenece esta entrada
     */
    public void setParticipante(Participante participante) {
        this.participanteProperty.set(participante);
    }

    /**
     * Permite fija el dia de esta entrada de horario. Perticipante + Dia no se
     * pueden repetir Nota: Este método se incluye para la persistencia
     *
     * @param dia El día al que pertenece esta entrada
     */
    public void setDia(Dia dia) {
        this.diaProperty.set(dia);
    }

    /**
     * Permite fijar la hora de entrada (entendida como un entero, usualmente
     * empezando por 1 para primera hora, 2 para la segunda, etc.) Nota: Este
     * método se incluye para la persistencia
     *
     * @param entrada Hora de entrada (como un entero referido a 1º hora = 1, 2º
     * hora = 2, etc)
     */
    public void setEntrada(int entrada) {
        this.entradaProperty.set(entrada);
    }

    /**
     * Permite fijar la hora de salida (entendida como un entero, usualmente
     * empezando por 1 para primera hora, 2 para la segunda, etc.) Nota: Este
     * método se incluye para la persistencia
     *
     * @param salida Hora de salida (como un entero referido a 1º hora = 1, 2º
     * hora = 2, etc)
     */
    public void setSalida(int salida) {
        this.salidaProperty.set(salida);
    }

    /**
     * Indica si el participante dispone de coche para ese día
     *
     * @param coche Indica si puede disponer de coche o no ese día
     */
    public void setCoche(boolean coche) {
        this.cocheProperty.set(coche);
    }

    /* FX Properties */
    public ObjectProperty<Participante> participanteProperty() {
        return participanteProperty;
    }

    public ObjectProperty<Dia> diaProperty() {
        return diaProperty;
    }

    public IntegerProperty entradaProperty() {
        return entradaProperty;
    }

    public IntegerProperty salidaProperty() {
        return salidaProperty;
    }

    public BooleanProperty cocheProperty() {
        return cocheProperty;
    }
}
