function [ offsprings ] = sscombine( elementos, n )
%SSCOMBINE Genera las n combinaciones siguiendo el metodo por defecto de SS

if (size(elementos,1)<2)
    offsprings = elementos;
else
    % De momento solo vamos a trabajar con 2 elementos...
    x = elementos(1, :);
    y = elementos(2, :);

    % Reservamos la memoria para el numero de hijos esperado
    offsprings = zeros(n, length(x));

    % Calculamos un vector director base para la combinacion
    d = (y - x) ./ 2;

    % Obtenemos un valor aleatorio que nos servira para tomar
    % decisiones.
    a = floor(rand() * 3);

    for ind=1:n
        r = rand();
        switch (mod(a, 3))
            case 0 % Generamos C2
                offsprings(ind, :) = x + (r .* d);
            case 1 % Generamos C1
                offsprings(ind, :) = x - (r .* d);
            otherwise % Generamos C3
                offsprings(ind, :) = y + (r .* d);
        end
        a = a + 1;
    end
end
end