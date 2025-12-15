package com.excluded.testapp;

import com.included.BaseComputeServer;

public class ExcludedClass {
    public void calculateFibonacci(BaseComputeServer holder, StringBuilder report) {
        holder.calculateFibonacci(report);
    }
}
