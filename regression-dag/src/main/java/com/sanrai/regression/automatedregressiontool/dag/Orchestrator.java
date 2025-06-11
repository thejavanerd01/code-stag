package com.sanrai.regression.automatedregressiontool.dag;


import com.jayway.jsonpath.JsonPath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Orchestrator {
    private final Map<String, Worker> workers;
    private final Map<String, Workflow> workflows;
    private final ExecutorService executorService;
    private final ConcurrentLinkedQueue<Task> taskQueue;
    private final WorkflowParser parser;
    private final Map<String, Task> taskMap;
    private final Map<String, Set<String>> dependencyGraph;
    private final Map<String, Set<String>> decisionTaskCases;
    private final Map<String, String> selectedDecisionCases;
    private final AtomicInteger pendingTasks;

    public Orchestrator() {
        this.workers = new HashMap<>();
        this.workflows = new HashMap<>();
        this.executorService = Executors.newFixedThreadPool(10);
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.parser = new WorkflowParser();
        this.taskMap = new HashMap<>();
        this.dependencyGraph = new HashMap<>();
        this.decisionTaskCases = new HashMap<>();
        this.selectedDecisionCases = new HashMap<>();
        this.pendingTasks = new AtomicInteger(0);
    }

    public void registerWorker(Worker worker) {
        workers.put(worker.getTaskName(), worker);
        System.out.println("Registered worker: " + worker.getTaskName());
    }

    public String startWorkflow(Workflow workflow) {
        workflows.put(workflow.getWorkflowId(), workflow);
        workflow.setStatus(Workflow.WorkflowStatus.RUNNING);
        buildTaskMapAndDependencyGraph(workflow);
        validateNoCycles(workflow);
        scheduleTasks(workflow);
        return workflow.getWorkflowId();
    }

    private void buildTaskMapAndDependencyGraph(Workflow workflow) {
        for (Task task : workflow.getAllTasks()) {
            taskMap.put(task.getTaskReferenceName(), task);
            dependencyGraph.put(task.getTaskReferenceName(), new HashSet<>(task.getDependsOn()));
            System.out.println("Mapped task: " + task.getTaskReferenceName() + " with dependencies: " + task.getDependsOn());
            if ("DECISION".equals(task.getType())) {
                Set<String> caseTaskRefs = new HashSet<>();
                task.getDecisionCases().values().forEach(tasks ->
                        tasks.forEach(t -> {
                            taskMap.put(t.getTaskReferenceName(), t);
                            dependencyGraph.put(t.getTaskReferenceName(), new HashSet<>(t.getDependsOn()));
                            caseTaskRefs.add(t.getTaskReferenceName());
                            System.out.println("Mapped decision task: " + t.getTaskReferenceName() + " with dependencies: " + t.getDependsOn());
                        }));
                task.getDefaultCase().forEach(t -> {
                    taskMap.put(t.getTaskReferenceName(), t);
                    dependencyGraph.put(t.getTaskReferenceName(), new HashSet<>(t.getDependsOn()));
                    caseTaskRefs.add(t.getTaskReferenceName());
                    System.out.println("Mapped default case task: " + t.getTaskReferenceName() + " with dependencies: " + t.getDependsOn());
                });
                decisionTaskCases.put(task.getTaskReferenceName(), caseTaskRefs);
            }
        }
    }

    private void validateNoCycles(Workflow workflow) {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        for (String taskRef : dependencyGraph.keySet()) {
            if (!visited.contains(taskRef)) {
                if (hasCycle(taskRef, visited, recStack)) {
                    throw new IllegalStateException("Cycle detected in workflow: " + workflow.getName());
                }
            }
        }
    }

    private boolean hasCycle(String taskRef, Set<String> visited, Set<String> recStack) {
        visited.add(taskRef);
        recStack.add(taskRef);
        Set<String> dependencies = dependencyGraph.getOrDefault(taskRef, new HashSet<>());
        for (String dep : dependencies) {
            if (!visited.contains(dep)) {
                if (hasCycle(dep, visited, recStack)) {
                    return true;
                }
            } else if (recStack.contains(dep)) {
                return true;
            }
        }
        recStack.remove(taskRef);
        return false;
    }

    private void scheduleTasks(Workflow workflow) {
        for (Task task : workflow.getAllTasks()) {
            if (areDependenciesMet(task)) {
                task.setStatus(Task.TaskStatus.PENDING);
                taskQueue.add(task);
                pendingTasks.incrementAndGet();
                System.out.println("Queued task: " + task.getTaskReferenceName() + ", pending tasks: " + pendingTasks.get());
            }
        }
        executeTasks();
    }

    private void executeTasks() {
        executorService.submit(() -> {
            System.out.println("Starting executeTasks, initial queue: " + taskQueue.stream().map(Task::getTaskReferenceName).toList());
            while (pendingTasks.get() > 0) {
                Task task = taskQueue.poll();
                if (task == null) {
                    System.out.println("Queue empty, waiting for tasks, pending: " + pendingTasks.get());
                    try {
                        Thread.sleep(100); // Avoid busy loop
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                System.out.println("Dequeued task: " + task.getTaskReferenceName() + " of type: " + task.getType());
                if ("DECISION".equals(task.getType())) {
                    handleDecisionTask(task);
                } else {
                    System.out.println("Available workers: " + workers.keySet());
                    Worker worker = workers.get(task.getTaskName());
                    if (worker != null) {
                        task.setStatus(Task.TaskStatus.IN_PROGRESS);
                        System.out.println("Running worker for task: " + task.getTaskName());
                        task.setInputData(parser.resolveInputMappings(task, taskMap));
                        CompletableFuture.supplyAsync(() -> worker.execute(task), executorService)
                                .thenAccept(result -> {
                                    synchronized (taskMap) {
                                        task.setStatus(result.getStatus());
                                        task.setOutputData(result.getOutputData());
                                        task.setOutputData(parser.resolveOutputMappings(task));
                                        taskMap.put(task.getTaskReferenceName(), task);
                                        System.out.println("Completed task: " + task.getTaskReferenceName() + " with status: " + task.getStatus() + ", Output: " + task.getOutputData());
                                        scheduleDependentTasks(task);
                                        pendingTasks.decrementAndGet();
                                        System.out.println("Task completed, pending tasks: " + pendingTasks.get());
                                        checkWorkflowCompletion(task.getWorkflowId());
                                    }
                                });
                    } else {
                        task.setStatus(Task.TaskStatus.FAILED);
                        System.err.println("No worker found for task: " + task.getTaskName());
                        scheduleDependentTasks(task);
                        pendingTasks.decrementAndGet();
                        checkWorkflowCompletion(task.getWorkflowId());
                    }
                }
            }
            System.out.println("Task execution complete, final queue: " + taskQueue.stream().map(Task::getTaskReferenceName).toList());
            workflows.values().forEach(w -> checkWorkflowCompletion(w.getWorkflowId()));
        });
    }

    private void handleDecisionTask(Task task) {
        task.setStatus(Task.TaskStatus.IN_PROGRESS);
        System.out.println("Handling DECISION task: " + task.getTaskReferenceName());
        try {
            Map<String, Object> resolvedInputs = parser.resolveInputMappings(task, taskMap);
            System.out.println("Resolved inputs for " + task.getTaskReferenceName() + ": " + resolvedInputs);
            task.setInputData(resolvedInputs);

            String registrationStatus = null;
            try {
                registrationStatus = JsonPath.parse(resolvedInputs).read("$.registrationResult.status", String.class);
                System.out.println("Decision condition evaluated to: " + registrationStatus);
            } catch (Exception e) {
                System.err.println("Error evaluating decision condition: " + e.getMessage());
                registrationStatus = "failure";
            }

            synchronized (selectedDecisionCases) {
                selectedDecisionCases.put(task.getTaskReferenceName(), registrationStatus);
            }
            List<Task> tasksToExecute = task.getDecisionCases().getOrDefault(registrationStatus, task.getDefaultCase());
            System.out.println("Tasks to execute: " + tasksToExecute.stream().map(Task::getTaskReferenceName).toList());

            synchronized (taskQueue) {
                for (Task selectedTask : tasksToExecute) {
                    if (areDependenciesMet(selectedTask)) {
                        selectedTask.setStatus(Task.TaskStatus.PENDING);
                        taskQueue.add(selectedTask);
                        pendingTasks.incrementAndGet();
                        System.out.println("Queued decision task: " + selectedTask.getTaskReferenceName() + ", pending tasks: " + pendingTasks.get());
                    } else {
                        System.out.println("Dependencies not met for decision task: " + selectedTask.getTaskReferenceName());
                    }
                }
            }
            task.setStatus(Task.TaskStatus.COMPLETED);
            System.out.println("DECISION task completed: " + task.getTaskReferenceName());
        } catch (Exception e) {
            System.err.println("Error in handleDecisionTask for task " + task.getTaskReferenceName() + ": " + e.getMessage());
            task.setStatus(Task.TaskStatus.FAILED);
        }
        scheduleDependentTasks(task);
        pendingTasks.decrementAndGet();
        System.out.println("Decision task completed, pending tasks: " + pendingTasks.get());
        checkWorkflowCompletion(task.getWorkflowId());
    }

    private boolean areDependenciesMet(Task task) {
        if (task.getDependsOn() == null || task.getDependsOn().isEmpty()) {
            return true;
        }
        for (String depRef : task.getDependsOn()) {
            Task depTask = taskMap.get(depRef);
            if (depTask == null || depTask.getStatus() != Task.TaskStatus.COMPLETED) {
                System.out.println("Dependency " + depRef + " not met for task: " + task.getTaskReferenceName());
                return false;
            }
        }
        return true;
    }

    private void scheduleDependentTasks(Task completedTask) {
        System.out.println("Scheduling dependent tasks for: " + completedTask.getTaskReferenceName());
        String decisionTaskRef = findParentDecisionTask(completedTask);
        String selectedCase = selectedDecisionCases.get(decisionTaskRef);
        Set<String> allowedTasks = decisionTaskCases.getOrDefault(decisionTaskRef, new HashSet<>());
        System.out.println("Decision task: " + decisionTaskRef + ", Selected case: " + selectedCase + ", Allowed tasks: " + allowedTasks);

        synchronized (taskQueue) {
            for (Task task : taskMap.values()) {
                if (task.getDependsOn().contains(completedTask.getTaskReferenceName())) {
                    if (decisionTaskRef == null || (allowedTasks.contains(task.getTaskReferenceName()) && isTaskInSelectedCase(task, decisionTaskRef, selectedCase))) {
                        if (areDependenciesMet(task)) {
                            task.setStatus(Task.TaskStatus.PENDING);
                            taskQueue.add(task);
                            pendingTasks.incrementAndGet();
                            System.out.println("Queued dependent task: " + task.getTaskReferenceName() + ", pending tasks: " + pendingTasks.get());
                        } else {
                            System.out.println("Dependencies not met for dependent task: " + task.getTaskReferenceName());
                        }
                    } else {
                        System.out.println("Task " + task.getTaskReferenceName() + " not in selected decision case, skipping");
                    }
                }
            }
        }
    }

    private boolean isTaskInSelectedCase(Task task, String decisionTaskRef, String selectedCase) {
        if (decisionTaskRef == null || selectedCase == null) return true;
        Task decisionTask = taskMap.get(decisionTaskRef);
        if (decisionTask == null) return true;
        List<Task> tasksInCase = decisionTask.getDecisionCases().getOrDefault(selectedCase, decisionTask.getDefaultCase());
        return tasksInCase.stream().anyMatch(t -> t.getTaskReferenceName().equals(task.getTaskReferenceName()));
    }

    private String findParentDecisionTask(Task task) {
        String taskRef = task.getTaskReferenceName();
        // If the task itself is a decision task, return it
        if ("DECISION".equals(task.getType())) {
            System.out.println("Task " + taskRef + " is a DECISION task, returning as parent");
            return taskRef;
        }
        // Check if task is part of a decision case
        for (Map.Entry<String, Set<String>> entry : decisionTaskCases.entrySet()) {
            if (entry.getValue().contains(taskRef)) {
                System.out.println("Task " + taskRef + " found in decision case of " + entry.getKey());
                return entry.getKey();
            }
        }
        // Check if task depends on a decision task
        for (String depRef : task.getDependsOn()) {
            Task depTask = taskMap.get(depRef);
            if (depTask != null && "DECISION".equals(depTask.getType())) {
                System.out.println("Task " + taskRef + " depends on DECISION task " + depRef);
                return depRef;
            }
        }
        System.out.println("No parent decision task found for " + taskRef);
        return null;
    }

    private void checkWorkflowCompletion(String workflowId) {
        Workflow workflow = workflows.get(workflowId);
        synchronized (taskMap) {
            System.out.println("Checking workflow completion, task statuses: " +
                    taskMap.values().stream()
                            .filter(t -> t.getWorkflowId().equals(workflowId))
                            .map(t -> t.getTaskReferenceName() + ":" + t.getStatus())
                            .toList());
            boolean anyInProgress = taskMap.values().stream()
                    .filter(task -> task.getWorkflowId().equals(workflowId))
                    .anyMatch(task -> task.getStatus() == Task.TaskStatus.IN_PROGRESS);
            boolean anyFailed = taskMap.values().stream()
                    .filter(task -> task.getWorkflowId().equals(workflowId))
                    .anyMatch(task -> task.getStatus() == Task.TaskStatus.FAILED);
            boolean allRelevantCompleted = taskMap.values().stream()
                    .filter(task -> task.getWorkflowId().equals(workflowId))
                    .allMatch(task -> {
                        String decisionTaskRef = findParentDecisionTask(task);
                        String selectedCase = selectedDecisionCases.get(decisionTaskRef);
                        // Non-decision tasks or tasks in selected case must be COMPLETED
                        boolean isInSelectedCase = decisionTaskRef != null && isTaskInSelectedCase(task, decisionTaskRef, selectedCase);
                        boolean isNonDecisionTask = decisionTaskRef == null || !decisionTaskCases.containsKey(decisionTaskRef);
                        if (isNonDecisionTask || isInSelectedCase) {
                            return task.getStatus() == Task.TaskStatus.COMPLETED;
                        }
                        // Tasks not in selected case can be PENDING
                        return task.getStatus() == Task.TaskStatus.PENDING || task.getStatus() == Task.TaskStatus.COMPLETED;
                    });
            if (anyFailed) {
                workflow.setStatus(Workflow.WorkflowStatus.FAILED);
                System.out.println("Workflow " + workflow.getName() + " failed");
            } else if (!anyInProgress && allRelevantCompleted) {
                workflow.setStatus(Workflow.WorkflowStatus.COMPLETED);
                System.out.println("Workflow " + workflow.getName() + " completed successfully");
            } else {
                System.out.println("Workflow " + workflow.getName() + " still running, pending tasks: " +
                        taskMap.values().stream()
                                .filter(task -> task.getWorkflowId().equals(workflowId) && task.getStatus() == Task.TaskStatus.PENDING)
                                .map(Task::getTaskReferenceName)
                                .toList());
            }
        }
    }

    public Workflow getWorkflow(String workflowId) {
        return workflows.get(workflowId);
    }

    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                System.out.println("Executor service forcibly shut down");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Orchestrator shut down");
    }
}