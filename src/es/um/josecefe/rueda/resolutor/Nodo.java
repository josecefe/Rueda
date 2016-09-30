/*
 * Copyright (C) 2016 josec
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
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.AsignacionDiaV5;
import es.um.josecefe.rueda.modelo.Dia;
import static es.um.josecefe.rueda.resolutor.Pesos.PESO_DIF_MAX_MIN_VECES_CONDUCTOR;
import static es.um.josecefe.rueda.resolutor.Pesos.PESO_MAXIMO_VECES_CONDUCTOR;
import static es.um.josecefe.rueda.resolutor.Pesos.PESO_TOTAL_CONDUCTORES;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

//Function<Nodo, Double> funcionCoste;
final class Nodo implements Comparable<Nodo> {

    final static boolean DEBUG = true;

    private final Nodo padre;
    private final AsignacionDiaV5 eleccion;
    private final byte[] vecesConductor;
    private final int indiceDia;
    private final int costeAcumulado;
    private final int cotaInferior;
    private final int cotaSuperior;
    private final ContextoResolucion contexto;
    private int costeEstimado;

    Nodo(final ContextoResolucion contexto) {
        this.contexto = contexto;
        padre = null;
        eleccion = null;
        vecesConductor = new byte[contexto.participantes.length];
        costeAcumulado = cotaInferior = 0;
        costeEstimado = cotaSuperior = Integer.MAX_VALUE;
        indiceDia = -1;
    }

    Nodo(Nodo padre, AsignacionDiaV5 nuevaAsignacion, final ContextoResolucion contexto) {
        this.contexto = contexto;
        this.padre = padre;
        indiceDia = padre.indiceDia + 1;
        eleccion = nuevaAsignacion;
        costeAcumulado = padre.costeAcumulado + nuevaAsignacion.getCoste();
        vecesConductor = Arrays.copyOf(padre.vecesConductor, padre.vecesConductor.length);
        boolean[] conductores = nuevaAsignacion.getConductoresArray();
        int maximo = 0;
        int maxCS = 0;
        int maxCI = 0;
        int total = 0;
        int totalMinRes = 0;
        int totalMaxRes = 0;
        int minimo = Integer.MAX_VALUE;
        int minCI = Integer.MAX_VALUE;
        int minCS = Integer.MAX_VALUE;
        final boolean terminal = contexto.dias.length == indiceDia + 1;
        //int sum = 0;
        //nuevaAsignacion.getConductores().stream().forEachOrdered(ic -> ++vecesConductor[ic]);
        for (int i = 0; i < vecesConductor.length; i++) {
            if (contexto.participantesConCoche[i]) {
                //sum = vecesConductor[i];
                if (conductores[i]) {
                    ++vecesConductor[i];
                }
                total += vecesConductor[i];
                int vecesConductorVirt = Math.round((float) (vecesConductor[i] * contexto.coefConduccion[i]));
                if (vecesConductorVirt > maximo) {
                    maximo = vecesConductorVirt;
                }
                if (vecesConductorVirt < minimo) {
                    minimo = vecesConductorVirt;
                }
                if (!terminal) {
                    int vecesConductorVirtCS = Math.round((float) ((vecesConductor[i] + contexto.maxVecesCondDia[indiceDia + 1][i]) * contexto.coefConduccion[i]));
                    if (vecesConductorVirtCS > maxCS) {
                        maxCS = vecesConductorVirtCS;
                    }
                    if (vecesConductorVirtCS < minCS) {
                        minCS = vecesConductorVirtCS;
                    }
                    int vecesConductorVirtCI = Math.round((float) ((vecesConductor[i] + contexto.minVecesCondDia[indiceDia + 1][i]) * contexto.coefConduccion[i]));
                    if (vecesConductorVirtCI > maxCI) {
                        maxCI = vecesConductorVirtCI;
                    }
                    if (vecesConductorVirtCI < minCI) {
                        minCI = vecesConductorVirtCI;
                    }
                    if (contexto.estrategia == Resolutor.Estrategia.MINCONDUCTORES) {
                        totalMaxRes += contexto.maxVecesCondDia[indiceDia + 1][i];
                        totalMinRes += contexto.minVecesCondDia[indiceDia + 1][i];
                    }
                }
            }
        }
        if (terminal) {
            //Es terminal
            if (contexto.estrategia == Resolutor.Estrategia.EQUILIBRADO) {
                cotaSuperior = cotaInferior = maximo * PESO_MAXIMO_VECES_CONDUCTOR + (maximo - minimo) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado;
            } else {
                // estrategia == Estrategia.MINCONDUCTORES
                cotaSuperior = cotaInferior = maximo * PESO_MAXIMO_VECES_CONDUCTOR + total * PESO_TOTAL_CONDUCTORES + (maximo - minimo) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado;
            }
        } else if (contexto.estrategia == Resolutor.Estrategia.EQUILIBRADO) {
            cotaInferior = maxCI * PESO_MAXIMO_VECES_CONDUCTOR + (maxCI - minCS) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado + contexto.mejorCosteDia[indiceDia + 1];
            cotaSuperior = maxCS * PESO_MAXIMO_VECES_CONDUCTOR + (maxCS - minCI) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado + contexto.peorCosteDia[indiceDia + 1] + 1; // Añadimos el 1 para evitar un bug que nos haría perder una solución
        } else {
            // estrategia == Estrategia.MINCONDUCTORES
            cotaInferior = maxCI * PESO_MAXIMO_VECES_CONDUCTOR + (total + totalMinRes) * PESO_TOTAL_CONDUCTORES + (maxCI - minCS) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado + contexto.mejorCosteDia[indiceDia + 1];
            cotaSuperior = maxCS * PESO_MAXIMO_VECES_CONDUCTOR + (total + totalMaxRes) * PESO_TOTAL_CONDUCTORES + (maxCS - minCI) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + costeAcumulado + contexto.peorCosteDia[indiceDia + 1] + 1; // Añadimos el 1 para evitar un bug que nos haría perder una solución
        }
        if (DEBUG && (cotaInferior < padre.cotaInferior || cotaSuperior > padre.cotaSuperior || cotaSuperior < cotaInferior)) {
            System.err.println("*****************\n**** ¡LIADA!:\n --> Padre=" + padre + "\n --> Hijo=" + this);
        }
        calculaCosteEstimado();
    }

    int getCotaSuperior() {
        return cotaSuperior;
    }

    int getCotaInferior() {
        return cotaInferior;
    }

    void calculaCosteEstimado() {
        costeEstimado = (contexto.pesoCotaInferiorNum * cotaInferior
                + cotaSuperior * (contexto.pesoCotaInferiorDen - contexto.pesoCotaInferiorNum)) / contexto.pesoCotaInferiorDen;
    }

    int getCosteEstimado() {
        return costeEstimado;
    }

    AsignacionDiaV5 getEleccion() {
        return eleccion;
    }

    int getIndiceDia() {
        return indiceDia;
    }

    Dia getDia() {
        return indiceDia >= 0 ? contexto.dias[indiceDia] : null;
    }

    Map<Dia, AsignacionDiaV5> getSolucion() {
        Map<Dia, AsignacionDiaV5> solucion = padre != null ? padre.getSolucion() : new HashMap<>();
        if (indiceDia >= 0) {
            solucion.put(getDia(), eleccion);
        }
        return solucion;
    }

    Stream<Nodo> generaHijos(boolean paralelo) {
        return (paralelo ? contexto.solucionesCandidatas.get(contexto.dias[indiceDia + 1]).parallelStream() : contexto.solucionesCandidatas.get(contexto.dias[indiceDia + 1]).stream()).map(this::generaHijo);
    }

    Nodo generaHijo(AsignacionDiaV5 solDia) {
        return new Nodo(this, solDia, contexto);
    }

    @Override
    public int compareTo(Nodo o) {
        return getCosteEstimado() - o.getCosteEstimado();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.eleccion, padre, indiceDia);
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
        final Nodo other = (Nodo) obj;
        if (this.indiceDia != other.indiceDia) {
            return false;
        }
        return Objects.equals(this.eleccion, other.eleccion) && Objects.equals(this.padre, other.padre);

    }

    @Override
    public String toString() {
        return String.format("Nodo{nivel=%d, estimado=%,d, inferior=%,d, superior=%,d}", indiceDia, getCosteEstimado(), getCotaInferior(), getCotaSuperior());
    }

}
