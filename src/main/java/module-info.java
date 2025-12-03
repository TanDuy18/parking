module org.example.duanparking {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop; // thêm nếu dùng OpenCV load ảnh
    requires java.rmi;
    requires java.sql;
    requires tess4j;
    requires webcam.capture;
    requires javafx.swing;
    requires org.controlsfx.controls;


    exports org.example.duanparking;
    exports org.example.duanparking.model to java.rmi;
    exports org.example.duanparking.server to java.rmi;
    exports org.example.duanparking.client to java.rmi;
    opens org.example.duanparking.client to java.rmi, javafx.fxml;
    exports org.example.duanparking.common.remote to java.rmi;
    opens org.example.duanparking.common.remote to java.rmi, javafx.fxml;
    opens org.example.duanparking.server to java.rmi, javafx.fxml;
    exports org.example.duanparking.client.controller to java.rmi;
    opens org.example.duanparking.client.controller to java.rmi, javafx.fxml;
    exports org.example.duanparking.server.dao to java.rmi;
    opens org.example.duanparking.common to javafx.fxml;
    exports org.example.duanparking.common;


}
