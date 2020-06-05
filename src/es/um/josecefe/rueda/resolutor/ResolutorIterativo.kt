/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.resolutor

import es.um.josecefe.rueda.modelo.AsignacionDia
import es.um.josecefe.rueda.modelo.Dia
import es.um.josecefe.rueda.modelo.Horario

/**
 * @author josecefe
 */
class ResolutorIterativo : ResolutorAcotado {

    private val resolutor: ResolutorAcotado
    private var solucion: Map<Dia, AsignacionDia> = emptyMap()

    constructor() {
        resolutor = if (Runtime.getRuntime().availableProcessors() >= 4) ResolutorV8() else ResolutorV7()
    }

    constructor(resolutor: ResolutorAcotado) {
        this.resolutor = resolutor
    }

    fun resolver(horarios: Set<Horario>, cotaCorteTope: Int, cotaCorteBase: Int): Map<Dia, AsignacionDia> {
        solucion = emptyMap()
        var corte = cotaCorteBase // Punto de partida
        while ((solucion.isEmpty()) && corte < cotaCorteTope) { // Valor inicial
            solucion = resolutor.resolver(horarios, corte)
            corte += PESO_MAXIMO_VECES_CONDUCTOR
        }
        if (solucion.isEmpty()) {
            solucion = resolutor.resolver(horarios, cotaCorteTope)
        }
        return solucion
    }

    override fun resolver(horarios: Set<Horario>, cotaInfCorte: Int): Map<Dia, AsignacionDia> {
        val numDias = horarios.map{ it.dia }.distinct().count()
        val numParticipantes = horarios.map{ it.participante}.distinct().count()
        val conductores = horarios.filter { it.coche }.map { it.participante }.distinct().sorted().toTypedArray()
        val tamMedioCoche = conductores.map{ it.plazasCoche }.average()
        val cotaCorteBase = (numParticipantes / tamMedioCoche * numDias / conductores.size + 1).toInt() * PESO_MAXIMO_VECES_CONDUCTOR - 1
        return resolver(horarios, cotaInfCorte, cotaCorteBase)
    }

    override fun resolver(horarios: Set<Horario>): Map<Dia, AsignacionDia> {
        return resolver(horarios, (horarios.map{ it.dia}.distinct().count() + 1) * PESO_MAXIMO_VECES_CONDUCTOR - 1)
    }

    override val estadisticas: Estadisticas
        get() = resolutor.estadisticas

    override val solucionFinal: Map<Dia, AsignacionDia>
        get() = solucion

    override var estrategia: Estrategia
        get() = resolutor.estrategia
        set(estrategia) {
            resolutor.estrategia = estrategia
        }

    override fun parar() {
        super.parar()
        resolutor.parar()
    }
}
