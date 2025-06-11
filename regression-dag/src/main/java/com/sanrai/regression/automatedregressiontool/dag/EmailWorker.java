package com.sanrai.regression.automatedregressiontool.dag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class EmailWorker implements Worker {

    private static final Logger logger = LogManager.getLogger(CustomLogWorker.class.getName());

    @Override
    public String getTaskName() {
        return "send_email";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("Sending email to: " + task.getInputData().get("email"));
        Map<String, Object> output = new HashMap<>();
        output.put("result", "Email sent to " + task.getInputData().get("email"));
        return new TaskResult(Task.TaskStatus.COMPLETED, output);
    }
}