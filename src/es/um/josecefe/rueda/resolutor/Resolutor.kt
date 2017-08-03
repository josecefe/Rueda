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
abstract class Resolutor {
    protected var continuar: Boolean = false

    /**
     * Resuelve el problema de optimización de la rueda dado un horario de entrada.
     *
     * @param horarios Entradas del horario
     * @return Resultado de la optimización
     */
    abstract fun resolver(horarios: Set<Horario>): Map<Dia, AsignacionDia>

    /**
     * Estadisticas referidas a la última resolución realizada
     *
     * @return Un opción con las estadisticas
     */
    abstract val estadisticas: Estadisticas

    /**
     * Solución de la última resolución realizada
     *
     * @return Solución de la última resolución realizada. Puede ser null si no hubo solución o aún no
     * se ha realizado ninguna resolución.
     */
    abstract val solucionFinal: Map<Dia, AsignacionDia>

    /**
     * Permite detener el algoritmo y obtener el resultado actual (si hay alguno), que puede no se óptimo.
     */
    open fun parar() {
        continuar = false
    }

    abstract var estrategia: Estrategia

    enum class Estrategia {
        /**
         * Estrategia equilibrado persigue conseguir que todos el mundo sea
         * el mismo número de veces conductor, minimizando a la vez el número
         * de veces que se es conductor. Da lugar a asignaciones justas pero
         * puede que muy desaprovechadas.
         */
        EQUILIBRADO,
        /**
         * No busca un equilibrio entre conductores, sino la mejor distribución
         * de los mismos para minimizar su número absoluto (da lugar a situaciones
         * injustas)
         */
        MINCONDUCTORES
    }
}
