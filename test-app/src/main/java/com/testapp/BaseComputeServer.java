package com.testapp;

public class BaseComputeServer extends AbstractServer {

    @Override
    protected void calculateFibonacci(StringBuilder report) {
        int n = 25;
        int numRuns = 1;
        long[] executionTimes = new long[numRuns];
        long totalTime = 0;
        int fibResult = 0;

        report.append("Running Fibonacci(" + n + ") " + numRuns + " times...\n");

        for (int i = 0; i < numRuns; i++) {
            long fibStart = System.nanoTime();
            fibResult = fibonacci(n);
            long fibElapsed = System.nanoTime() - fibStart;
            executionTimes[i] = fibElapsed;
            totalTime += fibElapsed;
            report.append("Run " + (i + 1) + ": " + (fibElapsed / 1_000_000) + " ms\n");
        }

        long averageTime = totalTime / numRuns;
        report.append("\nFibonacci(" + n + ") = " + fibResult + "\n");
        report.append("Average execution time: " + (averageTime / 1_000_000) + " ms\n");
    }

    private int fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}
