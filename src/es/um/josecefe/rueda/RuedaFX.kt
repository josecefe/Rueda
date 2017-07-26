/*
 * Copyright (c) 2016-2017. Jose Ceferino Ortega Carretero
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda

import es.um.josecefe.rueda.modelo.DatosRueda
import es.um.josecefe.rueda.persistencia.PersistenciaXML
import es.um.josecefe.rueda.vista.PrincipalController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import javafx.stage.Window

import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import java.util.prefs.Preferences

/**
 * @author josec
 */
class RuedaFX : Application() {
    private val datosRueda: DatosRueda
    private var primaryStage: Stage? = null

    init {
        this.datosRueda = DatosRueda()
    }

    fun getPrimaryStage(): Window? {
        return primaryStage
    }

    override fun start(pStage: Stage) {
        try {
            primaryStage = pStage
            pStage.title = "$TITLE $VERSION - $COPYRIGHT"
            pStage.icons.add(Image(javaClass.getResourceAsStream("res/rueda.png")))

            val loader = FXMLLoader()
            loader.location = javaClass.getResource("vista/Principal.fxml")
            val principal = loader.load<BorderPane>()

            // Give the controller access to the main app.
            val controller = loader.getController<PrincipalController>()
            controller.setDatosRueda(datosRueda)
            controller.setMainApp(this)

            // Show the scene containing the root layout.
            val scene = Scene(principal)
            pStage.scene = scene
            pStage.isMaximized = true

            pStage.show()

            // Try to load last opened person file.
            val file = lastFilePath
            if (file != null) {
                cargaHorarios(file)
            }
        } catch (ex: IOException) {
            Logger.getLogger(RuedaFX::class.java.name).log(Level.SEVERE, "Fallo inicializando la ventana principal", ex)
        }

    }

    /**
     * Sets the file path of the currently loaded file. The path is persisted in
     * the OS specific registry.
     *
     */
    var lastFilePath: File?
        get() {
            val prefs = Preferences.userNodeForPackage(javaClass)
            val lastSave = prefs.get("lastSave", null)
            return if (lastSave != null) {
                File(lastSave)
            } else {
                null
            }
        }
        set(file) {
            val prefs = Preferences.userNodeForPackage(javaClass)
            if (file != null) {
                prefs.put("lastSave", file.path)
                primaryStage!!.title = TITLE + " [" + file.name + "] - " + COPYRIGHT
            } else {
                prefs.remove("filePath")
                primaryStage!!.title = TITLE + " - " + COPYRIGHT
            }
        }

    /**
     * Loads horario data from the specified file. The current datosRueda data will
     * be replaced.
     *
     * @param file Fichero del que cargar los datos
     */
    fun cargaHorarios(file: File) {
        try {
            PersistenciaXML.cargaDatosRueda(file, datosRueda)
            // Save the file path to the registry.
            //datosRueda.reemplazar(hs);
            lastFilePath = file

        } catch (e: Exception) { // catches ANY exception
            Logger.getLogger(javaClass.name).log(Level.WARNING,
                    "No se ha podido cargar los datos del fichero:\n" + file.path, e)
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error"
            alert.headerText = "No se ha podido cargar los datos del fichero:\n" + file.path
            alert.contentText = e.toString()
            alert.showAndWait()
        }

    }

    /**
     * Saves the current horario data to the specified file.
     *
     * @param file Fichero en el que guardar los datosRueda
     */
    fun guardaHorarios(file: File) {
        try {
            PersistenciaXML.guardaDatosRueda(file, datosRueda)
            // Save the file path to the registry.
            lastFilePath = file
        } catch (e: Exception) { // catches ANY exception
            Logger.getLogger(javaClass.name).log(Level.WARNING,
                    "No se ha podido guardar los datos en el fichero:\n" + file.path, e)
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error"
            alert.headerText = "No se ha podido guardar los datos en el fichero:\n" + file.path
            alert.contentText = e.toString()
            alert.showAndWait()
        }

    }

    /**
     * Saves the current horario data to the specified file.
     *
     * @param file Fichero en el que guardar el HTML generado
     * @return indica si se completo con exito la operación
     */
    fun exportaAsignacion(file: File): Boolean {
        try {
            PersistenciaXML.exportaAsignacion(file, datosRueda)
            return true
            //Duda: ¿Guardar dónde se hizo la última exportación o no?
        } catch (e: Exception) { // catches ANY exception
            Logger.getLogger(javaClass.name).log(Level.WARNING,
                    "No se ha podido exportar la asignación en el fichero:\n" + file.path, e)
            val alert = Alert(Alert.AlertType.ERROR)
            alert.title = "Error"
            alert.headerText = "No se ha podido exportar la asignación en el fichero:\n" + file.path
            alert.contentText = e.toString()
            alert.showAndWait()
        }

        return false
    }
}

fun main(args: Array<String>) {
    Application.launch(*args)
}
