function mutationChildren = mimutacion(parents,options,GenomeLength, ...
    FitnessFcn,state,thisScore,thisPopulation,varargin)
%MIMUTACION Operador de mutación que aplica una busqueda local para mejorar.
%  MUTATIONCHILDREN = MIMUTACION(PARENTS,OPTIONS,GENOMELENGTH,...
%  FITNESSFCN,STATE,THISSCORE,THISPOPULATION, VARARGIN) Creates the mutated
%  children using adaptive mutation and doing a local search.
%
%   Example:
%     options = gaoptimset('MutationFcn',{@mimutacion});
%
%   This specifies that the mutation function used will be
%   MIMUTACION
%

%   Copyright 2008 Jose Ceferino Ortega.
%   $Revision: 1.0.0.0 $  $Date: 2008/07/23 13:42:42 $

% Creamos las opciones que vamos a usar en la busqueda
persistent opciones
if isempty(opciones)
    opciones = optimset('Algorithm', 'interior-point', 'MaxIter', 25,...
        'Display', 'off');
end

%% Llamamos a la funcion que genera la mutación original
mutationChildren = mutationadaptfeasible(parents,options,GenomeLength, ...
    FitnessFcn,state,thisScore,thisPopulation,varargin);

%% Tratamos el caso
if (mod(state.Generation, options.EliteCount*2) == 0)
    range = options.PopInitRange;
    minimo = range(1,:);
    maximo = range(2,:);

    % Definimos la estructura a usar para el problema de optimización
    problema=struct('objective', FitnessFcn, 'x0', [], 'Aineq', [],...
        'bineq', [], 'Aeq', [], 'beq', [], 'lb', minimo, 'ub', maximo, ...
        'nonlcon', [], 'solver', 'fmincon', 'options', opciones);

    %Iteramos hasta completar el tamaño, pero vamos a mejorar EliteCount elementos
    tamano=size(mutationChildren,1);
    cada=round(tamano/options.EliteCount);
    for l=1:cada:tamano
        problema.x0 = mutationChildren(l,:);
        t=fmincon(problema);
        if (isfinite(t))
            mutationChildren(l,:)=t;
        end
    end
end
end
