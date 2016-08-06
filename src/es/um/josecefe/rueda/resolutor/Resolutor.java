/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.Dia;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author josec
 */
public interface Resolutor {

    Optional<Estadisticas> getEstadisticas();

    Map<Dia, ? extends AsignacionDia> getSolucionFinal();

    Map<Dia, ? extends AsignacionDia> resolver();
    
}
