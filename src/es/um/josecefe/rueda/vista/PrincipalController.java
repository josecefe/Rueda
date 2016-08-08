/*
 * Copyright (C) 2016 josec
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.um.josecefe.rueda.vista;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * FXML Controller class
 *
 * @author josec
 */
public class PrincipalController {

    @FXML
    private TableView<?> tablaHorario;

    @FXML
    private TableColumn<?, ?> columnaHora;

    @FXML
    private TableView<?> tablaResultado;

    @FXML
    private ProgressIndicator indicadorProgreso;

    @FXML
    private Label barraEstado;

    /**
     * Initializes the controller class.
     */
    @FXML
    public void initialize() {
        assert tablaHorario != null : "fx:id=\"tablaHorario\" was not injected: check your FXML file 'Principal.fxml'.";
        assert columnaHora != null : "fx:id=\"columnaHora\" was not injected: check your FXML file 'Principal.fxml'.";
        assert tablaResultado != null : "fx:id=\"tablaResultado\" was not injected: check your FXML file 'Principal.fxml'.";
        assert indicadorProgreso != null : "fx:id=\"indicadorProgreso\" was not injected: check your FXML file 'Principal.fxml'.";
        assert barraEstado != null : "fx:id=\"barraEstado\" was not injected: check your FXML file 'Principal.fxml'.";
    }

    @FXML
    void cargarHorario(ActionEvent event) {

    }

    @FXML
    void guardarHorario(ActionEvent event) {

    }

    @FXML
    void guardarResultado(ActionEvent event) {

    }
}
