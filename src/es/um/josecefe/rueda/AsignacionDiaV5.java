/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static java.util.stream.Collectors.toSet;
import java.util.stream.IntStream;

/**
 *
 * @author josec
 */
public class AsignacionDiaV5 implements Comparable<AsignacionDiaV5>, AsignacionDia {
    private final Participante[] participantes;
    private final boolean[] conductores;
    private final Map<Participante, Lugar> peIda, peVuelta;
    private final int coste;
    
    public AsignacionDiaV5(Participante[] participantes, Set<Participante> conductores, Map<Participante, Lugar> puntoEncuentroIda, Map<Participante, Lugar> puntoEncuentroVuelta, int coste) {
        this.participantes=participantes;
        this.conductores = new boolean[participantes.length];
        for (int i=0; i<participantes.length; i++)
            this.conductores[i]=conductores.contains(participantes[i]);
        this.peIda=puntoEncuentroIda;
        this.peVuelta=puntoEncuentroVuelta;
        this.coste=coste;
    }

    public Participante[] getParticipantes() {
        return participantes;
    }
    
    @Override
    public Set<Participante> getConductores() {
        return IntStream.range(0, conductores.length).filter(i -> conductores[i]).mapToObj(i -> participantes[i]).collect(toSet());
    }
    
    public boolean isConductorPorIndice(int indParticipante) {
        return conductores[indParticipante];
    }
    
    boolean[] getConductoresArray() {
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
    public int compareTo(AsignacionDiaV5 o) {
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
        final AsignacionDiaV5 other = (AsignacionDiaV5) obj;
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
        return "AsignacionDia{ coste=" + coste + ", conductores=" + getConductores() + ", peIda=" + peIda + ", peVuelta=" + peVuelta +  '}';
    }
}
