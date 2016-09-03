/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda.modelo;

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
    private Participante[] participantes;
    private boolean[] conductores;
    private int numConductores;
    private Map<Participante, Lugar> peIda, peVuelta;
    private int coste;

    /**
     * Constructor pensado para la persistencia
     */
    public AsignacionDiaV5() {
    }
    
    /**
     * Constructor normal
     * 
     * @param participantes
     * @param conductores
     * @param puntoEncuentroIda
     * @param puntoEncuentroVuelta
     * @param coste 
     */
    public AsignacionDiaV5(Participante[] participantes, Set<Participante> conductores, Map<Participante, Lugar> puntoEncuentroIda, Map<Participante, Lugar> puntoEncuentroVuelta, int coste) {
        this.participantes=participantes;
        this.conductores = new boolean[participantes.length];
        for (int i=0; i<participantes.length; i++)
            this.conductores[i]=conductores.contains(participantes[i]);
        this.numConductores = conductores.size();
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
    
    public boolean[] getConductoresArray() {
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
        return Integer.compare(numConductores * 1000 + coste, o.numConductores * 1000 + o.coste);
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

    public void setParticipantes(Participante[] participantes) {
        this.participantes = participantes;
    }

    public void setConductoresArray(boolean[] conductores) {
        this.conductores = conductores;
    }

    public void setPeIda(Map<Participante, Lugar> peIda) {
        this.peIda = peIda;
    }

    public void setPeVuelta(Map<Participante, Lugar> peVuelta) {
        this.peVuelta = peVuelta;
    }

    public void setCoste(int coste) {
        this.coste = coste;
    }
    
    
}
