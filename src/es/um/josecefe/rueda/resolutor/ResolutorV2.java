/**
 *
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.Participante;
import es.um.josecefe.rueda.modelo.AsignacionDiaV1;
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
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.stream.DoubleStream;
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
public class ResolutorV2 extends Resolutor {

    final static long CADA_GENERADOS = 10000000L;
    final static long CADA_EXPANDIDOS = 100000L;
    private Set<Horario> horarios;
    private Dia[] dias;
    private Participante[] participantes;
    private Map<Dia, List<Set<Participante>>> solucionesCandidatas;
    private Map<Dia, AsignacionDiaV1> solucionFinal;
    private double[] tamanosNivel;
    private double totalNodos;
    private EstadisticasV1 estadisticas = new EstadisticasV1();

    @Override
    public Estadisticas getEstadisticas() {
        return estadisticas;
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

            cotaInferior = Math.max(estadisticas.getMax(), 2 * estadisticas.getMax() - (estadisticas.getMin() + (dias.length - 1 - indiceDia)));
            costeEstimado = cotaSuperior = 2 * (estadisticas.getMax() + (dias.length - 1 - indiceDia)) - estadisticas.getMin();
            //cotaSuperior = cotaInferior + (dias.length - indiceDia - 1);
            //costeEstimado = 2 * (estadisticas.getMax() + (dias.length - 1 - indiceDia) / 2.0) - estadisticas.getMin();
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

        List<Nodo> generaHijos() {
            if (esTerminal()) { //Es un nodo terminal
                return Collections.emptyList();
            }
            return solucionesCandidatas.get(dias[indiceDia + 1]).parallelStream().map((solDia) -> new Nodo(this, solDia)).collect(toList());
        }

        boolean esTerminal() {
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
            return "Nodo{" + "indiceDia=" + indiceDia + ", costeEstimado=" + costeEstimado + ", cotaInferior=" + cotaInferior + ", cotaSuperior=" + cotaSuperior + ", eleccion=" + eleccion + '}';
        }
    }

    public ResolutorV2(){
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
                if (validada) { // Tenemos una solución prometedora
                    // Eliminamos exceso de info
                    solucionesDia.add(lp.stream().flatMap(e -> e.stream()).collect(toSet()));
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

        tamanosNivel = solucionesCandidatas.keySet().stream().mapToDouble(k -> Double.valueOf(solucionesCandidatas.get(k).size())).toArray();
        totalNodos = DoubleStream.of(tamanosNivel).reduce(1.0, (a, b) -> a * b);
        System.out.println("Potencialmente vamos a comprobar " + DoubleStream.of(tamanosNivel).mapToObj(Double::toString).collect(Collectors.joining(" * ")) + " = "
                + totalNodos);
        double acum = 1.0, temp;
        for (int i = tamanosNivel.length - 1; i >= 0; i--) {
            temp = tamanosNivel[i];
            tamanosNivel[i] = acum;
            acum = acum * temp;
        }
    }

    @Override
    public Map<Dia, AsignacionDiaV1> resolver(Set<Horario> horarios) {
        estadisticas.inicia();
        this.horarios = horarios;
        inicializa();
        estadisticas.setTotalPosiblesSoluciones(totalNodos);
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
                for (Nodo n : actual.generaHijos()) { // TODO: ¿Y si convertimos la función para devuelva un stream y esto es un foreach?
                    //System.out.println("--> Hijo: "+n);
                    if (n.esTerminal()) { //Se trata de una solución completa, hay que valorarla
                        estadisticas.addTerminales(1); // Los terminales los tratamos inmediatamente
                        if (n.getCosteEstimado() < mejor.getCosteEstimado()) { //Mejoramos
                            mejor = n;
                            System.out.println("* Mejoramos: " + mejor);
                            System.out.println("** Por nueva solución, nuevo corte: Anterior=" + C + ", Nuevo=" + mejor.getCosteEstimado());
                            C = mejor.getCosteEstimado();
                        }
                    } else //Es un nodo intermedio
                    {
                        estadisticas.addGenerados(1);
                        if (n.getCotaInferior() < C) {
                            //System.out.println("<-- Entro a LNV");
                            listaNodosVivos.offer(n);
                            if (n.getCotaSuperior() < C) {
                                System.out.println("# Nuevo corte: Anterior=" + C + ", Nuevo=" + n.getCotaSuperior());
                                C = n.getCotaSuperior(); //Actualizamos C si es necesario
                            }
                        } else {
                            estadisticas.addDescartados(tamanosNivel[n.getIndiceDia()]);
                        }
                    }
                }
            } else {
                estadisticas.addDescartados(tamanosNivel[actual.getIndiceDia()]);
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
    public Map<Dia, AsignacionDiaV1> getSolucionFinal() {
        return solucionFinal;
    }
}
