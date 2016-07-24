function [ x, fval, xsel ] = backtracking( fitnessfnc, nvars, lb, ub,...
    meshsize, numsol, verbose )
%BACKTRACKING Resuelve el problema mediante el uso de backtracking
% Encuentras los NUMSOL mejores soluciones en cuanto a FITNESSFNC (min.)
% realizando un recorrido exhaustivo entre los valores de LB y UB (limites 
% inferior y superior respectivamente), en MESHSIZE pasos (debe ser impar), 
% probando todas las combinaciones para las NVARS variables.
% Devuelve en X la mejor solucion, en FVAL el valor de fitness de esa
% solución y en XSEL una matriz donde la primera columna es el valor de
% fitness de la solución contenida en esa misma fila.

xsel = zeros(numsol, nvars + 1);
xsel(:, 1) = Inf; % En la primera columna guardamos el valor de fitnes
xsel(:, 2:end) = NaN; % En el resto la solución correspondiente

if (length(lb)<nvars)
    lb = ones(1, nvars) * lb(1);
end

if (length(ub)<nvars)
    ub = ones(1, nvars) * ub(1);
end

sol = zeros(1, nvars);
ind = zeros(1, nvars);

nivel = 1;
cont = 0;

if (nargin<7)
    verbose=false;
end

total = (meshsize^nvars);

% Esto es verbose
if (verbose)
    fprintf('Realizando un backtracking completo de %d iteraciones:\n',...
        total);
end

while (nivel>0)
    % Generamos siguiente solución a evaluar
    sol(nivel) = (lb(nivel)+ub(nivel))/2 + ((-1)^ind(nivel))...
        * ceil(ind(nivel)/2)*(ub(nivel)-lb(nivel))/(meshsize-1); 
    
    ind(nivel)=ind(nivel) + 1; % Preparamos para el siguiente hermano
    
    if (nivel == nvars) % Comprobamos si es solución
        nfval = fitnessfnc(sol); % Calculamos su fitness

        % Esto es para hacer test
        %disp(sol);
        
        % Esto es verbose
        if (verbose)
            cont = cont + 1;
            if (mod(cont, 1000)==0)
                fprintf('.');
                if (mod(cont, 70000)==0)
                    fprintf(' %g%%\n', (100*cont/total));
                end
            end
        end

        if (nfval < xsel(end, 1)) % Mejoramos
            xsel(end, 1) = nfval; % Guardamos la mejor solución (fval)
            xsel(end, 2:end) = sol; % Guardamos la mejor solución (x)
            xsel = sortrows(xsel, 1);
            if (verbose)
                fprintf('********\n*MEJORA* -> fval=%g, x:', nfval);
                disp(sol);
            end
        end
    else
        nivel = nivel + 1; % Pasamos al siguente nivel
    end

    while (nivel>0 && ind(nivel)>=meshsize) % Hemos examinado todos los hermanos?
        ind(nivel) = 0; % Debemos retroceder de nivel
        nivel = nivel - 1;
    end
end
fval = xsel(1, 1);
x = xsel(1, 2:end); 
end
