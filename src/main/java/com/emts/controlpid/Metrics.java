package com.emts.controlpid;

/**
 * *******************************************************************
 * File: null.java
 * Package: com.emts.controlpid
 * Project: eMTS Smart Attendance System
 * © ٢٠٢٥ Mohamed Taha Elmeligy - eMTS (e Modern Tech Solutions)
 * This file is part of the eMTS Smart Attendance System.
 * Created on: 26/11/2025
 * Port Number: 8083
 * *******************************************************************
 */
public class Metrics {
    private final double[] time;
    private final double[] output;
    private static final double setPoint = 1.0;

    public Metrics(double[] time, double[] output) {
        this.time = time;
        this.output = output;
    }

    public double getRiseTime() {
        for (int i = 0; i < output.length; i++) {
            if (output[i] >= 0.9 * setPoint) {
                return time[i];
            }
        }
        return time[time.length - 1];
    }

    public double getSettlingTime() {
        double tolerance = 0.02 * setPoint;
        for (int i = output.length - 1; i >= 0; i--) {
            if (Math.abs(output[i] - setPoint) > tolerance) {
                return time[i];
            }
        }
        return time[0];
    }

    public double getOvershoot() {
        double max = getMaxOutput();
        return Math.max(0, ((max - setPoint) / setPoint) * 100);
    }

    public double getPeakTime() {
        double max = getMaxOutput();
        for (int i = 0; i < output.length; i++) {
            if (Math.abs(output[i] - max) < 0.001) {
                return time[i];
            }
        }
        return 0;
    }

    public double getSteadyStateError() {
        double avgEnd = 0;
        int sampleSize = (int)(output.length * 0.1);
        for (int i = output.length - sampleSize; i < output.length; i++) {
            avgEnd += output[i];
        }
        avgEnd /= sampleSize;
        return Math.abs(setPoint - avgEnd);
    }

    private double getMaxOutput() {
        double max = output[0];
        for (double v : output) {
            max = Math.max(max, v);
        }
        return max;
    }

    public String format() {
        return String.format(
                "Rise Time: %.3f s\n" +
                        "Peak Time: %.3f s\n" +
                        "Overshoot: %.2f %%\n" +
                        "Settling Time: %.3f s\n" +
                        "Steady-State Error: %.4f",
                getRiseTime(), getPeakTime(), getOvershoot(), getSettlingTime(), getSteadyStateError()
        );
    }
}