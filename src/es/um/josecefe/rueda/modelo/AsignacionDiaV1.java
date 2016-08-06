/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda.modelo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author josec
 */
public class AsignacionDiaV1 implements Comparable<AsignacionDiaV1>, AsignacionDia {
    private final Set<Participante> conductores;
    private final Map<Participante, Lugar> peIda, peVuelta;
    private final int coste;
    
    public AsignacionDiaV1(Set<Participante> conductores, Map<Participante, Lugar> puntoEncuentroIda, Map<Participante, Lugar> puntoEncuentroVuelta, int coste) {
        this.conductores=conductores;
        this.peIda=puntoEncuentroIda;
        this.peVuelta=puntoEncuentroVuelta;
        this.coste=coste;
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
        return "AsignacionDia{ coste=" + coste + ", conductores=" + conductores + ", peIda=" + peIda + ", peVuelta=" + peVuelta +  '}';
    }
}
