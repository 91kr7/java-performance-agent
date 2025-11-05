package com.testapp;

public class BaseComputeServer extends AbstractServer {

    @Override
    protected void calculateFibonacci(StringBuilder report) {
        int n = 32;
        long fibStart = System.nanoTime();
        int fibResult = fibonacci(n);
        long fibElapsed = System.nanoTime() - fibStart;
        report.append("Fibonacci(" + n + ") = " + fibResult + " [" + (fibElapsed / 1_000_000) + " ms]\n");
    }

    private int fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}
