module org.example.duanparking {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop; // thêm nếu dùng OpenCV load ảnh
    requires opencv;
    requires java.rmi;
    requires java.sql;

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

}
