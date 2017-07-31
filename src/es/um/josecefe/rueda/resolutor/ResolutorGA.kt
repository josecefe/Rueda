/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import es.um.josecefe.rueda.modelo.*
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.Collectors
import java.util.stream.Collectors.*
import java.util.stream.IntStream
import java.util.stream.Stream

/**
 * Implementa la resolución del problema de la Rueda mediante el empleo de un
 * algoritmo genético.
 *
 * @author josecefe
 */
class ResolutorGA(var tamPoblacion: Int = TAM_POBLACION_DEF, var probMutacion: Double = PROB_MUTACION_DEF, var numGeneraciones: Int = N_GENERACIONES_DEF, var tiempoMaximo: Long = TIEMPO_MAXIMO_DEF.toLong(), val objAptitud: Int = OBJ_APTITUD_DEF, val tournamentSize: Int = TAM_TORNEO_DEF, val tamElite: Int = TAM_ELITE_DEF, val tamExtranjero: Int = TAM_EXTRANJERO_DEF, var maxEstancado: Int = MAX_ESTANCADO_DEF) : Resolutor() {
    override var estrategia
        get() = Resolutor.Estrategia.EQUILIBRADO
        set(value) { /* Nada */}

    private var horarios: Set<Horario>? = null
    private var dias: List<Dia>? = null
    private var participantes: List<Participante>? = null
    private var coefConduccion: FloatArray? = null
    private var solCandidatasDiarias: Map<Dia, List<AsignacionDiaV5>>? = null
    override var solucionFinal: Map<Dia, AsignacionDia> = emptyMap()
    private var participantesConCoche: BooleanArray? = null
    private val estGlobal = EstadisticasGA()

