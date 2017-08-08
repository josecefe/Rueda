/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda

import es.um.josecefe.rueda.modelo.Dia
import es.um.josecefe.rueda.modelo.Horario
import es.um.josecefe.rueda.persistencia.Persistencia
import es.um.josecefe.rueda.resolutor.Resolutor
import es.um.josecefe.rueda.resolutor.ResolutorExhaustivo
import es.um.josecefe.rueda.resolutor.ResolutorV7
import es.um.josecefe.rueda.resolutor.ResolutorV8
import java.io.File
import java.util.*

/**
 * Clase principal de la aplicación Rueda
 *
 * @author josecefe@um.es
 */

private const val RUEDA_BASE = "rueda"
private const val RUEDAXML_HORARIOS = RUEDA_BASE + "_test.xml"
private const val RUEDAJSON_HORARIOS = RUEDA_BASE + "_test.json"
private const val COMPARANDO = false
private const val AMPLIADO = false

private fun duplicarHorario(horarios: Set<Horario>): Set<Horario> {
    val nHorarios: HashSet<Horario> = HashSet(horarios)
    val dias: Map<Dia, Dia> = horarios.map { it.dia }.distinct().filterNotNull().associate { Pair(it, Dia(it.descripcion + "Ex")) }
    nHorarios.addAll(horarios.map { Horario(it.participante, dias[it.dia], it.entrada, it.salida, it.coche) })

    return nHorarios
}

fun pruebaResolutor() {
    val datos = Persistencia.cargaDatosRuedaJSON(File(RUEDAJSON_HORARIOS))
    //val datos = Persistencia.cargaDatosRuedaXML(File(RUEDAXML_HORARIOS))
    val horarios: HashSet<Horario> = HashSet(datos.horarios)
    // Vamos a guardarlo en XML
    //Persistencia.guardaDatosRuedaXML(new File(RUEDAXML_HORARIOS), datos);

    val resolutores: List<Resolutor> = Arrays.asList(
            ResolutorV8(),
            ResolutorV7(),
            ResolutorExhaustivo()
    )
    if (COMPARANDO) {
        resolutores.forEach { r ->
            println("\n**********************************\n")
            System.out.format("Resolvemos el problema normal con %s:\n", r.javaClass.simpleName)
            r.resolver(horarios)
            println("\n**********************************\n")
        }
        resolutores.forEach { r ->
            System.out.format("Resolutor %s:\n->sol=%s\n->%s\n", r.javaClass.simpleName, r.solucionFinal,
                    r.estadisticas)
        }
    } else {
        val r = resolutores[resolutores.size - 1]
        println("\n**********************************\n")
        System.out.format("Resolvemos el problema normal con %s:\n", r.javaClass.simpleName)
        r.resolver(horarios)
        println("\n**********************************\n")
        System.out.format("Resolutor %s:\n=%s\n->%s\n", r.javaClass.simpleName, r.solucionFinal, r.estadisticas)
        //PersistenciaSQL.guardaAsignacionRuedaXML(RUEDABD, r.getSolucionFinal());
        datos.setSolucion(r.solucionFinal, r.estadisticas.fitness)
        Persistencia.guardaDatosRuedaJSON(File(RUEDAJSON_HORARIOS), datos)
    }
    if (AMPLIADO) {
        val horariosAmpliado = duplicarHorario(horarios)
        val r = ResolutorV8()

        println("\n**********************************\n")
        System.out.format("Resolvemos el problema Ampliado con %s:\n", r.javaClass.simpleName)
        System.out.format("Fase 1: Ampliado %s:\n", r.javaClass.simpleName)
        println(r.resolver(horariosAmpliado))
        System.out.format("->Est: %s\n", r.estadisticas)
    }
}


fun main(args: Array<String>) {
    pruebaResolutor()
}
