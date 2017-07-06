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
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.AsignacionDiaV5;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * ResolutorV8
 * <p>
 * Versión paralela del resolutor -> No implementa LNV, sino que sigue una
 * estrategia no guiada, solo podada (la estimación es innecesaria)
 *
 * @author josecefe
 */
public class ResolutorV8 extends ResolutorAcotado {

    private final static boolean DEBUG = false;

    private final static boolean ESTADISTICAS = true;
    private final static int CADA_EXPANDIDOS_EST = 1000;
    private final static int CADA_MILIS_EST = 1000;

    private static final int PESO_COTA_INFERIOR_NUM_DEF = 1;
    private static final int PESO_COTA_INFERIOR_DEN_DEF = 2;
    private final ContextoResolucion contexto = new ContextoResolucion();
    private final EstadisticasV8 estGlobal = new EstadisticasV8();
    private Set<Horario> horarios;
    private Map<Dia, AsignacionDiaV5> solucionFinal;
    private int[] tamanosNivel;
    private double[] nPosiblesSoluciones;
    private double totalPosiblesSoluciones;
    private AtomicInteger cotaInferiorCorte;
    private Nodo RAIZ;
    private long ultMilisEst; // La ultima vez que se hizo estadística

    public ResolutorV8() {

    }

    private void inicializa() {
        continuar = true;

        contexto.inicializa(horarios);
        contexto.pesoCotaInferiorNum = PESO_COTA_INFERIOR_NUM_DEF;
        contexto.pesoCotaInferiorDen = PESO_COTA_INFERIOR_DEN_DEF;

        solucionFinal = null;

        if (ESTADISTICAS) {
            tamanosNivel = contexto.solucionesCandidatas.keySet().stream().mapToInt(k -> contexto.solucionesCandidatas.get(k).size()).toArray();
            totalPosiblesSoluciones = IntStream.of(tamanosNivel).mapToDouble(i -> (double) i).reduce(1.0, (a, b) -> a * b);
            if (DEBUG) {
                System.out.println("Nº de posibles soluciones: " + IntStream.of(tamanosNivel).mapToObj(Double::toString).collect(Collectors.joining(" * ")) + " = "
                        + totalPosiblesSoluciones);
            }
            double acum = 1.0;
            nPosiblesSoluciones = new double[tamanosNivel.length];

            for (int i = tamanosNivel.length - 1; i >= 0; i--) {
                nPosiblesSoluciones[i] = acum;
                acum = acum * tamanosNivel[i];
            }
        }
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios) {
        return resolver(horarios, Integer.MAX_VALUE - 1);
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios, int cotaInfCorteInicial) {
        this.horarios = horarios;
        if (horarios.isEmpty()) {
            return Collections.emptyMap();
        }

        if (ESTADISTICAS) {
            estGlobal.inicia();
        }

        inicializa();

        if (ESTADISTICAS) {
            estGlobal.setTotalPosiblesSoluciones(totalPosiblesSoluciones);
            estGlobal.actualizaProgreso();
            if (DEBUG) {
                System.out.format("Tiempo inicializar =%s\n", estGlobal.getTiempoString());
            }
        }

        // Preparamos el algoritmo
        RAIZ = new Nodo(contexto);
        Nodo actual = RAIZ;
        Nodo mejor = actual;
        cotaInferiorCorte = new AtomicInteger(cotaInfCorteInicial + 1); //Lo tomamos como cota superior

        mejor = branchAndBound(actual, mejor).orElse(mejor); //Cogemos el nuevo mejor

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.setFitness(mejor.getCosteEstimado());
            estGlobal.actualizaProgreso();
            if (DEBUG) {
                System.out.println("====================");
                System.out.println("Estadísticas finales");
                System.out.println("====================");
                System.out.println(estGlobal);
                System.out.println("Solución final=" + mejor);
                System.out.println("-----------------------------------------------");
            }
        }
        // Construimos la solución final
        if (mejor.getCosteEstimado() < cotaInfCorteInicial) {
            solucionFinal = mejor.getSolucion();
        } else {
            solucionFinal = null;
        }

