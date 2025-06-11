package com.sanrai.regression.automatedregressiontool.dag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class CustomLogWorker implements Worker {

    private static final Logger logger = LogManager.getLogger(CustomLogWorker.class.getName());


    @Override
    public String getTaskName() {
        return "custom_log";
    }

    @Override
    public TaskResult execute(Task task) {
        String message = (String) task.getInputData().get("message");
        logger.info("Logging message: " + message);
        Map<String, Object> output = new HashMap<>();
        output.put("result", "Logged: " + message);
        return new TaskResult(Task.TaskStatus.COMPLETED, output);
    }
}