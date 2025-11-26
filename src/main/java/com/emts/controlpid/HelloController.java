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

    @FXML
    public void initialize() {
        System.out.println("=== Initialize Called ===");
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
        } catch (Exception e) {
            System.err.println("Initialize error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void simulate() {
        try {
            double kp = Double.parseDouble(tfKp.getText());
            double ki = Double.parseDouble(tfKi.getText());
            double kd = Double.parseDouble(tfKd.getText());
            btnRun.setDisable(true);

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
                lblMetrics.setText("Error: " + task.getException().getMessage());
                progressBar.setVisible(false);
                lblStatus.setText("✗ Error");
                btnRun.setDisable(false);
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

        NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();

        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(10);
        xAxis.setTickUnit(1);

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(2);
        yAxis.setTickUnit(0.5);

        XYChart.Series<Number, Number> seriesBefore = new XYChart.Series<>();
        seriesBefore.setName("Without PID");
        seriesBefore.getData().addAll(result.beforeSeries);

        XYChart.Series<Number, Number> seriesAfter = new XYChart.Series<>();
        seriesAfter.setName("With PID");
        seriesAfter.getData().addAll(result.afterSeries);

        chart.getData().addAll(seriesBefore, seriesAfter);
    }

    private void calculateMetrics(Simulation.SimulationResult result) {
        Metrics metrics = new Metrics(result.time, result.outputAfter);
        lblMetrics.setText(metrics.format());
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
                csv.append("Time,Before_PID,After_PID\n");
                for (int i = 0; i < lastResult.time.length; i++) {
                    csv.append(String.format("%.4f,%.6f,%.6f\n",
                            lastResult.time[i],
                            lastResult.outputBefore[i],
                            lastResult.outputAfter[i]
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

    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  {@code null} if the location is not known.
     * @param resources The resources used to localize the root object, or {@code null} if
     *                  the root object was not localized.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}