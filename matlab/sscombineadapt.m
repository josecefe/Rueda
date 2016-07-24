function xoverKids = sscombineadapt(parents, options, nvars, FitnessFcn, thisScore, thisPopulation)
%SSCOMBINEADAPT es una funcion que adapta a los algoritmos geneticos la
%funcion de combinacion sscombine creada originalmente para Scatter Search
% Los parametros que debe admitir esta funcion son:
%  * parents — Vector con los padres elegidos por la funcion de seleccion
%  * options — Estructura con las opciones -no se usa
%  * nvars — Numero de variables
%  * FitnessFcn — Funcion de Fitness - no se usa
%  * thisScore — Matriz de puntuacion - no se usa
%  * thisPopulation — La matriz con la poblacion actual

% Cuantos hijos hay que generar?
nKids = length(parents)/2;

% Reserva el espacio para los hijos del cruze
xoverKids = zeros(nKids,nvars);

for ind=1:nKids
    % Hacemos un cruce y pedimos solo un hijo
    xoverKids(ind,:)=sscombine(thisPopulation(parents(ind*2-1:ind*2),:),1);
end

% Extract information about linear constraints, if any
linCon = options.LinearConstr;
if ~isequal(linCon.type,'unconstrained')
    xoverKids = corrigelim(xoverKids, linCon.lb', linCon.ub');
end