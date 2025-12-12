package com.emts.controlpid;
import javafx.scene.chart.XYChart;

/**
 * محاكاة النظام: G(s) = 4/(s² + 2s)
 * المواصفات: ζ = 0.5, ωn = 2 rad/s
 * الدخل: r(t) = 1 (step response)
 */
public class Simulation {

    public static class SimulationResult {
        public final double[] time;
        public final double[] outputBefore;
        public final double[] outputAfter;
        public final double[] controlSignal;
        public final java.util.List<XYChart.Data<Number, Number>> beforeSeries;
        public final java.util.List<XYChart.Data<Number, Number>> afterSeries;

        public SimulationResult(int n) {
            time = new double[n];
            outputBefore = new double[n];
            outputAfter = new double[n];
            controlSignal = new double[n];
            beforeSeries = new java.util.ArrayList<>();
            afterSeries = new java.util.ArrayList<>();
        }
    }

    /**
     * تشغيل المحاكاة
     * @param kp معامل التناسب
     * @param ki معامل التكامل
     * @param kd معامل الاشتقاق
     */
    public static SimulationResult run(double kp, double ki, double kd) {
        double dt = 0.01;  // خطوة الوقت
        int N = 2000;      // عدد الخطوات (20 ثانية)

        SimulationResult result = new SimulationResult(N);

        // حالات النظام المفتوح: x1=y, x2=dy/dt
        double x1_open = 0.0;
        double x2_open = 0.0;

        // حالات النظام المغلق
        double x1_closed = 0.0;
        double x2_closed = 0.0;

        // متغيرات PID
        PID pidController = new PID(kp, ki, kd);

        // الدخل المرجعي
        double r = 1.0; // step input

        for (int k = 0; k < N; k++) {
            double t = k * dt;

            // ========== النظام المفتوح (بدون PID) ==========
            // الدخل المباشر هو الخطوة
            double u_open = r;
            double y_open = x1_open;

            // تحديث حالة النظام: dx1 = x2, dx2 = -2*x2 + 4*u
            // من: G(s) = 4/(s² + 2s) → ṳ1 = x2, ẋ2 = -2x2 + 4u
            double dx1_open = x2_open;
            double dx2_open = -2.0 * x2_open + 4.0 * u_open;

            x1_open += dx1_open * dt;
            x2_open += dx2_open * dt;

            // ========== النظام المغلق (مع PID) ==========
            double y_closed = x1_closed;
            double error = r - y_closed;

            // حساب إشارة التحكم من PID
            double u_closed = pidController.update(error, dt);

            // تحديث حالة النظام المغلق
            double dx1_closed = x2_closed;
            double dx2_closed = -2.0 * x2_closed + 4.0 * u_closed;

            x1_closed += dx1_closed * dt;
            x2_closed += dx2_closed * dt;

            // حفظ النتائج
            result.time[k] = t;
            result.outputBefore[k] = y_open;
            result.outputAfter[k] = y_closed;
            result.controlSignal[k] = u_closed;

            result.beforeSeries.add(new XYChart.Data<>(t, y_open));
            result.afterSeries.add(new XYChart.Data<>(t, y_closed));
        }

        return result;
    }
}