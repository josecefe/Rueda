/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.persistencia

import es.um.josecefe.rueda.COPYRIGHT
import es.um.josecefe.rueda.TITLE
import es.um.josecefe.rueda.VERSION
import es.um.josecefe.rueda.modelo.*
import htmlflow.HtmlView
import javafx.util.Pair
import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4
import java.beans.*
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Collectors.toList

/**
 * @author josec
 */
object PersistenciaXML {

    fun guardaDatosRueda(xmlfile: File, datosRueda: DatosRueda) {
        try {
            XMLEncoder(
                    BufferedOutputStream(
                            FileOutputStream(xmlfile))).use { encoder ->

                encoder.exceptionListener = ExceptionListener { it.printStackTrace() }
                encoder.setPersistenceDelegate(Pair::class.java, PairPersistenceDelegate())
                // Poco a poco
                encoder.writeObject(ArrayList(datosRueda.dias))
                encoder.writeObject(ArrayList(datosRueda.lugares))
                encoder.writeObject(ArrayList(datosRueda.participantes))
                encoder.writeObject(ArrayList(datosRueda.horarios))
                encoder.writeObject(ArrayList(datosRueda.asignacion))
                encoder.writeObject(datosRueda.costeAsignacion)
            }
        } catch (ex: Exception) {
            Logger.getLogger(PersistenciaXML::class.java.name).log(Level.SEVERE, null, ex)
        }

    }

    fun cargaDatosRueda(xmlfile: File, datosRueda: DatosRueda) {
        try {
            XMLDecoder(
                    BufferedInputStream(
                            FileInputStream(xmlfile))).use { decoder ->
                decoder.setExceptionListener { e -> Logger.getLogger(PersistenciaXML::class.java.name).log(Level.SEVERE, null, e) }
                val ld = decoder.readObject() as List<*>
                datosRueda.dias = ld.map { e -> e as Dia }
                val ll = decoder.readObject() as List<*>
                datosRueda.lugares = ll.map { e -> e as Lugar }
                val lp = decoder.readObject() as List<*>
                datosRueda.participantes = lp.map { e -> e as Participante }
                val lh = decoder.readObject() as List<*>
                datosRueda.horarios = lh.map { e -> e as Horario }
                val la = decoder.readObject() as List<*>
                datosRueda.asignacion = la.map { e -> e as Asignacion }
                datosRueda.costeAsignacion = decoder.readObject() as Int
            }
        } catch (ex: Exception) {
            Logger.getLogger(PersistenciaXML::class.java.name).log(Level.SEVERE, null, ex)
        }

    }

    fun guardaAsignacionRueda(xmlfile: String, solucionFinal: Map<Dia, AsignacionDia>) {
        try {
            XMLEncoder(
                    BufferedOutputStream(
                            FileOutputStream(xmlfile))).use { encoder -> encoder.writeObject(solucionFinal) }
        } catch (ex: FileNotFoundException) {
            Logger.getLogger(PersistenciaXML::class.java.name).log(Level.SEVERE, null, ex)
        }

    }

