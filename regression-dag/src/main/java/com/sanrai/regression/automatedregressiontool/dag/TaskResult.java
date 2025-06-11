package com.sanrai.regression.automatedregressiontool.dag;

import java.util.HashMap;
import java.util.Map;

public class TaskResult {
    private Task.TaskStatus status;
    private Map<String, Object> outputData;

    public TaskResult(Task.TaskStatus status, Map<String, Object> outputData) {
        this.status = status;
        this.outputData = outputData;
    }

    public Task.TaskStatus getStatus() { return status; }
    public Map<String, Object> getOutputData() { return outputData; }
}