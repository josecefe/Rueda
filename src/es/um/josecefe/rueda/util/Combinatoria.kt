package es.um.josecefe.rueda.util

tailrec fun factorial(n: Long, a: Long = 1): Long = if (n <= 1) a else factorial(n - 1, a * n)

fun permutations(n: Long, r: Long): Long = factorial(n) / factorial(n - r)

fun combinations(n: Long, r: Long): Long = permutations(n, r) / factorial(r)