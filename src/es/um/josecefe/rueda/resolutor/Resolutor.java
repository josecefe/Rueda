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
    public enum Estrategia {
        /**
         * Estrategia equilibrado persigue conseguir que todos el mundo sea
         * el mismo número de veces conductor, minimizando a la vez el número
         * de veces que se es conductor. Da lugar a asignaciones justas pero 
         * puede que muy desaprovechadas.
         */
        EQUILIBRADO, 
        /**
         * No busca un equilibrio entre conductores, sino la mejor distribución
         * de los mismos para minimizar su número absoluto (da lugar a situaciones
         * injustas)
         */
        MINCONDUCTORES
    }
    
    
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
    
    /**
     * Permite detener el algoritmo y obtener el resultado actual (si hay alguno), que puede no se óptimo.
     */
    public void parar() {
        continuar = false;
    }

    /** 
     * Fija la estrategia de optimización
     * 
     * @param estrategia Tipo de estrategia a seguir para la optimización
     */
    public abstract void setEstrategia(Estrategia estrategia);
}
