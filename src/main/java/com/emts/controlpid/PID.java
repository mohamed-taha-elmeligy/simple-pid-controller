package com.emts.controlpid;

/**
 * فئة التحكم PID محسّنة
 * مع معالجة صحيحة للـ derivative filtering و anti-windup
 */
public class PID {
    private final double kp;
    private final double ki;
    private final double kd;

    private double integral = 0.0;
    private double prevError = 0.0;
    private double prevDerivative = 0.0;

    private double uMax = 100.0;
    private double uMin = -100.0;
    private boolean antiWindup = true;
    private double filterAlpha = 0.2;  // معامل تصفية المشتقة (0.1 - 0.5)

    public PID(double kp, double ki, double kd) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
    }

    /**
     * حساب معاملات PID من المواصفات
     * ζ = 0.5 (damping ratio)
     * ωn = 2 rad/s (natural frequency)
     * للنظام: G(s) = 4/(s² + 2s)
     */
    public static PID fromSpecifications(double zeta, double omegaN) {
        // استخدام pole placement للنظام من الدرجة الثانية
        // لتحقيق المواصفات المطلوبة
        double kp = 2.0 * zeta * omegaN;  // ≈ 2.0
        double ki = omegaN * omegaN;       // ≈ 4.0
        double kd = (2.0 * zeta * omegaN - 2.0) / 2.0;  // ≈ 0.5

        return new PID(kp, ki, kd);
    }

    public void setLimits(double min, double max) {
        uMin = min;
        uMax = max;
    }

    public void setAntiWindup(boolean enabled) {
        antiWindup = enabled;
    }

    public void setFilterAlpha(double alpha) {
        this.filterAlpha = Math.max(0.01, Math.min(0.5, alpha));
    }

    /**
     * تحديث المتحكم مع خطأ معين وخطوة زمنية
     * @param error الخطأ الحالي (مرجع - خرج)
     * @param dt خطوة الوقت
     * @return إشارة التحكم
     */
    public double update(double error, double dt) {
        // ===== حساب الحد التناسبي =====
        double proportional = kp * error;

        // ===== حساب الحد التكاملي =====
        integral += error * dt;

        // Anti-windup: منع تراكم التكامل إذا تجاوز الحد الأقصى
        if (antiWindup && ki != 0) {
            double maxIntegral = uMax / ki;
            double minIntegral = uMin / ki;
            integral = Math.max(minIntegral, Math.min(maxIntegral, integral));
        }

        double integralTerm = ki * integral;

        // ===== حساب الحد الاشتقاقي =====
        double derivative = (error - prevError) / (dt + 1e-9);

        // تطبيق low-pass filter على المشتقة لتقليل الضوضاء
        // y_filtered = α*y_new + (1-α)*y_old
        derivative = filterAlpha * derivative + (1.0 - filterAlpha) * prevDerivative;

        double derivativeTerm = kd * derivative;

        // ===== حساب الخرج النهائي =====
        double u = proportional + integralTerm + derivativeTerm;

        // تطبيق التشبع (Saturation)
        u = Math.max(uMin, Math.min(uMax, u));

        // حفظ القيم للخطوة التالية
        prevError = error;
        prevDerivative = derivative;

        return u;
    }

    public void reset() {
        integral = 0.0;
        prevError = 0.0;
        prevDerivative = 0.0;
    }

    // Getters للمعاملات
    public double getKp() { return kp; }
    public double getKi() { return ki; }
    public double getKd() { return kd; }
}