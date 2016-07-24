function [ dist ] = minqdist( vec, matriz )
%MINQDIST Calcula la distancia cuadratica menor del vector al conjunto dado
    dist = min(sum((repmat(vec, size(matriz,1), 1) - matriz) .^ 2, 2));
end
