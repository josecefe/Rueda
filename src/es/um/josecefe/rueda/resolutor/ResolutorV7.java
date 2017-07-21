/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.AsignacionDiaV5;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * ResolutorV7
 * <p>
 * Se trata de un resolutor que aplica la tecnica de Ramificación y poda (B&B).
 *
 * @author josecefe
 */
public class ResolutorV7 extends ResolutorAcotado {

    private final static boolean DEBUG = true;
    private final static boolean ESTADISTICAS = true;
    private final static int CADA_EXPANDIDOS = 1000;
    private final static int CADA_MILIS_EST = 1000;

    private static final boolean CON_CAMBIO_DE_ESTRATEGIA = false;

    private static final int PESO_COTA_INFERIOR_NUM_DEF_INI = 1;
    private static final int PESO_COTA_INFERIOR_DEN_DEF_INI = 8;

    private static final int PESO_COTA_INFERIOR_NUM_DEF_FIN = 1;
    private static final int PESO_COTA_INFERIOR_DEN_DEF_FIN = 2;
    private final ContextoResolucion contexto = new ContextoResolucion();
    private final EstadisticasV7 estGlobal = new EstadisticasV7();
    private Set<Horario> horarios;
    private Map<Dia, AsignacionDiaV5> solucionFinal;
    private int[] tamanosNivel;
    private double[] nPosiblesSoluciones;
    private double totalPosiblesSoluciones;
    private long ultMilisEst; // La ultima vez que se hizo estadística

    public ResolutorV7() {

    }

