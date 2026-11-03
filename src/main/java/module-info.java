module cl.duoc.dsy1102.timecapsule {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;

    opens cl.duoc.dsy1102.timecapsule to javafx.fxml;
    opens cl.duoc.dsy1102.timecapsule.model to com.fasterxml.jackson.databind;

    exports cl.duoc.dsy1102.timecapsule;
}
