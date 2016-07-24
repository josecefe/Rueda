function [ offsprings ] = gacrossoveradapt(cof, opciones, ff, elementos,...
    n, cofargs)
%GACROSSOVERADAPT Adapta a ScatterSearch una función de cruce de los alg.
%geneticos del toolbox de MATLAB.
%   Los parametros necesarios son:
%    * cof: Funcion de cruce de geneticos
%    * opciones: opciones a usar en la función de cruce de geneticos
%    * ff: funcion de fitness
%    * cofargs: argumentos adicionales para la función de geneticos
%    * elementos: elementos a cruzar de Scatter Search
%    * n: numero de hijos a generar
%
% Ejemplo (suponiendo que miss es un objeto de la clase ScatterSearch):
%    opciones = miss.CreationFnc{2};
%    opciones.LinearConstr.lb = miss.LowerBound'; % Transpuesta de lb
%    opciones.LinearConstr.ub = miss.UpperBound'; % Transpuesta de ub
%    miss.CombineFnc = @(e,n) gacrossoveradapt(@crossoverheuristic,...
%        opciones, [], e, n)

% Calculamos el numero de elementos a pasar a la funcion
necesarios = ceil(n*2 / (size(elementos, 1)));
if (necesarios>1)
    indices = repmat(1:size(elementos,1), 1, necesarios);
else
    indices = 1:n*2;
end

if (nargin < 6)
    cofargs = {};
end

offsprings=cof(indices,opciones,size(elementos,2), ff,...
    ones(necesarios,1),elementos,cofargs{:});
end
