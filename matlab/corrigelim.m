function [ solfeasible ] = corrigelim( sol, lb, ub )
%CORRIGELIM Genera soluciones factibles a partir de una matriz de
%soluciones
%   Los parametros son:
%    * SOL: matriz de soluciones que pueden no ser factibles
%    * LB: Vector de limite inferior (si no hay, [])
%    * UB: Vector de limite superior (si no hay, [])
%   Devuelve:
%     SOLFEASIBLE: una matriz con igual numero de elementos que SOL pero
%     todos dentro de los limites LB y UB.
solfeasible = sol;
if (~isempty(ub))
    mihigh = repmat(ub, size(solfeasible, 1), 1);
    indmal = solfeasible > mihigh;
    if (any(any(indmal)))
        solfeasible(indmal) = mihigh(indmal);
    end
end
if (~isempty(lb))
    milow = repmat(lb, size(solfeasible, 1), 1);
    indmal = solfeasible < milow;
    if (any(any(indmal)))
        solfeasible(indmal) = milow(indmal);
    end
end
end

