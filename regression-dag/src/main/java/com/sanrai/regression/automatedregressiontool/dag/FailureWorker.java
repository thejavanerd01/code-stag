package com.sanrai.regression.automatedregressiontool.dag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FailureWorker implements Worker {
    private static final Logger logger = LoggerFactory.getLogger(FailureWorker.class);

    @Override
    public String getTaskName() {
        return "handle_failure";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();
        String error = (String) input.get("error");

        Map<String, Object> output = new HashMap<>();
        Map<String, Object> result = new HashMap<>();

        String errorMessage = error != null ? error : "Unknown error";
        result.put("errorMessage", errorMessage);
        output.put("result", result);

        System.out.println("Handled registration failure: " + errorMessage);
        logger.info("execution done for FailureWorker");
        return new TaskResult(Task.TaskStatus.COMPLETED, output);
    }
}