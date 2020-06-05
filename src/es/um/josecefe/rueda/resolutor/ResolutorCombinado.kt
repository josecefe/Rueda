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
 * Clase ResolutorCombinado. Es un resolutor que se basa en dos, uno primero
 * tipicamente de tipo no exhaustivo que da una solución buena pero normalmente
 * no optima y un segundo de tipo exhaustivo, que tomando como base la solución
 * del primero para realizar descartes en el arbol de soluciones, intenta dar
 * una solución óptima en un tiempo de ejecución mejor que si se ejecutarse a
 * ciegas
 *
 * @author josec
 */
class ResolutorCombinado : Resolutor {
    override val estadisticas: EstadisticasCombinado

    private var primero: Resolutor

    private var segundo: ResolutorAcotado

    private var solucion: Map<Dia, AsignacionDia> = emptyMap()

    /**
     * Crea un Resolutor combinando un ResolutorGA como resolutor no exhaustivo
     * y un ResolutorV8 como resolutor acotado que tomará como cota superior el
     * resultado del primero.
     */
    constructor() {
        val resolutorGA = ResolutorGA()
        resolutorGA.tamPoblacion = 200
        resolutorGA.numGeneraciones = 200
        val resolutorV8 = ResolutorV8()
        estadisticas = EstadisticasCombinado(resolutorGA.estadisticas, resolutorV8.estadisticas)
        this.primero = resolutorGA
        this.segundo = resolutorV8
    }

    /**
     * Crea una nueva instancia de la clase, tomando como resolutores a los dos
     * que se le pasan. El primero debe ser un Resolutor de rápida ejecución,
     * tipicamente no exhaustivo (algoritmo evolutivo o voraz) y un segundo
     * resolutor de tipo exhustivo que tomará la solución del primero como base
     * para realizar una busqueda guiada.
     *
     * @param primero Resolutor no exhaustivo de rápida ejecución
     * @param segundo Resolutor actotado, generalmente exhaustivo
     */
    constructor(primero: Resolutor, segundo: ResolutorAcotado) {
        estadisticas = EstadisticasCombinado(primero.estadisticas, segundo.estadisticas)
        this.primero = primero
        this.segundo = segundo
    }


    /**
     * Resuelve el problema mediante la aplicación sucesiva del resolutor
     * primero y segundo, tomando el segundo como cota superior del fitness de
     * la solución el resultado del primero.
     *
     * @param horarios Datos de entrada
     * @return null o Collections.emptyMap() si no hay solución, una solución
     * válida en otro caso
     */
    override fun resolver(horarios: Set<Horario>): Map<Dia, AsignacionDia> {
        estadisticas.inicia()
        solucion = primero.resolver(horarios)
        val estadisticas1 = primero.estadisticas
        estadisticas.fitness = estadisticas1.fitness
        estadisticas.actualizaProgreso()
        val solucion2 = segundo.resolver(horarios, estadisticas1.fitness)
        val estadisticas2 = segundo.estadisticas
        if (solucion2.isNotEmpty() && estadisticas2.fitness < estadisticas1.fitness) {
            solucion = solucion2
            estadisticas.fitness = estadisticas2.fitness
        }
        estadisticas.actualizaProgreso()
        return solucion
    }

    override fun parar() {
        primero.parar()
        segundo.parar()
    }

    override val solucionFinal: Map<Dia, AsignacionDia>
        get() = solucion

    override var estrategia: Estrategia
        get() = primero.estrategia
        set(value) {
            primero.estrategia = value
            segundo.estrategia = value
        }
}
