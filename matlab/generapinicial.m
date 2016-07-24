function pinicial = generapinicial(variables, funcoste, options)
% GENERAPINICIAL Permite generar una poblaci�n inicial del tama�o 'tamano'
% para la funci�n 'funcoste' dada, que admite 'variables' variables
% Util para los algoritmos de busqueda. Ej.
%  poblacion = generapinicial(@funcostetc, 8, 100)
% Para generar la poblaci�n se usa un algoritmo de busqueda directa
% partiendo de un punto al azar.

% Llamamos a la funci�n por defecto de generaci�n
% y trabajaremos sobre los valores que devuelva
pinicial = gacreationlinearfeasible(variables, funcoste, options);

range = options.PopInitRange;
minimo = range(1,:);
maximo = range(2,:);

% Creamos las opciones que vamos a usar en la busqueda
persistent opciones
if isempty(opciones)
    opciones = optimset('Algorithm', 'interior-point', 'MaxIter', 100,...
        'Display', 'off');
end

% Definimos la estructura a usar para el problema de optimizaci�n
problema=struct('objective', funcoste, 'x0', [], 'Aineq', [],...
    'bineq', [], 'Aeq', [], 'beq', [], 'lb', minimo, 'ub', maximo, ...
    'nonlcon', [], 'solver', 'fmincon', 'options', opciones);

%Iteramos hasta completar el tama�o (solo optimizamos EliteCount*2 elementos)
tamano=size(pinicial,1);
cada=ceil(tamano/(options.EliteCount*2));
for l=1:cada:tamano
    problema.x0 = pinicial(l,:);
    t=fmincon(problema);
    if (isfinite(t))
        pinicial(l,:)=t;
    end
end

end