/**
 *
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.Participante;
import es.um.josecefe.rueda.modelo.AsignacionDiaV1;
import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * @author josec
 *
 */
/**
 * Resolutor
 */
public class ResolutorV3 extends Resolutor {

    final static long CADA_EXPANDIDOS = 100000L;
    private Set<Horario> horarios;
    private Dia[] dias;
    private Participante[] participantes;
    private Map<Dia, List<Set<Participante>>> solucionesCandidatas;
    private Map<Dia, AsignacionDiaV1> solucionFinal;
    private int[] tamanosNivel;
    private double[] nPosiblesSoluciones;
    private double totalPosiblesSoluciones;
    private EstadisticasV1 estadisticas = new EstadisticasV1();

    @Override
    public Estadisticas getEstadisticas() {
        return estadisticas;
    }

    public ResolutorV3(){
        
    }

    private void inicializa() {
        continuar = true;
        dias = horarios.stream().map(Horario::getDia).distinct().sorted().toArray(Dia[]::new);
        participantes = horarios.stream().map(Horario::getParticipante).distinct().sorted().toArray(Participante[]::new);
        solucionesCandidatas = new HashMap<>(dias.length);
        solucionFinal = null;

        // Vamos a trabajar día a día
        for (Dia d : dias) {
            ArrayList<Set<Participante>> solucionesDia = new ArrayList<>();
            Set<Horario> horariosDia = horarios.stream().filter(h -> h.getDia() == d).collect(toSet());
            Map<Participante, Horario> participanteHorario = horariosDia.stream()
                    .collect(toMap(Horario::getParticipante, Function.identity()));
            // Para cada hora de entrada, obtenemos los conductores disponibles
            Map<Integer, Set<Participante>> entradaConductor = horariosDia.stream().filter(Horario::isCoche)
                    .collect(groupingBy(Horario::getEntrada, mapping(Horario::getParticipante, toSet())));
            // Para comprobar, vemos los participantes, sus entradas y salidas
            Map<Integer, Long> entradaParticipante = horariosDia.stream()
                    .collect(groupingBy(Horario::getEntrada, mapping(Horario::getParticipante, counting())));
            Map<Integer, Long> salidaParticipante = horariosDia.stream()
                    .collect(groupingBy(Horario::getSalida, mapping(Horario::getParticipante, counting())));
            // Generamos todas las posibilidades y a ver cuales sirven...
            List<Iterable<Set<Participante>>> conductoresDia = entradaConductor.keySet().stream()
                    .map(key -> new SubSets<>(entradaConductor.get(key), 1, entradaConductor.get(key).size())).collect(toList());
            Combinador<Set<Participante>> combinarConductoresDia = new Combinador<>(conductoresDia);
            for (List<Set<Participante>> lp : combinarConductoresDia) {
                // Validando que hay plazas suficientes sin tener en cuenta puntos de encuentro
                boolean validada = true;
                Map<Integer, Integer> plazasEntrada = lp.stream().flatMap(e -> e.stream())
                        .map(e -> participanteHorario.get(e)).collect(
                                groupingBy(Horario::getEntrada, summingInt(h -> h.getParticipante().getPlazasCoche())));
                Map<Integer, Integer> plazasSalida = lp.stream().flatMap(e -> e.stream())
                        .map(e -> participanteHorario.get(e))
                        .collect(groupingBy(Horario::getSalida, summingInt(h -> h.getParticipante().getPlazasCoche())));

                for (Entry<Integer, Long> e : entradaParticipante.entrySet()) {
                    if (plazasEntrada.getOrDefault(e.getKey(), 0) < e.getValue().intValue()) {
                        validada = false;
                        break;
                    }
                }
                for (Entry<Integer, Long> e : salidaParticipante.entrySet()) {
                    if (plazasSalida.getOrDefault(e.getKey(), 0) < e.getValue().intValue()) {
                        validada = false;
                        break;
                    }
                }
                if (validada) {
                    // Tenemos una solución prometedora
                    // Eliminamos exceso de info
                    final Set<Participante> solVal = lp.stream().flatMap(e -> e.stream()).collect(toSet());
                    solucionesDia.add(solVal);
                }
            }
            if (solucionesDia.size() > 0) {
                solucionesCandidatas.put(d, solucionesDia);
            } else {
                // ¡¡¡Sin solución!!!
                solucionesCandidatas = Collections.emptyMap();
                break;
            }
        }

        tamanosNivel = solucionesCandidatas.keySet().stream().mapToInt(k -> solucionesCandidatas.get(k).size()).toArray();
        totalPosiblesSoluciones = IntStream.of(tamanosNivel).mapToDouble(i -> (double) i).reduce(1.0, (a, b) -> a * b);
        System.out.println("Nº de posibles soluciones: " + IntStream.of(tamanosNivel).mapToObj(Double::toString).collect(Collectors.joining(" * ")) + " = "
                + totalPosiblesSoluciones);
        double acum = 1.0;
        nPosiblesSoluciones = new double[tamanosNivel.length];
        for (int i = tamanosNivel.length - 1; i >= 0; i--) {
            nPosiblesSoluciones[i] = acum;
            acum = acum * tamanosNivel[i];
        }
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios) {
        this.horarios = horarios;
        estadisticas.inicia();
        inicializa();
        estadisticas.setTotalPosiblesSoluciones(totalPosiblesSoluciones);
        // Preparamos el algoritmo
        Nodo actual = new Nodo();
        Nodo mejor = actual;
        double C = actual.getCotaSuperior();
        PriorityQueue<Nodo> listaNodosVivos = new PriorityQueue<>();
        listaNodosVivos.offer(actual);

        // Bucle principal
        do {
            actual = listaNodosVivos.poll();
            if (estadisticas.incExpandidos() % CADA_EXPANDIDOS == 0L) {
                    System.out.format("> LNV=%,d, ", listaNodosVivos.size());
                    System.out.println(estadisticas.setFitness((int) (C * 1000)).actualizaProgreso());
                    System.out.println("-- Trabajando con " + actual);
            }
            if (actual.getCotaInferior() < C) { //Estrategia de poda: si la cotaInferior >= C no seguimos
                estadisticas.addGenerados(tamanosNivel[actual.getIndiceDia() + 1]);
                if (actual.getIndiceDia() + 2 == dias.length) { // Los hijos son terminales
                    estadisticas.addTerminales(tamanosNivel[actual.getIndiceDia() + 1]);
                    Optional<Nodo> mejorHijo = actual.generaHijos(true).min(Nodo::compareTo);
                    if (mejorHijo.isPresent() && mejorHijo.get().compareTo(mejor) < 0) {
                        mejor = mejorHijo.get();
                        System.out.println("* Mejoramos: " + mejor);
                        C = mejor.getCosteEstimado();
                    }
                } else { // Es un nodo intermedio
                    final double Corte = C;
                    List<Nodo> lNF = actual.generaHijos(true).filter(n -> n.getCotaInferior() < Corte).collect(toList());
                    OptionalDouble menorCotaSuperior = lNF.stream().mapToDouble(Nodo::getCotaSuperior).min();
                    if (menorCotaSuperior.isPresent() && menorCotaSuperior.getAsDouble() < C) { // Mejora de C
                        System.out.println("# Nuevo corte: Anterior=" + C + ", Nuevo=" + menorCotaSuperior.getAsDouble());
                        C = menorCotaSuperior.getAsDouble(); //Actualizamos C
                        final double nuevoCorte = C;
                        lNF = lNF.stream().filter(n -> n.getCotaInferior() < nuevoCorte).collect(toList()); //Recalculamos lNF
                    }
                    estadisticas.addDescartados((tamanosNivel[actual.getIndiceDia() + 1] - lNF.size()) * nPosiblesSoluciones[actual.getIndiceDia() + 1]);
                    listaNodosVivos.addAll(lNF);
                }
            } else {
                estadisticas.addDescartados(nPosiblesSoluciones[actual.getIndiceDia()]);
            }
        } while (!listaNodosVivos.isEmpty());

        //Estadisticas finales
        estadisticas.setFitness((int) (C * 1000)).actualizaProgreso();

        System.out.println("====================");
        System.out.println("Estadísticas finales");
        System.out.println("====================");
        System.out.println(estadisticas);
        System.out.println("----------------------------------------------------");

        // Construimos la solución final
        if (mejor.getCosteEstimado() != Double.POSITIVE_INFINITY) {
            Iterator<Set<Participante>> i = mejor.getEleccion().iterator();
            solucionFinal = Stream.of(dias).collect(toMap(Function.identity(), 
                    d -> new AsignacionDiaV1(i.next(), null, null, 0)));
        }

        return solucionFinal;
    }

