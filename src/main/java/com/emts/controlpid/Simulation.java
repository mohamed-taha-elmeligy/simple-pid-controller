package com.emts.controlpid;
import javafx.scene.chart.XYChart;

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

public class Simulation {

    public static class SimulationResult {
        public final double[] time;
        public final double[] outputBefore;
        public final double[] outputAfter;
        public final java.util.List<XYChart.Data<Number, Number>> beforeSeries;
        public final java.util.List<XYChart.Data<Number, Number>> afterSeries;

        public SimulationResult(int n) {
            time = new double[n];
            outputBefore = new double[n];
            outputAfter = new double[n];
            beforeSeries = new java.util.ArrayList<>();
            afterSeries = new java.util.ArrayList<>();
        }
    }

    // run with only this plant G(s)=4/(s(s+2))
    public static SimulationResult run(double kp, double ki, double kd) {

        double dt = 0.01;
        int N = 2000;

        SimulationResult result = new SimulationResult(N);

        // states for open-loop and closed-loop
        double x1_before = 0, x2_before = 0;
        double x1_after  = 0, x2_after  = 0;

        double integ = 0;
        double prevErr = 0;

        for (int k = 0; k < N; k++) {
            double t = k * dt;
            double r = 1.0; // step input

            // ===== BEFORE PID (Open-Loop) =====
            double u_before = r; // <-- هنا الصلاح: استخدم الإشارة step كدخل
            double y_before = x1_before;

            // plant state updates: dx1 = x2, dx2 = -2*x2 + 4*u
            double dx1_b = x2_before;
            double dx2_b = -2.0 * x2_before + 4.0 * u_before;

            x1_before += dx1_b * dt;
            x2_before += dx2_b * dt;

            // ===== PID (compute control using closed-loop output) =====
            double y_closed = x1_after;
            double e = r - y_closed;

            integ += e * dt;
            double deriv = (e - prevErr) / dt;
            prevErr = e;

            double u = kp * e + ki * integ + kd * deriv;

            // ===== AFTER PID (Closed-Loop) =====
            double dx1_a = x2_after;
            double dx2_a = -2.0 * x2_after + 4.0 * u;

            x1_after += dx1_a * dt;
            x2_after += dx2_a * dt;

            double y_after = x1_after;

            // store results
            result.time[k] = t;
            result.outputBefore[k] = y_before;
            result.outputAfter[k]  = y_after;

            result.beforeSeries.add(new XYChart.Data<>(t, y_before));
            result.afterSeries.add(new XYChart.Data<>(t, y_after));
        }

        return result;
    }
}