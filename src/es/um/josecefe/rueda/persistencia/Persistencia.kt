/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.persistencia

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import es.um.josecefe.rueda.COPYRIGHT
import es.um.josecefe.rueda.TITLE
import es.um.josecefe.rueda.VERSION
import es.um.josecefe.rueda.modelo.*
import htmlflow.HtmlView
import org.apache.commons.text.StringEscapeUtils.escapeHtml4
import java.beans.*
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author josec
 */
object Persistencia {

    fun guardaDatosRuedaXML(xmlfile: File, datosRueda: DatosRueda) {
        try {
            XMLEncoder(BufferedOutputStream(FileOutputStream(xmlfile))).use { encoder ->

                with(encoder) {
                    exceptionListener = ExceptionListener { it.printStackTrace() }
                    setPersistenceDelegate(Pair::class.java, PairPersistenceDelegate())
                    writeObject(ArrayList(datosRueda.dias))
                    writeObject(ArrayList(datosRueda.lugares))
                    writeObject(ArrayList(datosRueda.participantes))
                    writeObject(ArrayList(datosRueda.horarios))
                    writeObject(ArrayList(datosRueda.asignacion))
                    writeObject(datosRueda.costeAsignacion)
                }
            }
        } catch (ex: Exception) {
            Logger.getLogger(Persistencia::class.java.name).log(Level.SEVERE, null, ex)
        }

    }

    fun cargaDatosRuedaXML(xmlfile: File): DatosRueda {
        try {
            XMLDecoder(
                    BufferedInputStream(
                            FileInputStream(xmlfile))).use { decoder ->
                decoder.setExceptionListener { e -> Logger.getLogger(Persistencia::class.java.name).log(Level.SEVERE, null, e) }
                val ld = decoder.readObject() as List<*>
                val dias = ld.map { e -> e as Dia }.toMutableList()
                val ll = decoder.readObject() as List<*>
                val lugares = ll.map { e -> e as Lugar }.toMutableList()
                val lp = decoder.readObject() as List<*>
                val participantes = lp.map { e -> e as Participante }.toMutableList()
                val lh = decoder.readObject() as List<*>
                val horarios = lh.map { e -> e as Horario }.toMutableList()
                val la = decoder.readObject() as List<*>
                val asignacion = la.map { e -> e as Asignacion }.toMutableList()
                val costeAsignacion = decoder.readObject() as Int
                return DatosRueda(dias, lugares, participantes, horarios, asignacion, costeAsignacion)
            }
        } catch (ex: Exception) {
            Logger.getLogger(Persistencia::class.java.name).log(Level.SEVERE, null, ex)
        }
        return DatosRueda()
    }

    fun guardaAsignacionRuedaXML(xmlfile: String, solucionFinal: Map<Dia, AsignacionDia>) {
        try {
            XMLEncoder(BufferedOutputStream(FileOutputStream(xmlfile))).use { encoder -> encoder.writeObject(solucionFinal) }
        } catch (ex: FileNotFoundException) {
            Logger.getLogger(Persistencia::class.java.name).log(Level.SEVERE, null, ex)
        }

    }