    @Override
    public Map<Dia, ? extends AsignacionDia> getSolucionFinal() {
        return solucionFinal;
    }

    //Function<Nodo, Double> funcionCoste;
    final private class Nodo implements Comparable<Nodo> {

        private final List<Set<Participante>> eleccion;
        private final int vecesConductor[];
        private final int indiceDia;
        private double costeEstimado;
        private double cotaInferior;
        private double cotaSuperior;

        Nodo() {
            eleccion = Collections.emptyList();
            vecesConductor = new int[participantes.length];
            cotaInferior = 0;
            costeEstimado = cotaSuperior = Double.POSITIVE_INFINITY;
            indiceDia = -1;
        }

        Nodo(Nodo padre, Set<Participante> nuevaAsignacion) {
            indiceDia = padre.indiceDia + 1;
            eleccion = new ArrayList<>(padre.eleccion.size() + 1);
            eleccion.addAll(padre.eleccion);
            eleccion.add(nuevaAsignacion);
            vecesConductor = Arrays.copyOf(padre.vecesConductor, padre.vecesConductor.length);
            // Ahora actualizamos las veces que es conductor cada participantes
            for (int i = 0; i < participantes.length; i++) {
                if (nuevaAsignacion.contains(participantes[i])) {
                    vecesConductor[i]++;
                }
            }
            calculaCostes();
        }

