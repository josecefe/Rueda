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

import org.junit.jupiter.api.Assertions.*
import java.io.File

internal class PersistenciaXMLTest {
    @Test
    fun guardaDatosRueda() {
        val datosRueda = DatosRueda()
        datosRueda.dias = listOf(Dia(1,"Lunes"),Dia(2,"Martes"),Dia(3,"Miercoles"),Dia(4,"Jueves"),Dia(5,"Viernes"))
        datosRueda.lugares = listOf(Lugar(1, "LA"), Lugar(2, "LB"))
        datosRueda.participantes = listOf(Participante(1, "PA", 5,listOf(datosRueda.lugares[0])), Participante(2, "PB", 5, listOf(datosRueda.lugares[0], datosRueda.lugares[1])))
        datosRueda.horarios = listOf(
                Horario(datosRueda.participantes[0], datosRueda.dias[0], 1, 6, true),
                Horario(datosRueda.participantes[0], datosRueda.dias[1], 1, 6, true),
                Horario(datosRueda.participantes[0], datosRueda.dias[2], 1, 6, true),
                Horario(datosRueda.participantes[0], datosRueda.dias[3], 1, 6, true),
                Horario(datosRueda.participantes[0], datosRueda.dias[4], 1, 6, true),
                Horario(datosRueda.participantes[1], datosRueda.dias[0], 1, 6, true),
                Horario(datosRueda.participantes[1], datosRueda.dias[1], 1, 6, true),
                Horario(datosRueda.participantes[1], datosRueda.dias[2], 1, 6, true),
                Horario(datosRueda.participantes[1], datosRueda.dias[3], 1, 6, true),
                Horario(datosRueda.participantes[1], datosRueda.dias[4], 1, 6, true)
        )

        PersistenciaXML.guardaDatosRueda(File("datosRuedaTest.xml"), datosRueda)
    }

    @Test
    fun cargaDatosRueda() {
        //val fichero = File("datosRuedaTest.xml")
        val fichero = File("datosRuedaTest.xml")
        if (fichero.exists()) {
            val datosRueda = DatosRueda()
            PersistenciaXML.cargaDatosRueda(fichero, datosRueda)
            println(datosRueda.dias)
            println(datosRueda.lugares)
            println(datosRueda.participantes)
            println(datosRueda.horarios)
            PersistenciaXML.guardaDatosRueda(File("datosRuedaTest.xml"), datosRueda) //Reguardado
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