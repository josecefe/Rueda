/**
 *
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.AsignacionDiaV5;
import es.um.josecefe.rueda.modelo.Horario;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static java.util.stream.Collectors.toMap;

/**
 * @author josec
 *
 */
/**
 * Resolutor
 */
public class ResolutorV7 extends ResolutorAcotado {

    final static boolean DEBUG = false;
    private final static boolean ESTADISTICAS = true;
    private final static int CADA_EXPANDIDOS = 1000;
    private final static int CADA_MILIS_EST = 1000;

    private static final boolean CON_CAMBIO_DE_ESTRATEGIA = false;

    private Set<Horario> horarios;

    private final ContextoResolucion contexto = new ContextoResolucion();

    private Map<Dia, AsignacionDiaV5> solucionFinal;
    private int[] tamanosNivel;
    private double[] nPosiblesSoluciones;
    private double totalPosiblesSoluciones;

    private int cotaInferiorCorte = Integer.MAX_VALUE;
    private Nodo RAIZ;
    private final EstadisticasV1 estGlobal = new EstadisticasV1();
    private long ultMilisEst; // La ultima vez que se hizo estadística

    public ResolutorV7() {

    }

    private void inicializa() {
        continuar = true;

        contexto.inicializa(horarios);
        
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
    public Map<Dia, AsignacionDiaV5> resolver(Set<Horario> horarios) {
        return resolver(horarios, Integer.MAX_VALUE-1);
    }

    /**
     *
     * @param horarios
     * @param cotaInfCorte
     * @return
     */
    @Override
    public Map<Dia, AsignacionDiaV5> resolver(Set<Horario> horarios, int cotaInfCorte) {
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
        cotaInferiorCorte = cotaInfCorte < Integer.MAX_VALUE ?  cotaInfCorte + 1 : cotaInfCorte; //Lo tomamos como cota superior
        Collection<Nodo> LNV;
        Supplier<Nodo> opPull;
        contexto.pesoCotaInferior = 0.1; //Primero buscamos en profundidad
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
                    estGlobal.addGenerados(tamanosNivel[actual.getIndiceDia() + 1]);
                }
                if (actual.getIndiceDia() + 2 == contexto.dias.length) { // Los hijos son terminales
                    if (ESTADISTICAS) {
                        estGlobal.addTerminales(tamanosNivel[actual.getIndiceDia() + 1]);
                    }
                    Optional<Nodo> mejorHijo = actual.generaHijos(true).min(Nodo::compareTo); //true para paralelo
                    if (mejorHijo.isPresent() && mejorHijo.get().compareTo(mejor) < 0) {
                        if (mejor == RAIZ) {
                            contexto.pesoCotaInferior = 0.55; //Después buscamos más en anchura
                            PriorityQueue<Nodo> colaNodosVivos;
                            if (CON_CAMBIO_DE_ESTRATEGIA) {
                                colaNodosVivos = new PriorityQueue<>(LNV);
                                if (DEBUG) {
                                    System.out.println("---- CAMBIANDO A COLA DE PRIORIDAD");
                                }
                            } else {
                                colaNodosVivos = new PriorityQueue<>(LNV.size());
                                colaNodosVivos.addAll(LNV);
                                if (DEBUG) {
                                    System.out.println("---- ACTUALIZANDO LA LNV POR CAMBIO DE PESOS");
                                }
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
                                estGlobal.addDescartados(LNV.parallelStream().filter(n -> n.getCotaInferior() >= fC).mapToDouble(n -> nPosiblesSoluciones[n.getIndiceDia()]).sum());
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
                            estGlobal.addDescartados(LNV.parallelStream().filter(n -> n.getCotaInferior() >= fC).mapToDouble(n -> nPosiblesSoluciones[n.getIndiceDia()]).sum());
                            estGlobal.setFitness(cotaInferiorCorte).actualizaProgreso();
                            antes = LNV.size();
                        }
                        boolean removeIf = LNV.removeIf(n -> n.getCotaInferior() >= fC);
                        if (ESTADISTICAS && DEBUG && removeIf) {
                            System.out.format("## Hemos eliminado %,d nodos de la LNV\n", antes - LNV.size());
                        }
                    }
                    if (ESTADISTICAS) {
                        estGlobal.addDescartados((tamanosNivel[actual.getIndiceDia() + 1] - lNF.size()) * nPosiblesSoluciones[actual.getIndiceDia() + 1]);
                    }
                    LNV.addAll(lNF);
                }
            } else if (ESTADISTICAS) {
                estGlobal.addDescartados(nPosiblesSoluciones[actual.getIndiceDia()]);
            }
        } while (!LNV.isEmpty() && continuar);

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.setFitness(cotaInferiorCorte ).actualizaProgreso();
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
        if (mejor.compareTo(RAIZ) < 0) {
            Iterator<AsignacionDiaV5> i = mejor.getEleccion().iterator();
            solucionFinal = Stream.of(contexto.dias).collect(toMap(Function.identity(), d -> i.next()));
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
        super.setEstrategia(estrategia);
        contexto.estrategia = getEstrategia();
    }
}
