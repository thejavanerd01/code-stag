package com.sanrai.regression.automatedregressiontool.dag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Task {
    private String taskId;
    private String workflowId;
    private String taskName;
    private String taskReferenceName;
    private String type; // SIMPLE, DECISION
    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
    private TaskStatus status;
    private Map<String, List<Task>> decisionCases; // For DECISION tasks
    private List<Task> defaultCase; // For DECISION tasks
    private Map<String, String> inputMappings; // JSONPath mappings
    private Map<String, String> outputMappings; // JSONPath mappings
    private List<String> dependsOn; // Task dependencies

    public enum TaskStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }

    public Task(String taskName, String taskReferenceName, String type) {
        this.taskId = java.util.UUID.randomUUID().toString();
        this.taskName = taskName;
        this.taskReferenceName = taskReferenceName;
        this.type = type;
        this.inputData = new HashMap<>();
        this.outputData = new HashMap<>();
        this.status = TaskStatus.PENDING;
        this.decisionCases = new HashMap<>();
        this.defaultCase = new ArrayList<>();
        this.inputMappings = new HashMap<>();
        this.outputMappings = new HashMap<>();
        this.dependsOn = new ArrayList<>();
    }

    // Getters and setters
    public String getTaskId() { return taskId; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public String getTaskName() { return taskName; }
    public String getTaskReferenceName() { return taskReferenceName; }
    public String getType() { return type; }
    public Map<String, Object> getInputData() { return inputData; }
    public Map<String, Object> getOutputData() { return outputData; }
    public TaskStatus getStatus() { return status; }
    public Map<String, List<Task>> getDecisionCases() { return decisionCases; }
    public List<Task> getDefaultCase() { return defaultCase; }
    public Map<String, String> getInputMappings() { return inputMappings; }
    public Map<String, String> getOutputMappings() { return outputMappings; }
    public List<String> getDependsOn() { return dependsOn; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }
    public void setOutputData(Map<String, Object> outputData) { this.outputData = outputData; }
    public void addDecisionCase(String caseValue, List<Task> tasks) { this.decisionCases.put(caseValue, tasks); }
    public void setDefaultCase(List<Task> tasks) { this.defaultCase = tasks; }
    public void setInputMappings(Map<String, String> inputMappings) { this.inputMappings = inputMappings; }
    public void setOutputMappings(Map<String, String> outputMappings) { this.outputMappings = outputMappings; }
    public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }
}