module com.sanrai.regression.automatedregressiontool {

    requires json.path;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires java.logging;
    requires org.slf4j;

    opens com.sanrai.regression.automatedregressiontool to javafx.fxml;
    exports com.sanrai.regression.automatedregressiontool;
}