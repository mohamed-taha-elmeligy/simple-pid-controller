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
public class PID {
    private final double kp;
    private final double ki;
    private final double kd;
    private double integral = 0;
    private double prevError = 0;
    private double uMax = 100;
    private double uMin = -100;
    private boolean antiWindup = true;

    public PID(double kp, double ki, double kd) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
    }

    public void setLimits(double min, double max) {
        uMin = min;
        uMax = max;
    }

    public void setAntiWindup(boolean enabled) {
        antiWindup = enabled;
    }

    public double update(double error, double dt) {
        integral += error * dt;

        // Anti-windup: clamp integral term
        if (antiWindup) {
            double maxIntegral = uMax / (ki + 1e-6);
            integral = Math.max(-maxIntegral, Math.min(maxIntegral, integral));
        }

        double derivative = (error - prevError) / (dt + 1e-9);

        // Low-pass filter on derivative
        double alpha = 0.1;
        derivative = alpha * derivative + (1 - alpha) * (prevError / dt);

        double u = kp * error + ki * integral + kd * derivative;

        // Saturation
        u = Math.max(uMin, Math.min(uMax, u));

        prevError = error;
        return u;
    }

    public void reset() {
        integral = 0;
        prevError = 0;
    }
}