    private fun inicializa() {
        continuar = true
        dias = horarios!!.map{ it.dia }.filterNotNull().toSet().toList()
        participantes = horarios!!.map{ it.participante }.filterNotNull().toSet().toList()
        participantesConCoche = BooleanArray(participantes!!.size)
        for (i in participantes!!.indices) {
            participantesConCoche!![i] = participantes!![i].plazasCoche > 0
        }

        val vecesParticipa = horarios!!.groupingBy{ it.participante!! }.eachCount()
        val maxVecesParticipa = vecesParticipa.values.max() ?: 0

        coefConduccion = FloatArray(participantes!!.size)
        for (i in coefConduccion!!.indices) {
            coefConduccion!![i] = maxVecesParticipa.toFloat() / vecesParticipa[participantes!![i]]!!.toFloat() - 0.001f // Restamos 0.001 para evitar que al redondear ciertos casos se desmadren
        }
        solucionFinal = emptyMap()

        // Vamos a trabajar día a día
        solCandidatasDiarias = IntStream.range(0, dias!!.size).parallel().boxed().collect(toConcurrentMap({ ind -> dias!![ind] }) { indDia ->
            //for (int indDia = 0; indDia < dias.length; indDia++) {
            val d = dias!![indDia]
            val solucionesDia = ArrayList<AsignacionDiaV5>()
            val horariosDia = horarios!!.filter { it.dia == d }.toSet()
            val participanteHorario = horariosDia.associate{Pair(it.participante!!, it)  }
            val participantesDia = horariosDia.map{ it.participante!! }.sortedBy { it.nombre }

            // Para cada hora de entrada, obtenemos los conductores disponibles
            val entradaConductor = horariosDia.filter{ it.coche }.groupBy ({ it.entrada}, {it.participante})
            // Para comprobar, vemos los participantes, sus entradas y salidas
            val nParticipantesIda = horariosDia.groupingBy { it.entrada }.eachCount()

            val nParticipantesVuelta = horariosDia.groupingBy { it.salida }.eachCount()
            // Generamos todas las posibilidades y a ver cuales sirven...
            val conductoresDia = entradaConductor.keys.map { key -> SubSets(entradaConductor[key]!!, 1, entradaConductor[key]!!.size) }
            val combinarConductoresDia = Combinador(conductoresDia)

            for (condDia in combinarConductoresDia) {
                val selCond = condDia.flatMap { it }.filterNotNull().toSet()
                // Validando que hay plazas suficientes sin tener en cuenta puntos de encuentro

                val plazasIda = selCond.map { participanteHorario[it] }.groupBy{ it!!.entrada }.mapValues { it.value.sumBy { it!!.participante!!.plazasCoche }}
                val plazasVuelta = selCond.map { participanteHorario[it] }.groupBy { it!!.salida }.mapValues { it.value.sumBy { it!!.participante!!.plazasCoche }}

                if (nParticipantesIda.entries.stream().allMatch { e -> plazasIda.getOrDefault(e.key, 0) >= e.value } && nParticipantesVuelta.entries.stream().allMatch { e -> plazasVuelta.getOrDefault(e.key, 0) >= e.value }) {

                    // Obtenemos la lista de posibles lugares teniendo en cuenta quien es el conductor
                    val posiblesLugares = participantesDia.map{ it.puntosEncuentro }.plus(selCond.map{ it.puntosEncuentro })

                    var mejorCoste = Integer.MAX_VALUE
                    var mejorLugaresIda: Map<Participante, Lugar>? = null
                    var mejorLugaresVuelta: Map<Participante, Lugar>? = null

                    for (selLugares in Combinador(posiblesLugares)) {
                        val lugaresIda: MutableMap<Participante, Lugar>
                        val lugaresVuelta: MutableMap<Participante, Lugar>
                        lugaresIda = HashMap()
                        lugaresVuelta = HashMap()
                        val il = selLugares.subList(participantesDia.size, selLugares.size).iterator()
                        for (i in participantesDia.indices) {
                            lugaresIda.put(participantesDia[i], selLugares[i])
                            lugaresVuelta.put(participantesDia[i], if (selCond.contains(participantesDia[i]))
                                il.next()
                            else
                                selLugares[i])
                        }
                        val plazasDisponiblesIda = selCond.stream()
                                .collect(groupingBy({ p -> participanteHorario[p]!!.entrada }, groupingBy({ lugaresIda[it] }, summingInt({ it.plazasCoche }))))

                        val plazasDisponiblesVuelta = selCond.stream()
                                .collect(groupingBy({ p -> participanteHorario[p]!!.salida }, groupingBy({ lugaresVuelta[it]!! }, summingInt({ it.plazasCoche }))))
                        // Para comprobar, vemos los participantes, sus entradas y salidas
                        val plazasNecesariasIda = horariosDia.groupBy{ it.entrada }.mapValues { it.value.groupingBy { lugaresIda[it.participante]!! }.eachCount() }

                        val plazasNecesariasVuelta = horariosDia.groupBy{ it.salida }.mapValues { it.value.groupingBy { lugaresVuelta[it.participante]!! }.eachCount() }

                        if (plazasNecesariasIda.entries.stream().allMatch { e -> e.value.entries.stream().allMatch { ll -> ll.value <= plazasDisponiblesIda[e.key]!!.getOrDefault(ll.key, 0) } } && plazasNecesariasVuelta.entries.stream().allMatch { e -> e.value.entries.stream().allMatch { ll -> ll.value <= plazasDisponiblesVuelta[e.key]!!.getOrDefault(ll.key, 0) } }) {
                            // Calculamos coste
                            val coste = participantesDia.stream().mapToInt { e -> e!!.puntosEncuentro.indexOf(lugaresIda[e]) + e.puntosEncuentro.indexOf(lugaresVuelta[e]) }.sum()

                            if (coste < mejorCoste) {
                                mejorCoste = coste
                                mejorLugaresIda = lugaresIda
                                mejorLugaresVuelta = lugaresVuelta
                            }
                        }
                    }
                    if (mejorCoste < Integer.MAX_VALUE) {
                        // Tenemos una solución válida
                        solucionesDia.add(AsignacionDiaV5(participantes!!.toTypedArray(), selCond, mejorLugaresIda!!, mejorLugaresVuelta!!, mejorCoste))
                    }
                }
            }
            solucionesDia
        })

        if (ESTADISTICAS) {
            val tamanosNivel = solCandidatasDiarias!!.keys.stream().mapToInt { k -> solCandidatasDiarias!![k]!!.size }.toArray()
            val totalPosiblesSoluciones = IntStream.of(*tamanosNivel).mapToDouble { i -> i.toDouble() }.reduce(1.0) { a, b -> a * b }
            if (DEBUG) {
                println("Nº de posibles soluciones: " + IntStream.of(*tamanosNivel).mapToObj{ java.lang.Double.toString(it.toDouble()) }.collect(Collectors.joining(" * ")) + " = "
                        + totalPosiblesSoluciones)
            }
        }
    }

