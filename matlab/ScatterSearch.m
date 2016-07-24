classdef ScatterSearch < handle
%ScatterSearch Es el resolutor/optimizador de problemas basado en Scatter Search
%   ScatterSearch es un clase que implementa un resolutor de problemas basado en
%   la tecnica metaheuristica Scatter Search.

% Ejemplo de uso:
%  [ceros, raices, cte] = cerosraices5();
%  pos = t2trisection3();
%  fitnessfnc=@(x)funcoste(ceros,raices,cte,pos,x);
%  miss = ScatterSearch(fitnessfnc, length(pos), 10, 10, 200, -5, 5);
% Para hacerlo mas informativo:
%  miss.Verbose=1;
% Y para empezar la optimizacion:
%  [x,fval]=miss.optimize();

    properties
        FitnessFnc = []; % Funcion de fitness
        
        DiverFnc = @minqdist; % Funcion de medida de la diversidad
        
        CreationFnc = {@gacreationlinearfeasible, ScatterSearch.opCreateFnc(), ...
            'PopulationSize', 'PopInitRange'}; % Funcion de creacion
        % La funcion de creacion se pasa en un cell con la estructura:
        %   CreationFnc{1} => Manejador de la funcion
        %   CreationFnc{2} => Opciones
        %   CreationFnc{3} => Campo donde se indica el tamano de la Poblacion
        %   CreationFnc{4} => Campo donde se indica la matriz de limites: fila
        %    * la primera fila sera el vector con los limites inferiores
        %    * la segunda fila sera el vector con los limites superiores
        
        % Opciones de funcion de mejora
        OptimFncOpt = optimset('MaxIter',20,'Display','off');
        
        OptimFnc = ScatterSearch.OptimFncDefault(); % Funcion de Mejora
        % La funcion de mejora debe tener la siguiente signatura:
        %  [x, fval] = optimfnc(fitnessfnc, x, lb, ub, options)
        OptimFactor = 0.2; % Tanto por 1 de aplicacion de la mejora
        
        CombineFnc = @sscombine; % Funcion de combinacion de elementos
        % La funcion de CombineFnc debe tener la siguiente signatura
        %  [offsprings] = CombineFnc(elems, n)
        % donde elems son los elementos a combinar (cada fila un elemento), 
        % y n el numero de nuevos elementos que deben resultar de la 
        % combiancion en la matriz de salida offsprings.
        
        NVar = uint8(0); % Numero de variables del problema
        UpperBound = []; % Limite superior
        LowerBound = []; % Limite inferior
        Generations = uint32(50); % Numero de Maximo de Iteraciones
        QSetSize = uint32(10); % Tamano del conjunto de calidad
        DSetSize = uint32(10); % Tamano del conjunto de diversidad
        PSize = uint32(200); % Tamano de la poblacion inicial
        FitnessLimit = -Inf; % Parar al alcanzar este valor de fitness
        HybridFnc = {ScatterSearch.HybridFncDefault(), ...
            optimset('Algorithm', 'interior-point', 'MaxIter', 1000,...
            'Display', 'off', 'UseParallel', 'always')}; % Funcion hibrida
        OutputFcns = []; % Array de celdas con punteros a funciones a llamar despues de cada iteracion
        StallGenLimit =  uint32(2); % Numero de iteraciones en las que si no hay mejora parar.
        StallTimeLimit = Inf; % Tiempo maximo sin mejorar
        TimeLimit = Inf; % Tiempo maximo para llevar a cabo la optimizacion
        Verbose = uint32(0); % Cada cuantas iteraciones muestra informacion
    end

    properties (SetAccess = 'protected') % , GetAccess = 'protected'
        Generation = uint32(0); % Numero de Iteraciones actuales

        QSet = []; % Soluciones de calidad, Size = QSetSize X (NVar+2)
        % En QSet no solo guardamos las soluciones, sino tambien:
        %   QSet(1) => Valor de calidad (funcion FitnessFnc)
        %   QSet(2) => N. de iteraciones
        %   QSet(3,end) => Valores de la solucion

        DSet = []; % Soluciones de diversidad, Size = DSetSize X (NVar+2)
        % En DSet no solo guardamos las soluciones, sino tambien:
        %   DSet(1) => Valor de diversidad (funcion DiverFnc)
        %   DSet(2) => N. de iteraciones
        %   DSet(3,end) => Valores de la solucion
        LastCombine = uint32(0);  % Numero de iteraciones de la ultima combinacion de elementos
        NewElements = false;  % true se se han anadido nuevos elementos en la ultima combinacion
        ApplyOptimFnc = false; % Variable que indica si hay que hacer mejora o no
        NApplyOptimFnc = uint32(0); % Numero de veces que se ha optimizado
        % Variables de control para la parada
        LastAdvanceTime = []; % Momento de la ultima de mejora
        LastAdvance = uint32(0); % Iteracion de la ultima mejora
        StopCause = []; % Causa de la parada
        TolFun = 0; % Tolerancia en la funcion de fitness
        XTol = 0; % Tolerancia empleada en las comparaciones de elem.
        DTol = 0; % Tolerancia en la funcion de diversidad
    end

    properties (Dependent = true)
        x;
        fval;
    end

    methods (Static = true, Access = 'private')
        function opciones = opCreateFnc()
            opciones = gaoptimset(gaoptimset(@ga), 'Display', 'off');
            opciones.LinearConstr = struct('type', 'boundconstraints',...
                'Aineq', [], 'bineq', [], 'Aeq', [], 'lb', [], 'ub', [],...
                'beq', []);
        end
        
        function handle = OptimFncDefault()
            handle = @(fun, x, lb, ub, opt) fminsearch(fun,x,opt);
        end
        
        function handle = HybridFncDefault()
            handle = @(fun, x, lb, ub, opt) fmincon(fun, x, [], [], [],...
                [], lb, ub, [], opt);
        end
    end
    methods
        function ssObj = ScatterSearch(fitnessFnc, nvar, sizeQSet,...
                sizeDSet, populationSize, lowerBound, upperBound)
            % Construccion del objeto
            ssObj.FitnessFnc = fitnessFnc;
            ssObj.NVar = nvar;
            ssObj.PSize = populationSize;
            ssObj.QSetSize = sizeQSet;
            ssObj.DSetSize = sizeDSet;
            ssObj.LowerBound = lowerBound;
            ssObj.UpperBound = upperBound;
        end

        function [x,fval] = optimize(ss)
            % Tiempo de inicio
            tini = clock;
            
            % Reserva de memoria
            ss.QSet = zeros(ss.QSetSize, ss.NVar + 2);
            ss.DSet = zeros(ss.DSetSize, ss.NVar + 2);
            
            % Preparamos los argumentos para las funciones Creacion
            ss.CreationFnc{2}.(ss.CreationFnc{3}) = ss.PSize;
            ss.CreationFnc{2}.(ss.CreationFnc{4}) = [ss.LowerBound ; ss.UpperBound];

            % Mejora
            ss.ApplyOptimFnc = isa(ss.OptimFnc, 'function_handle');
            ss.NApplyOptimFnc = uint32(0);
            
            % Calculo de la tolerancia inicial
            ss.XTol = (ss.UpperBound-ss.LowerBound) ./ double(ss.PSize);

            % Tratamos el caso de Verbose > 0
            if (ss.Verbose > 0)
                regen = 0;
                disp('Iniciando. Generando conjunto inicial...');
            end

            % Inicializamos el conjunto de Referencia
            ss.Initiate_RSet();
            
            % Realizamos la optimizacion
            while (ss.Generation < ss.Generations)
                if (ss.Verbose>0 && mod(ss.Generation, ss.Verbose)==0)
                    fprintf('Gen. %d, fval: best=%.7g, mean=%.7g, x=',...
                        ss.Generation, ss.fval, mean(ss.QSet(:,1)));
                    disp(ss.x);
                end
                if (ss.NewElements)
                    ss.Combine_RefSet();
                else
                    ss.Update_DSet();

                    if (ss.Verbose > 0)
                        regen = regen + 1;
                        fprintf('Generacion %d, **** REGENERACION n. %d del conjunto DSet ****\n',...
                            ss.Generation, regen);
                    end

                    ss.Combine_RefSet();
                end
                
                % Examinamos las condiciones de parada
                if (isfinite(ss.FitnessLimit) && ...
                        (ss.FitnessLimit - ss.fval > ss.TolFun))
                    if (ss.Verbose > 0)
                        disp('**** Parando al alcanzar FitnessLimit ****');
                    end
                    ss.StopCause = 'FitnessLimit';
                    break;
                end    
                if (isfinite(ss.StallTimeLimit) && ...
                        etime(clock, ss.LastAdvanceTime)>ss.StallTimeLimit)
                    if (ss.Verbose > 0)
                        fprintf('**** Parando por que no hay mejora en  %.0f segundos****\n',...
                            etime(clock, ss.LastAdvanceTime)>ss.StallTimeLimit);
                    end
                    ss.StopCause = 'StallTimeLimit';
                    break;
                end
                if (isfinite(ss.StallGenLimit) && ...
                        ss.Generation-ss.LastAdvance >(ss.StallGenLimit+1))
                    if (ss.Verbose > 0)
                        fprintf('**** Parando por que no hay mejora en  %d generaciones****\n',...
                            ss.Generation - ss.LastAdvance - 1);
                    end
                    ss.StopCause = 'StallGenLimit';
                    break;
                end
                if (isfinite(ss.TimeLimit) &&...
                        etime(clock, tini)>ss.TimeLimit)
                    if (ss.Verbose > 0)
                        fprintf('**** Agotado el tiempo limite, empleados %.0f segundos ****\n',...
                            etime(clock, ss.LastAdvanceTime)>ss.StallTimeLimit);
                    end
                    ss.StopCause = 'TimeLimit';
                    break;
                end                    
            end
            if (ss.Generation >= ss.Generations)
                ss.StopCause = 'Generations';
            end
            
            % Vemos si hay que aplicar la funcion hibrida
            if (~isempty(ss.HybridFnc))
                if (ss.Verbose > 0)
                    disp('>>>>>>> Conmutando a funcion hibrida <<<<<<<<');
                end
                if (isa(ss.HybridFnc, 'function_handle')) % Handle directo
                    [x, fval] = ss.HybridFnc(ss.FitnessFnc, ss.x);
                else
                    [x, fval] = ss.HybridFnc{1}(ss.FitnessFnc, ss.x, ...
                        ss.LowerBound, ss.UpperBound, ss.HybridFnc{2});
                end
                if (ss.fval - fval > ss.TolFun)
                    la = ss.LastAdvance;
                    lat = ss.LastAdvanceTime;
                    ss.try_add_QSet(ss.corrigeLimites(x));
                    ss.LastAdvance = la;
                    ss.LastAdvanceTime = lat;
                end

            end
            
            x = ss.x;
            fval = ss.fval;
            if (ss.Verbose > 0)
                fprintf('FINAL: Generation %d, fval=%.15g, x=',...
                    ss.Generation, ss.fval);
                disp(ss.x);
            end
        end

        function x = get.x(ss)
            if (~isempty(ss.QSet))
                x = ss.QSet(1, 3:end);
            else
                x = NaN;
            end
        end

        function fval = get.fval(ss)
            if (~isempty(ss.QSet))
                fval = ss.QSet(1,1);
            else
                fval = NaN;
            end
        end

        function set.QSetSize(ss, sizeQSet)
            if (sizeQSet>ss.PSize/2)
                ss.QSetSize = uint32(ss.PSize/2);
            else
                ss.QSetSize = uint32(sizeQSet);
            end
        end

        function set.DSetSize(ss, sizeDSet)
            if (sizeDSet>ss.PSize/2)
                ss.DSetSize = uint32(ss.PSize/2);
            else
                ss.DSetSize = uint32(sizeDSet);
            end
        end

        function set.PSize(ss, sizeP)
            if (sizeP>2)
                ss.PSize = uint32(sizeP);
                ss.DSetSize = ss.DSetSize;
                ss.QSetSize = ss.QSetSize;
            end
        end

        function set.NVar(ss, nvar)
            if (nvar>0)
                ss.NVar = uint8(nvar);
                ss.LowerBound = ss.LowerBound;
                ss.UpperBound = ss.UpperBound;
            end
        end

        function set.LowerBound(ss, lb)
            if (~isempty(lb))
                if (length(lb)<ss.NVar)
                    ss.LowerBound = ones(1, ss.NVar) * lb(1);
                else
                    ss.LowerBound = lb(1:ss.NVar);
                end
            else
                ss.LowerBound = lb;
            end
        end
        
        function set.UpperBound(ss, ub)
            if (~isempty(ub))
                if (length(ub)<ss.NVar)
                    ss.UpperBound = ones(1, ss.NVar) * ub(1);
                else
                    ss.UpperBound = ub(1:ss.NVar);
                end
            else
                ss.UpperBound = ub;
            end
        end
        
        function set.StallGenLimit(ss, sgl)
            if (ss.StallGenLimit ~= sgl)
                if (isfinite(sgl))
                    ss.StallGenLimit = uint32(sgl);
                else
                    ss.StallGenLimit = sgl;
                end
            end
        end
    end

    methods (Access = 'protected')
        function Initiate_RSet(ss)
            % Consideramos que esta es la primera generacion
            ss.Generation = uint32(1);

            % Y por supuesto vamos a tener nuevos elementos
            ss.NewElements = true;

            % Debemos reservar la memoria para las soluciones
            solutions = zeros(ss.PSize, ss.NVar + 2);
            % En soluciones guardamos, en la posicion (1) el fitness y 
            % en la posicion 2 la diversidad y a continuacion los valores 
            % de la solucion (3:end).

            % Ahora generamos los valores para la poblacion de trabajo
            % inicial, para ello usaremos una funcion de creacion
            solutions(:,3:end) = ss.CreationFnc{1}(ss.NVar,...
                ss.FitnessFnc, ss.CreationFnc{2});

            % Calculamos el fitness de cada solucion
            for l=1:size(solutions,1)
                solutions(l,1)=ss.FitnessFnc(solutions(l, 3:end));
            end
            % Ahora ordenamos los valores en funcion del fitness (de menor
            % a mayor)
            solutions = sortrows(solutions, 1);
            % Y le aplicamos una mejora a los 10 mejores antes de pasarlos
            % al conjunto de referencia si exite
            if (ss.ApplyOptimFnc)
                for ind=1:ss.QSetSize
                    % ss.OptimFnc{2}.(ss.OptimFnc{4}) = ...
                    % solutions(ind,2:end);
                    [ss.QSet(ind,3:end),ss.QSet(ind,1)] =...
                        ss.OptimFncSimple(solutions(ind,3:end),...
                        solutions(ind,1));
                end
                ss.QSet = sortrows(ss.QSet, 1);
            else
                ss.QSet(:, 3:end) = solutions(1:ss.QSetSize, 3:end);
                ss.QSet(:, 1) = solutions(1:ss.QSetSize, 1); % Fitness
            end
            ss.QSet(:, 2) = ss.Generation; % Generation
            ss.Update_TolFun(); % Actualizamos la tolerancia
            
            % Creamos el conjuto de diversidad
            ss.Update_DSet(solutions(ss.QSetSize+1:end,:));
            
            ss.LastCombine = uint32(0);
        end

        function Update_DSet(ss, solutions)
            % Funcion interna. Actualiza el conjunto de referencia
            % dedicado a la diversidad. Si no se le pasa un conjunto
            % de soluciones pregenerado, lo genera el mismo.
            if (nargin==1 || isempty(solutions))
                % Aumentamos el valor de la generacion
                ss.Generation = ss.Generation + 1;
                
                % Aumentamos un poco la tolerancia
                ss.XTol = ss.XTol / 2;
                
                % Reseteamos la generacion del conjunto QSet para permitir
                % nuevas combinaciones
                ss.QSet(:,2) = ss.Generation;
                
                % Debemos reservar la memoria para las soluciones
                solutions = zeros(ss.PSize, ss.NVar + 2);
                % En soluciones guardamos, en la posicion (1) el fitness,
                %  la distancia minima al resto en la posicion (2),
                %  y a continuacion los valores de la solucion (3:end).

                % Ahora generamos los valores para la poblacion de trabajo
                % inicial, para ello usaremos una funcion de creacion
                ss.CreationFnc{2}.(ss.CreationFnc{3}) = ss.PSize;
                ss.CreationFnc{2}.(ss.CreationFnc{4}) = [ss.LowerBound ; ss.UpperBound];
                solutions(:,3:end) = ss.CreationFnc{1}(ss.NVar,...
                    ss.FitnessFnc, ss.CreationFnc{2});
                
                for l=1:size(solutions,1)
                    solutions(l,1)=ss.FitnessFnc(solutions(l, 3:end));
                end
            end

            % Calculamos la diversidad a la solucion Q
            for l=1:size(solutions,1)
                solutions(l,2)=ss.DiverFnc(solutions(l, 3:end),...
                    ss.QSet(:,3:end));
            end

            % Primero ordenamos por fitness
            solutions = sortrows(solutions, 1);
            % Nos quedamos con la mitad que tenga mejor fitness y            
            % del resto las DSetSize/2 con mejor diversidad
            solutionsQ = solutions(1:round(size(solutions,1)/2),:); 
            
            % Ahora ordenamos por diver
            solutions = sortrows(solutions, -2);
            solutionsD = solutions(1:ss.DSetSize/2,:);
            
            % Y unimos ambas
            solutions = [solutionsQ ; solutionsD];
            
            % Vamos seleccionando de uno en uno los mas diversos,
            % actualizando la diversidad del resto respecto a este.
            for l=1:ss.DSetSize
                % Ordenamos los valores en funcion de la diversidad
                % (de mayor a menor)
                solutions = sortrows(solutions, -2);
                ss.DSet(l, 3:end) = solutions(1, 3:end); % Solucion
                ss.DSet(l, 2) = ss.Generation; % Iteracion
                ss.DSet(l, 1) = solutions(1, 2); % Diversidad

                if (l<ss.DSetSize)
                    % Actualizamos los valores de diversidad teniendo en cuenta
                    % el valor que hemos anexado.
                    for ll=1:size(solutions,1)
                        solutions(ll,2)=min([solutions(ll, 2),...
                            ss.DiverFnc(solutions(ll,3:end),...
                            ss.DSet(l,3:end))]);
                    end
                end
            end

            % Actualizamos la diversidad por completo del conjuto de
            % referencia de diversidad (DSet), sin incluir el QSet ya
            % que lo hemos tratado previamente.
            ss.Update_DSet_Diver(false);
        end

        function Update_DSet_Diver(ss, completo)
            %Update_DSet_Diver permite actualizar el conjuto DSet en
            %cuanto a diversidad.
            % Update_DSet_Diver(ss, completo) => completo=true indica
            % recalcular completamente la diversidad volviendo a medirla
            % respecto al conjunto QSet.
            if (nargin>=2 && completo)
                % Primero calculamos la diversidad de todas las soluciones
                % del conjunto RefSet respecto al conjunto QSet, ya que
                % se ha seleccionado un recalculo completo.
                for l=1:ss.DSetSize
                    ss.DSet(l,1)=ss.DiverFnc(ss.DSet(l, 3:end),...
                        ss.QSet(:,3:end));
                end
            end

            % Actualizamos la diversidad del conjunto DSet completo
            ss.DSet(1, 1) = min([ss.DSet(1, 1),...
                ss.DiverFnc(ss.DSet(1, 3:end),...
                ss.DSet(2:end, 3:end))]);
            for l=2:ss.DSetSize-1
                ss.DSet(l, 1) = min([ss.DSet(l, 1), ss.DiverFnc(...
                    ss.DSet(l,3:end), ss.DSet(1:l-1,3:end)),...
                    ss.DiverFnc(ss.DSet(l,3:end),...
                    ss.DSet(l+1:end,3:end))]);
            end
            ss.DSet(end, 1) = min([ss.DSet(end, 1),...
                ss.DiverFnc(ss.DSet(end, 3:end),...
                ss.DSet(1:ss.DSetSize-1, 3:end))]);
            
            % Por ultimo ordenamos el conjunto de diversidad de mas a menos
            ss.DSet = sortrows(ss.DSet, -1);
            
            % Y actualizamos la tolerancia
            ss.DTol = (ss.DSet(1,1)-ss.DSet(end,1))/double(ss.DSetSize/2);
        end

        function Combine_RefSet(ss)
            ss.NewElements = false;

            % Extraemos cuantos elementos nuevos hay en cada conjunto
            nq =  sum(ss.QSet(:,2)>ss.LastCombine);
            nd =  sum(ss.DSet(:,2)>ss.LastCombine);

            if (nq>=2)
                qxqSize = 4*(factorial(nq)/(factorial(nq-2)*2)+nq*(ss.QSetSize-nq));
            else
                qxqSize = 4*nq*(ss.QSetSize-nq);
            end
            
            if (nd>=2)
                dxdSize = 2*(factorial(nd)/(factorial(nd-2)*2)+ nd*(ss.DSetSize-nd));
            else
                dxdSize = 2*(nd*(ss.DSetSize-nd));
            end
            
            total_size=...
                qxqSize+...
                3*(nq*ss.DSetSize + nd*ss.QSetSize - nq*nd)+...
                dxdSize;
            
            np = zeros(total_size,ss.NVar);
            np_size = 1;

            % Combinamos primero entre los elementos del QSet
            % que en teoria debe contener mucha informacion sobre
            % la solucion optima.
            for ii=1:ss.QSetSize
                for jj=ii+1:ss.QSetSize
                    if (ss.QSet(ii, 2) > ss.LastCombine || ...
                            ss.QSet(jj, 2) > ss.LastCombine)
                        np(np_size:np_size+3,:) = ss.CombineFnc([
                            ss.QSet(ii,3:end); ss.QSet(jj,3:end)], 4);
                        np_size = np_size + 4;
                    end
                end
            end

            % Combinamos a continuacion los elementos del QSet con los
            % del DSet, para garantizar explorar guiados por los mejores
            for ii=1:ss.QSetSize
                for jj=1:ss.DSetSize
                    if (ss.QSet(ii, 2) > ss.LastCombine || ...
                            ss.DSet(jj, 2) > ss.LastCombine)
                        np(np_size:np_size+2,:) = ss.CombineFnc([
                            ss.QSet(ii,3:end); ss.DSet(jj,3:end)], 3);
                        np_size = np_size + 3;
                    end
                end
            end

            % Por ultimo combinamos entre si los elementos del DSet
            % buscando sobre todo mejorar la diversidad
            for ii=1:ss.DSetSize
                for jj=ii+1:ss.DSetSize
                    if (ss.DSet(ii, 2) > ss.LastCombine || ...
                            ss.DSet(jj, 2) > ss.LastCombine)
                        np(np_size:np_size+1,:) = ss.CombineFnc([
                            ss.DSet(ii,3:end); ss.DSet(jj,3:end)], 2);
                        np_size = np_size + 2;
                    end
                end
            end
            
            if (mod(ss.Generation, ss.Verbose)==0)
                fprintf('* %d cand. ',np_size-1);
                if (total_size ~= (np_size-1))
                    fprintf('Fallo al calcular total_size (=%d)\n',...
                        total_size);
                end
            end
    
            % Actualizamos las variables de control
            ss.LastCombine = ss.Generation;
            ss.Generation = ss.Generation + 1;

            % Corregimos las soluciones por si acaso
            np=ss.corrigeLimites(np(1:np_size-1, :));
            
            % Vemos si alguna de las soluciones generadas nos sirve
            for ii=1:np_size-1
                ss.try_add_QSet(np(ii,:));
                ss.try_add_DSet(np(ii,:));
            end
        end

        function try_add_QSet(ss, sol)
            %try_add_QSet. Intenta anadir una solucion al conjunto de
            % referencia QSet, aplicando la funcion de mejora.
            
            if (ss.ApplyOptimFnc && rand()<ss.OptimFactor)
                [sol, fitness] = ss.OptimFncSimple(sol);
            else
                fitness = ss.FitnessFnc(sol);
            end

            if ((ss.QSet(end,1)-fitness>ss.TolFun)&&(ss.is_new(sol)))
                % Es mejor que el ultimo y no existe previamente, lo 
                % incorporamos en el conjunto de referencia QSet
                ss.NewElements = true; % Indicamos que tenemos nuevo elem.
                ss.LastAdvance = ss.Generation; % Generacion de mejora
                ss.LastAdvanceTime = clock; % Momento de esta mejora
                ss.QSet(end, 1) = fitness; % Valor de fitnes
                ss.QSet(end, 2) = ss.Generation; % Generacion
                ss.QSet(end, 3:end) = sol; % Solucion
                ss.QSet = sortrows(ss.QSet, 1); % Reordenamos QSet
                ss.Update_TolFun();
                ss.Update_DSet_Diver(true); % Recalculamos Diver
            end
        end

        function try_add_DSet(ss, sol)
            %try_add_DSet. Intenta incorporar una elem. al conjunto de
            % referencia DSet, sin aplicar funcion de mejora (interesa
            % la diversidad y no la bondad).
            diver = min([ss.DiverFnc(sol, ss.QSet(:,3:end)),...
                ss.DiverFnc(sol, ss.DSet(:,3:end))]);

            if (diver - ss.DSet(end, 1) > ss.DTol)
                % Mejoramos la diversidad peor del conjunto DSet
                ss.NewElements = true; % Indicamos que tenemos nuevo elem.
                ss.DSet(end, 3:end) = sol; % Guardamos la sol
                ss.DSet(end, 2) = ss.Generation;
                ss.DSet(end, 1) = diver;
                ss.Update_DSet_Diver(true); % Recalculamos Diver
            end
        end

        function [solfeasible] = corrigeLimites(ss, sol)
            solfeasible = corrigelim(sol, ss.LowerBound, ss.UpperBound);
        end
    
        function [isnew] = is_new(ss, sol)
            % Comprueba si la solucion existe en el conjunto QSet
            isnew = all(sum(abs(ss.QSet(:, 3:end)...
                - repmat(sol, ss.QSetSize, 1)) >...
                repmat(ss.XTol, ss.QSetSize, 1), 2));
        end
        
        function [nx, nfval] = OptimFncSimple(ss, x, fval)
            ss.NApplyOptimFnc = ss.NApplyOptimFnc + 1;
            [nx, nfval] = ss.OptimFnc(ss.FitnessFnc, x,...
                ss.LowerBound, ss.UpperBound, ss.OptimFncOpt);
            nx = ss.corrigeLimites(nx);
            if (nargin>2 && isfinite(fval) && fval-nfval<ss.TolFun) 
                % No mejoramos, posiblemente al corregir los limites
                nx = x;
                nfval = fval;
            end
        end
        
        function Update_TolFun(ss)
            ss.TolFun=(ss.QSet(end,1)-ss.QSet(1,1))/double(ss.QSetSize/2);
        end
    end
end
