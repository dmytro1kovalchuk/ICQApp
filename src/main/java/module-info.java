module org.icq.server_interface {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires java.xml;
    exports org.icq.client;

    opens org.icq.client to javafx.fxml;
    opens org.icq.server to javafx.fxml;
    exports org.icq.server;




}