    private fun tournamentSelection(pop: List<Individuo>, tamTorneo: Int): Individuo? {
        // Create a tournament population
        var ind: Individuo
        var mejor: Individuo? = null
        var mejorAdaptacion = Integer.MAX_VALUE
        // For each place in the tournament get a random individual
        for (i in 0..tamTorneo - 1) {
            ind = pop[ThreadLocalRandom.current().nextInt(pop.size)]
            if (ind.aptitud < mejorAdaptacion) {
                mejorAdaptacion = ind.aptitud
                mejor = ind
            }
        }
        // Get the fittest
        return mejor
    }

    override fun resolver(horarios: Set<Horario>): Map<Dia, AsignacionDia> {
        this.horarios = horarios
        if (horarios.isEmpty()) {
            return emptyMap()
        }

        if (ESTADISTICAS) {
            estGlobal.inicia()
        }

        inicializa()

        if (ESTADISTICAS) {
            estGlobal.numGeneraciones = numGeneraciones
            estGlobal.actualizaProgreso()
            if (DEBUG) {
                System.out.format("Tiempo inicializar =%s\n", estGlobal.tiempoString)
            }
        }

        var poblacion: MutableList<Individuo> = Stream.generate{ Individuo() }.limit(tamPoblacion.toLong()).collect(toList())
        var mejor = poblacion.stream().min { obj, other -> obj.compareTo(other) }.orElseGet{ Individuo() }

        if (DEBUG) {
            println("Mejor Población Inicial: " + mejor)
            println("Est. Población Inicial: " + poblacion.stream().mapToInt{ it.aptitud }.summaryStatistics())
        }
        var nGen = 0
        var estancado = 0
        //AHORA VA EL ESQUEMA DEL GA
        val ti = System.currentTimeMillis()
        while (System.currentTimeMillis() - ti < tiempoMaximo && nGen++ < numGeneraciones && mejor.aptitud > objAptitud && continuar) {
            // Paso 1 y 2: Seleccionar padres, combinar y después mutar...
            var elite: List<Individuo>? = null
            if (tamElite > 1) {
                Collections.sort(poblacion) // Ordenacion natural
                elite = poblacion.subList(0, tamElite) // Creamos el grupo de elite
            }
            val antPoblacion = poblacion
            poblacion = poblacion.parallelStream().map { ind -> ind.cruze(tournamentSelection(antPoblacion, tournamentSize)) }.map { ind -> ind.mutacion(probMutacion) }.collect(toList())

            if (ESTADISTICAS) {
                estGlobal.incGeneracion()
                estGlobal.addGenerados(poblacion.size.toDouble())
            }

            if (tamElite > 0) {
                poblacion = poblacion.subList(0, tamPoblacion) // Para controlar el tamaño de la población
            }

            // Añadimos el mejor que teníamos hasta ahora a la nueva población
            if (tamElite > 0) {
                if (tamElite == 1) {
                    poblacion.add(mejor)
                } else {
                    if (elite != null) {
                        poblacion.addAll(elite)
                    }
                }
            }

            if (tamExtranjero > 0) {
                poblacion.addAll(Stream.generate{ Individuo() }.limit(tamExtranjero.toLong()).collect(toList()))
            }
            // Paso 3: Elegir al mejor y actualizar estadisticas
            val nuevoMejor = poblacion.stream().min { obj, other -> obj.compareTo(other) }.orElseGet{ Individuo() }

            if (nuevoMejor.compareTo(mejor) < 0) {
                mejor = nuevoMejor
                if (ESTADISTICAS) {
                    estGlobal.fitness = mejor.aptitud
                    estGlobal.actualizaProgreso()
                }
                estancado = 0
            } else if (++estancado >= maxEstancado) {
                if (DEBUG) {
                    System.out.format("******Estamos estancados durante %,d generaciones, vamos a regenerar la población conservando solo el mejor****\n", estancado)
                }
                poblacion = Stream.generate{ Individuo() }.limit((tamPoblacion - 1).toLong()).collect(toList())
                poblacion.add(mejor)
                maxEstancado += estancado // Subimos el umbral para no regenerar en exceso
                estancado = 0
            }

            if (ESTADISTICAS && nGen % IMP_CADA_CUANTAS_GEN == 0) {
                estGlobal.actualizaProgreso()
                if (DEBUG) {
                    System.out.format("Generación %d -> mejor=%s\n", nGen, mejor)
                    println(" - Est. población: " + poblacion.stream().mapToInt{ it.aptitud }.summaryStatistics())
                    println(" - Est. algoritmo:" + estGlobal)
                }
            }
        }

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.fitness = mejor.aptitud
            estGlobal.actualizaProgreso()
            if (DEBUG) {
                println("====================")
                println("Estadísticas finales")
                println("====================")
                println(estGlobal)
                println("Solución final=" + mejor)
                println("-----------------------------------------------")
            }
        }
        solucionFinal = mejor.genes
        return solucionFinal
    }

    override val estadisticas: Estadisticas
        get() = estGlobal

    private inner class Individuo : Comparable<Individuo> {
        val genes: Map<Dia, AsignacionDia>
        internal val aptitud: Int

        internal constructor() {
            genes = dias!!.groupBy { it }.mapValues { solCandidatasDiarias!![it.key]!!.get(ThreadLocalRandom.current().nextInt(solCandidatasDiarias!![it.key]!!.size))}
            aptitud = calculaAptitud(genes)
        }

        //Creamos un individuo con estos genes
        internal constructor(genes: Map<Dia, AsignacionDia>) {
            this.genes = genes
            aptitud = calculaAptitud(genes)
        }

        private fun calculaAptitud(genes: Map<Dia, AsignacionDia>): Int {
            val vecesCoche = genes.values.flatMap { it.conductores}.groupingBy { it }.eachCount()
            val est = IntStream.range(0, participantes!!.size).filter { i -> participantesConCoche!![i] }.map { i -> (vecesCoche.getOrDefault(participantes!![i], 0) * coefConduccion!![i] + 0.5f).toInt() }.summaryStatistics()
            var apt = est.max * PESO_MAXIMO_VECES_CONDUCTOR + (est.max - est.min) * PESO_DIF_MAX_MIN_VECES_CONDUCTOR + genes.values.stream().mapToInt{ it.coste }.sum()
            if (estrategia === Resolutor.Estrategia.MINCONDUCTORES)
                apt += est.sum.toInt() * PESO_TOTAL_CONDUCTORES

            return apt
        }

        /**
         * Crea un hijo como resultado del cruce de los genes de este individuo
         * y el otro padre
         *
         * @param otroPadre El otro individuo que sera padre de nuevo individuo,
         * aportando sus genes
         * @return nuevo individuo resultado del cruce de los genes de los
         * progenitores
         */
        internal fun cruze(otroPadre: Individuo?): Individuo {
            val genesHijo = dias!!.groupBy { it }.mapValues { if (ThreadLocalRandom.current().nextBoolean()) genes[it.key]!! else otroPadre!!.genes[it.key]!! }

            return Individuo(genesHijo)
        }

        /**
         * Crea una mutación a partir de este individuo
         *
         * @param probMutacionGen valor entre 0 y 1 indicando la probabilidad de
         * mutar de cada gen
         * @return un nuevo individuo mutado
         */
        internal fun mutacion(probMutacionGen: Double): Individuo {
            val genesHijo = dias!!.groupBy { it }.mapValues {
                if (ThreadLocalRandom.current().nextDouble() < probMutacionGen)
                    solCandidatasDiarias!![it.key]!!.get(ThreadLocalRandom.current().nextInt(solCandidatasDiarias!![it.key]!!.size))
                else
                    genes[it.key]!!
            }

            return Individuo(genesHijo)
        }

        override fun compareTo(other: Individuo) = Integer.compare(aptitud, other.aptitud)

        override fun hashCode() = 7 * 23  + Objects.hashCode(this.genes)

        override fun equals(other: Any?): Boolean = this === other || (other != null && other is Individuo && this.genes == other.genes)

        override fun toString() = String.format("Individuo{aptitud=%,d, genes=%s}", aptitud, genes)

    }

    companion object {
        private const val DEBUG = true
        private const val ESTADISTICAS = true
        private const val IMP_CADA_CUANTAS_GEN = 100

        private const val TAM_POBLACION_DEF = 1000
        private const val PROB_MUTACION_DEF = 0.015
        private const val TAM_TORNEO_DEF = 5
        private const val N_GENERACIONES_DEF = 1000
        private const val TIEMPO_MAXIMO_DEF = Integer.MAX_VALUE
        private const val OBJ_APTITUD_DEF = 0
        private const val TAM_ELITE_DEF = 5
        private const val TAM_EXTRANJERO_DEF = TAM_ELITE_DEF
        private const val MAX_ESTANCADO_DEF = N_GENERACIONES_DEF / 10
    }
}
