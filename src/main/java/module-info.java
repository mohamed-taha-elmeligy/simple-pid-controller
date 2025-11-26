module com.emts.controlpid {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.swing;


    opens com.emts.controlpid to javafx.fxml;
    exports com.emts.controlpid;
}