package com.sanrai.regression.automatedregressiontool.dag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Workflow {
    private final String name;
    private final int version;
    private final String workflowId;
    private WorkflowStatus status;
    private final List<Task> tasks;

    public enum WorkflowStatus {
        RUNNING, COMPLETED, FAILED
    }

    public Workflow(String name, int version) {
        this.name = name;
        this.version = version;
        this.workflowId = UUID.randomUUID().toString();
        this.status = WorkflowStatus.RUNNING;
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public String getName() {
        return name;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<Task> getAllTasks() {
        List<Task> allTasks = new ArrayList<>(tasks);
        for (Task task : tasks) {
            if ("DECISION".equals(task.getType())) {
                task.getDecisionCases().values().forEach(allTasks::addAll);
                allTasks.addAll(task.getDefaultCase());
            }
        }
        return allTasks;
    }
}