        return solucionFinal;
    }

    private Optional<Nodo> branchAndBound(Nodo actual, Nodo mejorPadre) {
        Nodo mejor = mejorPadre;
        if (ESTADISTICAS && estGlobal.incExpandidos() % CADA_EXPANDIDOS_EST == 0L && System.currentTimeMillis() - ultMilisEst > CADA_MILIS_EST) {
            ultMilisEst = System.currentTimeMillis();
            estGlobal.setFitness(cotaInferiorCorte.get());
            estGlobal.actualizaProgreso();
            if (DEBUG) {
                System.out.println(estGlobal);
                System.out.println("-- Trabajando con " + actual);
            }
        }
        if (actual.getCotaInferior() < cotaInferiorCorte.get() && continuar) { //Estrategia de poda: si la cotaInferior >= C no seguimos
            if (ESTADISTICAS) {
                estGlobal.addGenerados(tamanosNivel[actual.getIndiceDia() + 1]);
            }
            if (actual.getIndiceDia() + 2 == contexto.dias.length) { // Los hijos son terminales
                if (ESTADISTICAS) {
                    estGlobal.addTerminales(tamanosNivel[actual.getIndiceDia() + 1]);
                }
                Optional<Nodo> mejorHijo = actual.generaHijos(true).min(Nodo::compareTo); //true para paralelo

                if (mejorHijo.isPresent() && mejorHijo.get().compareTo(mejor) < 0) {
                    mejor = mejorHijo.get();
                    int cota;
                    do {
                        cota = cotaInferiorCorte.get();
                    }
                    while (mejor.getCosteEstimado() < cota && !cotaInferiorCorte.compareAndSet(cota, mejor.getCosteEstimado()));
                    if (mejor.getCosteEstimado() < cota) {
                        if (ESTADISTICAS) {
                            estGlobal.setFitness(mejor.getCosteEstimado());
                            estGlobal.actualizaProgreso();
                        }
                        if (DEBUG) {
                            System.out.format("$$$$ A partir del padre=%s\n    -> mejoramos con el hijo=%s\n", actual, mejor);
                            System.out.format("** Nuevo (nueva solución) C: Anterior=%,d, Nuevo=%,d\n", cota, mejor.getCosteEstimado());
                        }
                    }
                }
            } else { // Es un nodo intermedio
                List<Nodo> lNF = actual.generaHijos(true).filter(n -> n.getCotaInferior() < cotaInferiorCorte.get()).collect(toList()); //PARALELO poner true
                OptionalInt menorCotaSuperior = lNF.stream().mapToInt(Nodo::getCotaSuperior).min();
                if (menorCotaSuperior.isPresent()) {
                    int cota;
                    do {
                        cota = cotaInferiorCorte.get();
                    }
                    while (menorCotaSuperior.getAsInt() < cota && !cotaInferiorCorte.compareAndSet(cota, menorCotaSuperior.getAsInt()));
                    if (menorCotaSuperior.getAsInt() < cota) {
                        if (ESTADISTICAS) {
                            estGlobal.setFitness(menorCotaSuperior.getAsInt());
                            estGlobal.actualizaProgreso();
                        }
                        if (DEBUG) {
                            System.out.format("** Nuevo C: Anterior=%,d, Nuevo=%,d\n", cota, menorCotaSuperior.getAsInt());
                        }
                    }
                    lNF.removeIf(n -> n.getCotaInferior() >= cotaInferiorCorte.get()); //Recalculamos lNF
                    // Limpiamos la LNV
                }
                if (ESTADISTICAS) {
                    estGlobal.addDescartados((tamanosNivel[actual.getIndiceDia() + 1] - lNF.size()) * nPosiblesSoluciones[actual.getIndiceDia() + 1]);
                }
                final Nodo mejorAhora = mejor;
                Optional<Nodo> mejorHijo = lNF.parallelStream().sorted().map(n -> branchAndBound(n, mejorAhora)).filter(Optional<Nodo>::isPresent).map(Optional<Nodo>::get).min(Nodo::compareTo);
                if (mejorHijo.isPresent() && mejorHijo.get().compareTo(mejor) < 0) { // Tenemos un hijo que mejora
                    mejor = mejorHijo.get();
                }
            }
        } else if (ESTADISTICAS) {
            if (continuar) {
                estGlobal.addDescartados(nPosiblesSoluciones[actual.getIndiceDia()]);
            }
        }

        return Optional.ofNullable(mejor);
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> getSolucionFinal() {
        return solucionFinal;
    }

    @Override
    public Estadisticas getEstadisticas() {
        return estGlobal;
    }

    @Override
    public void setEstrategia(Estrategia estrategia) {
        contexto.estrategia = estrategia;
    }
}
