/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.um.josecefe.rueda.persistencia

import es.um.josecefe.rueda.modelo.*
import org.junit.jupiter.api.Test
import java.io.File

internal class PersistenciaTest {
    @Test
    fun guardaDatosRueda() {
        val dias = mutableListOf(Dia("Lunes"), Dia("Martes"), Dia("Miercoles"), Dia("Jueves"), Dia("Viernes"))
        val lugares = mutableListOf(Lugar("LA"), Lugar("LB"))
        val participantes = mutableListOf(Participante("PA", 5, listOf(lugares[0])), Participante("PB", 5, listOf(lugares[0], lugares[1])))
        val datosRueda = DatosRueda(
                dias,
                lugares,
                participantes,
                mutableListOf(
                        Horario(participantes[0], dias[0], 1, 6, true),
                        Horario(participantes[0], dias[1], 1, 6, true),
                        Horario(participantes[0], dias[2], 1, 6, true),
                        Horario(participantes[0], dias[3], 1, 6, true),
                        Horario(participantes[0], dias[4], 1, 6, true),
                        Horario(participantes[1], dias[0], 1, 6, true),
                        Horario(participantes[1], dias[1], 1, 6, true),
                        Horario(participantes[1], dias[2], 1, 6, true),
                        Horario(participantes[1], dias[3], 1, 6, true),
                        Horario(participantes[1], dias[4], 1, 6, true)
                ))

        Persistencia.guardaDatosRuedaJSON(File("datosRuedaTest.json"), datosRueda)
    }

    @Test
    fun cargaDatosRueda() {
        //val fichero = File("datosRuedaTest.xml")
        val fichero = File("datosRuedaTest.json")
        if (fichero.exists()) {
            val datosRueda = Persistencia.cargaDatosRuedaJSON(fichero)
            println(datosRueda.dias)
            println(datosRueda.lugares)
            println(datosRueda.participantes)
            println(datosRueda.horarios)
            Persistencia.guardaDatosRuedaJSON(File("datosRuedaTest.xml"), datosRueda) //Reguardado
        } else {
            println("No ha fichero con datos, prueba a ejecutar el test de guardado")
        }
    }

    @Test
    fun guardaAsignacionRueda() {
    }

    @Test
    fun exportaAsignacion() {
    }

}