    private void inicializa() {
        continuar = true;

        contexto.inicializa(horarios);

        solucionFinal = null;

        if (ESTADISTICAS) {
            tamanosNivel = IntStream.of(contexto.ordenExploracionDias).map(i -> contexto.solucionesCandidatas.get(contexto.dias[i]).size()).toArray();
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
    public Map<Dia, AsignacionDiaV5> resolver(Set<Horario> horarios) {
        return resolver(horarios, Integer.MAX_VALUE - 1);
    }

    @Override
    public Map<Dia, AsignacionDiaV5> resolver(Set<Horario> horarios, int cotaInfCorteInicial) {
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
        Nodo RAIZ = new Nodo(contexto);
        Nodo actual = RAIZ;
        Nodo mejor = actual;
        int cotaInferiorCorte = cotaInfCorteInicial < Integer.MAX_VALUE ? cotaInfCorteInicial + 1 : cotaInfCorteInicial;
        Collection<Nodo> LNV;
        Supplier<Nodo> opPull;
        contexto.pesoCotaInferiorNum = PESO_COTA_INFERIOR_NUM_DEF_INI; //Primero buscamos en profundidad
        contexto.pesoCotaInferiorDen = PESO_COTA_INFERIOR_DEN_DEF_INI; //Primero buscamos en profundidad
        if (CON_CAMBIO_DE_ESTRATEGIA) {
            ArrayDeque<Nodo> pilaNodosVivos = new ArrayDeque<>(); // Inicialmente es una cola LIFO (pila)
            LNV = pilaNodosVivos;
            opPull = pilaNodosVivos::removeLast; //Para controlar si es pila o cola, inicialmente pila
        } else {
            PriorityQueue<Nodo> colaNodosVivos = new PriorityQueue<>();
            LNV = colaNodosVivos;
            opPull = colaNodosVivos::poll;
        }
        LNV.add(actual);

        // Bucle principal
        do {
            actual = opPull.get();
            if (ESTADISTICAS && estGlobal.incExpandidos() % CADA_EXPANDIDOS == 0L && System.currentTimeMillis() - ultMilisEst > CADA_MILIS_EST) {
                ultMilisEst = System.currentTimeMillis();
                estGlobal.setFitness(cotaInferiorCorte).actualizaProgreso();
                if (DEBUG) {
                    System.out.format("> LNV=%,d, ", LNV.size());
                    System.out.println(estGlobal);
                    System.out.println("-- Trabajando con " + actual);
                }
            }
            if (actual.getCotaInferior() < cotaInferiorCorte) { //Estrategia de poda: si la cotaInferior >= C no seguimos
                if (ESTADISTICAS) {
                    estGlobal.addGenerados(tamanosNivel[actual.getNivel() + 1]);
                }
                if (actual.getNivel() + 2 == contexto.dias.length) { // Los hijos son terminales
                    if (ESTADISTICAS) {
                        estGlobal.addTerminales(tamanosNivel[actual.getNivel() + 1]);
                    }
                    Optional<Nodo> mejorHijo = actual.generaHijos(true).min(Nodo::compareTo); //true para paralelo
                    if (mejorHijo.isPresent() && mejorHijo.get().compareTo(mejor) < 0) {
                        if (mejor == RAIZ) {
                            // Cambiamos los pesos
                            contexto.pesoCotaInferiorNum = PESO_COTA_INFERIOR_NUM_DEF_FIN; //Después buscamos más equilibrado
                            contexto.pesoCotaInferiorDen = PESO_COTA_INFERIOR_DEN_DEF_FIN; //Después buscamos más equilibrado
                            LNV.parallelStream().forEach(Nodo::calculaCosteEstimado);// Hay que forzar el calculo de nuevo de los costes de los nodos
                            PriorityQueue<Nodo> colaNodosVivos;
                            colaNodosVivos = new PriorityQueue<>(LNV.size());
                            colaNodosVivos.addAll(LNV);
                            if (DEBUG) {
                                System.out.println("---- ACTUALIZANDO LA LNV POR CAMBIO DE PESOS");
                            }
                            opPull = colaNodosVivos::poll;
                            LNV = colaNodosVivos;
                        }

                        mejor = mejorHijo.get();
                        if (DEBUG) {
                            System.out.format("$$$$ A partir del padre=%s\n    -> mejoramos con el hijo=%s\n", actual, mejor);
                        }
                        if (mejor.getCosteEstimado() < cotaInferiorCorte) {
                            if (DEBUG) {
                                System.out.format("** Nuevo C: Anterior=%,d, Nuevo=%,d\n", cotaInferiorCorte, mejor.getCosteEstimado());
                            }
                            final int fC = cotaInferiorCorte = mejor.getCosteEstimado();

                            // Limpiamos la lista de nodos vivos de los que no cumplan...
                            int antes;
                            if (ESTADISTICAS) {
                                estGlobal.addDescartados(LNV.parallelStream().filter(n -> n.getCotaInferior() >= fC).mapToDouble(n -> nPosiblesSoluciones[n.getNivel()]).sum());
                                estGlobal.setFitness(cotaInferiorCorte).actualizaProgreso();
                                antes = LNV.size();
                            }
                            boolean removeIf = LNV.removeIf(n -> n.getCotaInferior() >= fC);
                            if (ESTADISTICAS && DEBUG && removeIf) {
                                System.out.format("** Hemos eliminado %,d nodos de la LNV\n", antes - LNV.size());
                            }
                        }
                    }
                } else { // Es un nodo intermedio
                    final int Corte = cotaInferiorCorte;
                    List<Nodo> lNF = actual.generaHijos(true).filter(n -> n.getCotaInferior() < Corte).collect(toList()); //PARALELO poner true
                    OptionalInt menorCotaSuperior = lNF.stream().mapToInt(Nodo::getCotaSuperior).min();
                    if (menorCotaSuperior.isPresent() && menorCotaSuperior.getAsInt() < cotaInferiorCorte) { // Mejora de C
                        if (DEBUG) {
                            System.out.format("** Nuevo C: Anterior=%,d, Nuevo=%,d\n", cotaInferiorCorte, menorCotaSuperior.getAsInt());
                        }
                        cotaInferiorCorte = menorCotaSuperior.getAsInt(); //Actualizamos C
                        final int fC = cotaInferiorCorte;
                        lNF.removeIf(n -> n.getCotaInferior() >= fC); //Recalculamos lNF
                        // Limpiamos la LNV
                        int antes;
                        if (ESTADISTICAS) {
                            estGlobal.addDescartados(LNV.parallelStream().filter(n -> n.getCotaInferior() >= fC).mapToDouble(n -> nPosiblesSoluciones[n.getNivel()]).sum());
                            estGlobal.setFitness(cotaInferiorCorte).actualizaProgreso();
                            antes = LNV.size();
                        }
                        boolean removeIf = LNV.removeIf(n -> n.getCotaInferior() >= fC);
                        if (ESTADISTICAS && DEBUG && removeIf) {
                            System.out.format("## Hemos eliminado %,d nodos de la LNV\n", antes - LNV.size());
                        }
                    }
                    if (ESTADISTICAS) {
                        estGlobal.addDescartados((tamanosNivel[actual.getNivel() + 1] - lNF.size()) * nPosiblesSoluciones[actual.getNivel() + 1]);
                    }
                    LNV.addAll(lNF);
                }
            } else if (ESTADISTICAS) {
                estGlobal.addDescartados(nPosiblesSoluciones[actual.getNivel()]);
            }
        } while (!LNV.isEmpty() && continuar);

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.setFitness(cotaInferiorCorte).actualizaProgreso();
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

    @Override
    public Map<Dia, AsignacionDiaV5> getSolucionFinal() {
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
