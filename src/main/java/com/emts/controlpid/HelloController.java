package com.emts.controlpid;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    @FXML private TextField tfKp;
    @FXML private TextField tfKi;
    @FXML private TextField tfKd;
    @FXML private Button btnRun;
    @FXML private LineChart<Number, Number> chart;
    @FXML private Label lblMetrics;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;

    private Simulation.SimulationResult lastResult;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== Controller Initialization ===");
        System.out.println("tfKp: " + (tfKp != null ? "✓ Loaded" : "✗ NULL"));
        System.out.println("chart: " + (chart != null ? "✓ Loaded" : "✗ NULL"));
        System.out.println("lblMetrics: " + (lblMetrics != null ? "✓ Loaded" : "✗ NULL"));

        try {
            if (progressBar != null) {
                progressBar.setVisible(false);
            }

            if (lblStatus != null) {
                lblStatus.setText("Ready");
            }

            // حساب معاملات PID من المواصفات
            // ζ = 0.5, ωn = 2 rad/s
            calculateOptimalGains();

        } catch (Exception e) {
            System.err.println("Initialize error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * حساب معاملات PID المثلى من المواصفات
     * ζ = 0.5 (damping ratio)
     * ωn = 2 rad/s (natural frequency)
     */
    private void calculateOptimalGains() {
        double zeta = 0.5;
        double omegaN = 2.0;

        // من نظرية التحكم:
        // Kp ≈ 2*ζ*ωn = 2*0.5*2 = 2.0
        // Ki ≈ ωn² = 2² = 4.0
        // Kd ≈ (2*ζ*ωn - a)/b حيث a, b معاملات النظام

        double kp = 2.0;    // قيمة محسوبة
        double ki = 4.0;    // قيمة محسوبة
        double kd = 0.5;    // قيمة محسوبة

        tfKp.setText(String.valueOf(kp));
        tfKi.setText(String.valueOf(ki));
        tfKd.setText(String.valueOf(kd));

        System.out.println(String.format(
                "Optimal PID Gains (ζ=%.1f, ωn=%.1f):\n Kp=%.2f, Ki=%.2f, Kd=%.2f",
                zeta, omegaN, kp, ki, kd
        ));
    }

    @FXML
    private void simulate() {
        try {
            double kp = Double.parseDouble(tfKp.getText());
            double ki = Double.parseDouble(tfKi.getText());
            double kd = Double.parseDouble(tfKd.getText());

            btnRun.setDisable(true);

            System.out.println(String.format(
                    "Running simulation with Kp=%.2f, Ki=%.2f, Kd=%.2f",
                    kp, ki, kd
            ));

            Task<Simulation.SimulationResult> task = new Task<>() {
                @Override
                protected Simulation.SimulationResult call() {
                    return Simulation.run(kp, ki, kd);
                }
            };

            task.setOnRunning(e -> {
                progressBar.setVisible(true);
                lblStatus.setText("Running simulation...");
            });

            task.setOnSucceeded(e -> {
                lastResult = task.getValue();
                updateChart(lastResult);
                calculateMetrics(lastResult);
                progressBar.setVisible(false);
                lblStatus.setText("✓ Complete");
                btnRun.setDisable(false);
            });

            task.setOnFailed(e -> {
                Throwable ex = task.getException();
                String errorMsg = ex != null ? ex.getMessage() : "Unknown error";
                lblMetrics.setText("Error: " + errorMsg);
                progressBar.setVisible(false);
                lblStatus.setText("✗ Error");
                btnRun.setDisable(false);
                ex.printStackTrace();
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();

        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter valid numbers for Kp, Ki, Kd");
            btnRun.setDisable(false);
        }
    }

    private void updateChart(Simulation.SimulationResult result) {
        chart.getData().clear();

        // تكوين المحاور
        NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();

        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(20);  // 20 ثانية
        xAxis.setTickUnit(2);

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(2.5);  // مع هامش للـ overshoot
        yAxis.setTickUnit(0.25);

        // إنشاء السلاسل البيانية
        XYChart.Series<Number, Number> seriesBefore = new XYChart.Series<>();
        seriesBefore.setName("Without PID (Open-Loop)");
        seriesBefore.getData().addAll(result.beforeSeries);

        XYChart.Series<Number, Number> seriesAfter = new XYChart.Series<>();
        seriesAfter.setName("With PID (Closed-Loop)");
        seriesAfter.getData().addAll(result.afterSeries);

        chart.getData().addAll(seriesBefore, seriesAfter);
    }

    private void calculateMetrics(Simulation.SimulationResult result) {
        Metrics metrics = new Metrics(result.time, result.outputAfter);
        lblMetrics.setText(metrics.format());

        // طباعة المقاييس في console أيضاً
        System.out.println("\n=== Performance Metrics ===");
        System.out.println(metrics.format());
    }

    @FXML
    private void saveChart() {
        if (lastResult == null) {
            showAlert("No Data", "Please run a simulation first");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chart as Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Files", "*.png")
        );
        fileChooser.setInitialFileName("pid_response.png");

        File file = fileChooser.showSaveDialog(btnRun.getScene().getWindow());
        if (file != null) {
            try {
                WritableImage image = chart.snapshot(null, null);
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                showAlert("Success", "Chart saved to: " + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert("Error", "Failed to save chart: " + e.getMessage());
            }
        }
    }

    @FXML
    private void exportCSV() {
        if (lastResult == null) {
            showAlert("No Data", "Please run a simulation first");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Data as CSV");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("pid_data.csv");

        File file = fileChooser.showSaveDialog(btnRun.getScene().getWindow());
        if (file != null) {
            try {
                StringBuilder csv = new StringBuilder();
                csv.append("Time,Before_PID,After_PID,Control_Signal\n");
                for (int i = 0; i < lastResult.time.length; i++) {
                    csv.append(String.format("%.4f,%.6f,%.6f,%.6f\n",
                            lastResult.time[i],
                            lastResult.outputBefore[i],
                            lastResult.outputAfter[i],
                            lastResult.controlSignal[i]
                    ));
                }
                java.nio.file.Files.write(file.toPath(), csv.toString().getBytes());
                showAlert("Success", "Data exported to: " + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert("Error", "Failed to export CSV: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}