    fun exportaAsignacionHTML(htmlfile: File, datosRueda: DatosRueda) {

        try {
            PrintStream(htmlfile).use { out ->
                val conLugar = datosRueda.asignacion.flatMap { a -> a.peIda }.map { it.second }.distinct().count() > 1

                // Todas las horas con actividad:
                val horasActivas = datosRueda.horarios.map { it.entrada }.toMutableSet()
                horasActivas.addAll(datosRueda.horarios.map { it.salida }.toSet())

                // Vamos a guardar en una tabla "virtual"
                val datosTabla: MutableMap<Dia, MutableMap<Int, MutableList<ParticipanteIdaConduceLugar>>> = HashMap(datosRueda.dias.size)
                for (a in datosRueda.asignacion) {
                    val d = a.dia
                    datosRueda.horarios.filter { h -> h.dia == d }.forEach { h ->
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
                        a.peIda.find { it.first == pIda.participante }?.let {
                            pIda.lugar = it.second
                            celdaIda.add(pIda)
                        }
                        a.peVuelta.find { p -> p.first == pVuelta.participante }?.let {
                            pVuelta.lugar = it.second
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
                datosRueda.dias.sorted().forEach { d -> headerRow.th().text(escapeHtml4(d.toString())) }
                horasActivas.sorted().forEach { hora ->
                    val tr = table.tr()
                    tr.td().text(hora.toString()) //Hora
                    datosRueda.dias.sorted().forEach { dia ->
                        val dd = datosTabla[dia]
                        val t = if (dd != null) dd[hora] else null
                        var valor = ""
                        if (t != null) {
                            valor = t.sortedWith(kotlin.Comparator { p1, p2 ->
                                if (p1.ida != p2.ida)
                                    if (p1.ida) -1 else 1
                                else if (p1.lugar!!.compareTo(p2.lugar!!) != 0)
                                    p1.lugar!!.compareTo(p2.lugar!!)
                                else
                                    if (p1.conduce != p2.conduce)
                                        if (p1.conduce) -1 else 1
                                    else
                                        p1.participante!!.compareTo(p2.participante!!)
                            })
                                    .joinToString(separator = "<br>\n") { p ->
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
                                    }
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
                datosRueda.participantes.sorted().forEach { participante ->
                    val dias = datosRueda.asignacion.filter { a -> a.conductores.contains(participante) }.map { it.dia }.sorted()
                    if (dias.isNotEmpty()) {
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
                datosRueda.asignacion.sorted().forEach { a ->
                    val tr = tablaDias.tr()
                    tr.td().text(escapeHtml4(a.dia.descripcion))
                    tr.td().text(escapeHtml4(a.conductores.toString()))
                    tr.td().text(a.conductores.size.toString())
                }

                // Pie de pagina
                htmlView.body().hr().div().text("Generado con <b>$TITLE $VERSION</b> <i>$COPYRIGHT</i>").addAttr("style", "color:royalblue;text-align:right")
                htmlView.setPrintStream(out)
                htmlView.write()
            }
        } catch (ex: Exception) {
            Logger.getLogger(Persistencia::class.java.name).log(Level.SEVERE, "Problemas generando la exportación a HTML: ", ex)
        }

    }

    private class ParticipanteIdaConduceLugar {

        internal var participante: Participante? = null
        internal var ida: Boolean = false
        internal var conduce: Boolean = false
        internal var lugar: Lugar? = null
    }

    private class PairPersistenceDelegate : DefaultPersistenceDelegate(arrayOf("first", "second")) {
        override fun instantiate(oldInstance: Any, out: Encoder): Expression {
            val par = oldInstance as Pair<*, *>
            val constructorArgs = arrayOf(par.first, par.second)
            return Expression(oldInstance, oldInstance::class.java, "new", constructorArgs)
        }
    }

    fun guardaDatosRuedaJSON(file: File, datosRueda: DatosRueda) {
        try {

            BufferedOutputStream(
                    FileOutputStream(file)).use { f ->
                val mapper = jacksonObjectMapper()

                with(mapper) {
                    writeValue(f, datosRueda)
                }
            }
        } catch (ex: Exception) {
            Logger.getLogger(Persistencia::class.java.name).log(Level.SEVERE, null, ex)
        }
    }

    fun cargaDatosRuedaJSON(file: File): DatosRueda {
        try {

            BufferedInputStream(
                    FileInputStream(file)).use { f ->
                val mapper = jacksonObjectMapper()

                with(mapper) {
                    return mapper.readValue(f)
                }
            }
        } catch (ex: Exception) {
            Logger.getLogger(Persistencia::class.java.name).log(Level.SEVERE, null, ex)
        }
        return DatosRueda()
    }
}
