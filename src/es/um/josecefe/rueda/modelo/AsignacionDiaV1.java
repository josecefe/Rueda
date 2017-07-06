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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author josec
 */
public class AsignacionDiaV1 implements Comparable<AsignacionDiaV1>, AsignacionDia {
    private final Set<Participante> conductores;
    private final Map<Participante, Lugar> peIda, peVuelta;
    private final int coste;

    public AsignacionDiaV1(Set<Participante> conductores, Map<Participante, Lugar> puntoEncuentroIda, Map<Participante, Lugar> puntoEncuentroVuelta, int coste) {
        this.conductores = conductores;
        this.peIda = puntoEncuentroIda;
        this.peVuelta = puntoEncuentroVuelta;
        this.coste = coste;
    }

    @Override
    public Set<Participante> getConductores() {
        return conductores;
    }

    @Override
    public Map<Participante, Lugar> getPeIda() {
        return peIda;
    }

    @Override
    public Map<Participante, Lugar> getPeVuelta() {
        return peVuelta;
    }

    @Override
    public int getCoste() {
        return coste;
    }

    @Override
    public int compareTo(AsignacionDiaV1 o) {
        return Integer.compare(coste, o.coste);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.conductores);
        hash = 67 * hash + Objects.hashCode(this.peIda);
        hash = 67 * hash + Objects.hashCode(this.peVuelta);
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
        final AsignacionDiaV1 other = (AsignacionDiaV1) obj;
        if (!Objects.equals(this.conductores, other.conductores)) {
            return false;
        }
        if (!Objects.equals(this.peIda, other.peIda)) {
            return false;
        }
        return Objects.equals(this.peVuelta, other.peVuelta);
    }

    @Override
    public String toString() {
        return "AsignacionDia{ coste=" + coste + ", conductores=" + conductores + ", peIda=" + peIda + ", peVuelta=" + peVuelta + '}';
    }
}