        /**
         * Esta función calcula las cotas superior e inferior y el coste
         * estimado del nodo
         */
        private void calculaCostes() {
            IntSummaryStatistics estadisticas = IntStream.of(vecesConductor).summaryStatistics();
            if (this.isTerminal()) {
                costeEstimado = cotaSuperior = cotaInferior = 2 * estadisticas.getMax() - estadisticas.getMin();
            } else {
                int minASumar = (int) ((dias.length - 1 - indiceDia) - (estadisticas.getMax() * vecesConductor.length - estadisticas.getSum()));
                int max = estadisticas.getMax() + (minASumar > 0 ? 1 + minASumar / vecesConductor.length : 0);
                cotaInferior = 2 * max - Math.min(max, (estadisticas.getMin() + (dias.length - 1 - indiceDia)));
                cotaSuperior = 2 * (estadisticas.getMax() + (dias.length - 1 - indiceDia)) - estadisticas.getMin();
                //costeEstimado = cotaInferior + (estadisticas.getAverage()/ (indiceDia+1))*(dias.length - 1 - indiceDia);
                costeEstimado = (cotaSuperior + cotaInferior) / 2;
            }
        }

        double getCotaSuperior() {
            return cotaSuperior;
        }

        double getCotaInferior() {
            return cotaInferior;
        }

        double getCosteEstimado() {
            return costeEstimado;
        }

        List<Set<Participante>> getEleccion() {
            return eleccion;
        }

        int getIndiceDia() {
            return indiceDia;
        }

        Stream<Nodo> generaHijos(boolean paralelo) {
            return (paralelo ? solucionesCandidatas.get(dias[indiceDia + 1]).parallelStream()
                    : solucionesCandidatas.get(dias[indiceDia + 1]).stream()).map((solDia) -> new Nodo(this, solDia));
        }

        boolean isTerminal() {
            return dias.length == indiceDia + 1;
        }

        @Override
        public int compareTo(Nodo o) {
            return Double.compare(costeEstimado, o.costeEstimado);
            //return Double.compare(o.costeEstimado, costeEstimado);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + Objects.hashCode(this.eleccion);
            hash = 23 * hash + this.indiceDia;
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
            final Nodo other = (Nodo) obj;
            if (this.indiceDia != other.indiceDia) {
                return false;
            }
            return Objects.equals(this.eleccion, other.eleccion);
        }

        @Override
        public String toString() {
            return "Nodo{" + "nivel=" + indiceDia + ", estimado=" + costeEstimado + ", inferior=" + cotaInferior + ", superior=" + cotaSuperior + ", eleccion=" + eleccion + '}';
        }
    }
}