    fun exportaAsignacion(htmlfile: File, datosRueda: DatosRueda) {

        try {
            PrintStream(htmlfile).use { out ->
                val conLugar = datosRueda.asignacion.flatMap { a -> a.peIda}.map{ it.getValue() }.distinct().count() > 1

                // Todas las horas con actividad:
                val horasActivas = datosRueda.horarios.map{ it.entrada}.toMutableSet()
                horasActivas.addAll(datosRueda.horarios.map{ it.salida }.toSet())

                // Vamos a guardar en una tabla "virtual"
                val datosTabla: MutableMap<Dia, MutableMap<Int, MutableList<ParticipanteIdaConduceLugar>>> = HashMap(datosRueda.dias.size)
                for (a in datosRueda.asignacion) {
                    val d = a.dia
                    datosRueda.horarios.stream().filter { h -> h.dia == d }.forEach { h ->
                        var horasDia = datosTabla[d]
                        if (horasDia == null) {
                            horasDia = HashMap(horasActivas.size)
                            datosTabla.put(d, horasDia)
                        }
                        val celdaIda = horasDia.computeIfAbsent(h.entrada) { ArrayList() }

                        val celdaVuelta = horasDia.computeIfAbsent(h.salida) { ArrayList() }

                        // Datos de la IDA y la VUELTA
                        val pIda = ParticipanteIdaConduceLugar()
                        val pVuelta = ParticipanteIdaConduceLugar()
                        pIda.ida = true
                        pVuelta.ida = false
                        pVuelta.participante = h.participante
                        pIda.participante = pVuelta.participante
                        pVuelta.conduce = a.conductores.contains(pIda.participante)
                        pIda.conduce = pVuelta.conduce
                        a.peIda.find { it.key == pIda.participante }?.let {
                            pIda.lugar = it.value
                            celdaIda.add(pIda)
                        }
                        a.peVuelta.find { p -> p.key == pVuelta.participante }?.let {
                            pVuelta.lugar = it.value
                            celdaVuelta.add(pVuelta)
                        }
                    }
                }
                //Ahora generamos la tabla en HTML
                val htmlView = HtmlView<Any>()
                htmlView.head()
                        .title(escapeHtml4("Asignación Rueda - " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                        .linkCss("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")
                val table = htmlView
                        .body().classAttr("container")
                        .heading(3, escapeHtml4("Asignación Rueda - " + LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT))))
                        .div()
                        .table().classAttr("table table-bordered")
                val headerRow = table.tr()
                headerRow.th().text("Hora")
                datosRueda.dias.forEach { d -> headerRow.th().text(escapeHtml4(d.toString())) }
                horasActivas.stream().sorted().forEachOrdered { hora ->
                    val tr = table.tr()
                    tr.td().text(hora!!.toString()) //Hora
                    datosRueda.dias.forEach { dia ->
                        val dd = datosTabla[dia]
                        val t = if (dd != null) dd[hora] else null
                        var valor = ""
                        if (t != null) {
                            valor = t.stream()
                                    .sorted { p1, p2 ->
                                        if (p1.ida != p2.ida)
                                            if (p1.ida) -1 else 1
                                        else if (p1.lugar!!.compareTo(p2.lugar!!) != 0)
                                            p1.lugar!!.compareTo(p2.lugar!!)
                                        else
                                            if (p1.conduce != p2.conduce)
                                                if (p1.conduce) -1 else 1
                                            else
                                                p1.participante!!.compareTo(p2.participante!!)
                                    }
                                    .map { p ->
                                        val res = StringBuilder()
                                        if (p.ida) {
                                            res.append("<i>")
                                        }
                                        if (p.conduce) {
                                            res.append("<b>").append(escapeHtml4("*"))
                                        }
                                        res.append(escapeHtml4(p.participante!!.toString()))
                                        if (conLugar) {
                                            res.append(" [")
                                            res.append(escapeHtml4(p.lugar!!.toString()))
                                            res.append("]")
                                        }


                                        if (p.conduce) {
                                            res.append("</b>")
                                        }
                                        if (p.ida) {
                                            res.append("</i>")
                                        }
                                        res.toString()
                                    }.collect(Collectors.joining("<br>\n"))
                        }
                        tr.td().text(valor)
                    }
                }

                // Leyenda
                if (conLugar) {
                    htmlView.body().div().text("Leyenda: <i><b>*Conductor [Lugar de Ida]</b></i> | <i>Pasajero [Lugar de Ida]</i> | <b>*Conductor [Lugar de Vuelta]</b> | Pasajero [Lugar de Vuelta]").addAttr("style", "color:green;text-align:center")
                } else {
                    htmlView.body().div().text("Leyenda: <i><b>*Conductor Ida</b></i> | <i>Pasajero Ida</i> | <b>*Conductor Vuelta</b> | Pasajero Vuelta").addAttr("style", "color:green;text-align:center")
                }

                // Coste
                htmlView.body().div().text(String.format("%s <b>%,d</b>", escapeHtml4("Coste total asignación: "), datosRueda.costeAsignacion)).addAttr("style", "color:royal-blue;text-align:right")

                // Cuadro de conductor/dias
                htmlView.body().heading(4, escapeHtml4("Cuadro de conductor/días"))
                val tabla = htmlView.body().table().classAttr("table table-bordered")
                val cabecera = tabla.tr()
                cabecera.th().text(escapeHtml4("Conductor"))
                cabecera.th().text(escapeHtml4("Días"))
                cabecera.th().text(escapeHtml4("Total"))
                datosRueda.participantes.stream().sorted().forEachOrdered { participante ->
                    val dias = datosRueda.asignacion.stream().filter { a -> a.conductores.contains(participante) }.map{ it.dia }.sorted().collect(toList())
                    if (dias.size > 0) {
                        val tr = tabla.tr()
                        tr.td().text(escapeHtml4(participante.nombre))
                        tr.td().text(escapeHtml4(dias.toString()))
                        tr.td().text(dias.size.toString())
                    }
                }

                // Cuadro de dia/conductores
                htmlView.body().heading(4, escapeHtml4("Cuadro de día/conductores"))
                val tablaDias = htmlView.body().table().classAttr("table table-bordered")
                val cabeceraDias = tablaDias.tr()
                cabeceraDias.th().text(escapeHtml4("Día"))
                cabeceraDias.th().text(escapeHtml4("Conductores"))
                cabeceraDias.th().text(escapeHtml4("Total"))
                datosRueda.asignacion.stream().sorted().forEachOrdered { a ->
                    val tr = tablaDias.tr()
                    tr.td().text(escapeHtml4(a.dia.descripcion))
                    tr.td().text(escapeHtml4(a.conductores.toString()))
                    tr.td().text(a.conductores.size.toString())
                }

                // Pie de pagina
                htmlView.body().hr().div().text("Generado con <b>$TITLE $VERSION<b> <i>$COPYRIGHT</i>").addAttr("style", "color:royalblue;text-align:right")
                htmlView.setPrintStream(out)
                htmlView.write()
            }
        } catch (ex: Exception) {
            Logger.getLogger(PersistenciaXML::class.java.name).log(Level.SEVERE, "Problemas generando la exportación a HTML: ", ex)
        }

    }

    private class ParticipanteIdaConduceLugar {

        internal var participante: Participante? = null
        internal var ida: Boolean = false
        internal var conduce: Boolean = false
        internal var lugar: Lugar? = null
    }

    private class PairPersistenceDelegate : DefaultPersistenceDelegate(arrayOf("key", "value")) {

        override fun instantiate(oldInstance: Any, out: Encoder): Expression {
            val par = oldInstance as Pair<*, *>
            val constructorArgs = arrayOf(par.key, par.value)
            return Expression(oldInstance, oldInstance.javaClass, "new", constructorArgs)
        }
    }

}
