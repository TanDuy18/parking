module org.example.duanparking {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop; // thêm nếu dùng OpenCV load ảnh
    requires opencv;
    requires java.rmi;
    requires java.sql;

    opens org.example.duanparking.controller to javafx.fxml;
    exports org.example.duanparking;
    exports org.example.duanparking.common to java.rmi;
    opens org.example.duanparking.server to java.rmi;

}
