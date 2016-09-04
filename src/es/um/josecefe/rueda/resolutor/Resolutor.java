/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda.resolutor;

import es.um.josecefe.rueda.modelo.AsignacionDia;
import es.um.josecefe.rueda.modelo.Dia;
import es.um.josecefe.rueda.modelo.Horario;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author josec
 */
public abstract class Resolutor {
    protected boolean continuar;
    /**
     * Resuelve el problema de optimización de la rueda dado un horario de entrada.
     * 
     * @param horarios Entradas del horario
     * @return Resultado de la optimización
     */
    public abstract Map<Dia, ? extends AsignacionDia> resolver(Set<Horario> horarios);
    
    /**
     * Estadisticas referidas a la última resolución realizada
     * @return Un opción con las estadisticas
     */
    public abstract Estadisticas getEstadisticas();

    /**
     * Solución de la última resolución realizada
     * @return Solución de la última resolución realizada. Puede ser null si no hubo solución o aún no
     * se ha realizado ninguna resolución.
     */
    public abstract Map<Dia, ? extends AsignacionDia> getSolucionFinal();
    
    public void parar() {
        continuar = false;
    }
}
