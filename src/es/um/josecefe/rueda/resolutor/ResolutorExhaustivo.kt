package es.um.josecefe.rueda.resolutor

import es.um.josecefe.rueda.modelo.*
import es.um.josecefe.rueda.resolutor.Pesos.PESO_MAXIMO_VECES_CONDUCTOR
import es.um.josecefe.rueda.util.combinations
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class ResolutorExhaustivo : Resolutor() {
    private val DEBUG = true

    private val ESTADISTICAS = true
    private val CADA_EXPANDIDOS_EST = 100000

    private val estGlobal = EstadisticasV7()
    private var solucionFinal: Map<Dia, AsignacionDia>? = null
    private var totalPosiblesSoluciones = 0.0
    private val mapParticipanteDias = LinkedHashMap<Participante, Set<Dia>>()
    //private val coefCorrecionCond = HashMap<Participante, Double>()
    private val diasFijos: HashMap<Participante, MutableSet<Dia>> = HashMap<Participante, MutableSet<Dia>>()
    private var minCond = 1

    override fun resolver(horarios: Set<Horario>): Map<Dia, AsignacionDia>? {
        if (horarios.isEmpty()) {
            return emptyMap()
        }

        if (ESTADISTICAS) {
            estGlobal.inicia()
        }

        /* Inicialización */
        continuar = true

        val contexto = ContextoResolucionSimple(horarios) //TODO: Importar aquí las partes interesantes de contexto.inicializa

        diasFijos.clear()
        for ((dia, solDia) in contexto.solucionesCandidatas){
            val conductores = contexto.participantes.toMutableSet()
            for (asignacion in solDia) {
                conductores.retainAll(asignacion.conductores)
            }
            for (c in conductores) {
                diasFijos.computeIfAbsent(c, {HashSet()}).add(dia)
            }
        }
        minCond = Math.max(1, diasFijos.values.map { it.size }.max()?.toInt() ?: 0)

        mapParticipanteDias.clear()
        for (i in contexto.participantes.indices) {
            if (contexto.participantesConCoche[i]) {
                val p = contexto.participantes[i]
                val setDias = horarios.filter { h -> h.isCoche && h.participante == p }.map { h -> h.dia }.toSet()
                mapParticipanteDias[p] = setDias
            }
        }

        solucionFinal = null
        val vacio = emptySet<Dia>().toMutableSet()

        if (ESTADISTICAS) {
            totalPosiblesSoluciones = 0.0
            for (i in minCond..contexto.dias.size) {
                var comb = 1.0
                if (DEBUG) print("Para $i veces conducir por participante tenemos ")
                for ((key, value) in mapParticipanteDias) {
                    val numVecesCond = Math.max(1, Math.round(i.toDouble() / contexto.coefConduccion[key]!!)) //Para minorar adecuadamente el valor de i usando el coeficiente de conductor
                    val porDiasFijos = diasFijos.getOrDefault(key, vacio).size.toLong()
                    if (porDiasFijos >= numVecesCond) continue
                    val combinations = combinations(Math.max(1, value.size.toLong() - porDiasFijos), numVecesCond - porDiasFijos)
                    if (DEBUG) print(" * $combinations")
                    comb *= combinations
                }
                if (DEBUG) {
                    println(" = $comb")
                }
                totalPosiblesSoluciones += comb
            }
            if (DEBUG) {
                println("Nº total de posibles soluciones: $totalPosiblesSoluciones")
            }
        }
        /* Fin inicialización */

        if (ESTADISTICAS) {
            estGlobal.setTotalPosiblesSoluciones(totalPosiblesSoluciones)
            estGlobal.actualizaProgreso()
            if (DEBUG) {
                println("Tiempo inicializar = ${estGlobal.tiempoString}")
            }
        }

        var mejorCoste = Int.MAX_VALUE

        //Vamos a ir buscando soluciones de menos veces al tope de veces
        bucledias@ for (i in minCond..contexto.dias.size) {
            if (mejorCoste<Int.MAX_VALUE) break // Hemos terminado

            if (DEBUG) {
                println("*** Probando soluciones de nivel $i...")
            }
            if (!continuar) break
            val listSubcon: MutableList<Iterable<Set<Dia>>> = ArrayList(mapParticipanteDias.size)
            for ((key, value) in mapParticipanteDias) {
                val numVecesCond = Math.max(1, Math.round(i.toDouble() / contexto.coefConduccion[key]!!).toInt()) - diasFijos.getOrDefault(key, emptySet<Dia>().toMutableSet()).size //Para minorar adecuadamente el valor de i usando el coeficiente de conductor
                val diasCambiantes: Set<Dia> = if (diasFijos[key] != null) value.minus(diasFijos[key]!!.asSequence()) else value
                listSubcon.add(SubSets(diasCambiantes, numVecesCond, numVecesCond).map { if (diasFijos[key]!= null) diasFijos[key]!!.plus(it) else it})
            }
            val combinaciones = Combinador(listSubcon)

            //TODO: Habría que ver las implicaciones del paralelismos, especialmente en las variables compartidas
            combinaciones.stream().parallel().forEach {c ->
                if (!continuar) return@forEach
                val asignacion = HashMap<Dia, HashSet<Participante>>()

                val participaIt = mapParticipanteDias.keys.iterator()
                for (e in c) {
                    val p = participaIt.next()
                    e.map { asignacion.computeIfAbsent(it) { HashSet() } }.forEach { it.add(p) }
                }
                if (asignacion.size < contexto.solucionesCandidatas.size) {
                    if (ESTADISTICAS)
                        estGlobal.descartados++
                    return@forEach
                }
                val solCand = HashMap<Dia, AsignacionDia>()
                for ((key, value) in asignacion) {
                    for (s in contexto.solucionesCandidatas[key]!!) {
                        if (s.conductores == value) {
                            // Encontrada la solucion correspondiente
                            solCand[key] = s
                            break
                        }
                    }
                }

                // Para ver si es valida bastaria con ver si hay solución en cada día
                if (solCand.size == contexto.solucionesCandidatas.size && contexto.dias.all { solCand[it] != null }) {
                    val apt = calculaAptitud(solCand, i)
                    if (apt < mejorCoste) {
                        if (DEBUG) {
                            println("---> Encontrada una mejora: Coste anterior = $mejorCoste, nuevo coste = $apt, sol = $solCand")
                        }
                        mejorCoste = apt
                        solucionFinal = solCand
                        if (ESTADISTICAS) {
                            estGlobal.setFitness(mejorCoste).actualizaProgreso()
                        }
                    }
                }
                if (ESTADISTICAS && estGlobal.incExpandidos() % CADA_EXPANDIDOS_EST == 0L) {
                    estGlobal.terminales=estGlobal.expandidos.toDouble()
                    estGlobal.actualizaProgreso()
                    if (DEBUG) {
                        println(estGlobal)
                    }
                }
            }
        }

        //Estadisticas finales
        if (ESTADISTICAS) {
            estGlobal.setFitness(mejorCoste)
            estGlobal.actualizaProgreso()
            if (DEBUG) {
                println("====================")
                println("Estadísticas finales")
                println("====================")
                println(estGlobal)
                println("Solución final=$solucionFinal")
                println("-----------------------------------------------")
            }
        }

        return solucionFinal
    }

    private fun calculaAptitud(sol: Map<Dia, AsignacionDia>, vecesCond: Int) = vecesCond * PESO_MAXIMO_VECES_CONDUCTOR + sol.values.map { it.coste }.sum()

    override fun getSolucionFinal(): Map<Dia, AsignacionDia>? {
        return solucionFinal
    }

    override fun getEstadisticas(): Estadisticas {
        return estGlobal
    }

    override fun setEstrategia(estrategia: Resolutor.Estrategia) {
        //Nada
    }

}
