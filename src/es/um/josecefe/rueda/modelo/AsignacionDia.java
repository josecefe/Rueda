/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda.modelo;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author josec
 */
public interface AsignacionDia {

    Set<Participante> getConductores();

    int getCoste();

    Map<Participante, Lugar> getPeIda();

    Map<Participante, Lugar> getPeVuelta();
    
}
