/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.um.josecefe.rueda.resolutor;

/**
 *
 * @author josec
 */
public interface Estadisticas {

    Estadisticas setFitness(int aptitud);

    Estadisticas updateTime();
    
    int getFitness();
}