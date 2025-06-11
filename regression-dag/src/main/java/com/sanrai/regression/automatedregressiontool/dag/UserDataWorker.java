package com.sanrai.regression.automatedregressiontool.dag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class UserDataWorker implements Worker {

    private static final Logger logger = LogManager.getLogger(UserDataWorker.class.getName());

    @Override
    public String getTaskName() {
        return "collect_user_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        logger.info("Collecting data for user: " + userId);
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", userId + "@example.com");
        userData.put("phone", "1234567890");
        userData.put("preferredMethod", userId.equals("12345") ? "email_preferred" : "sms_preferred");
        Map<String, Object> output = new HashMap<>();
        output.put("result", userData);
        return new TaskResult(Task.TaskStatus.COMPLETED, output);
    }
}