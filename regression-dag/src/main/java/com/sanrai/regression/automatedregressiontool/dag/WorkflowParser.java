package com.sanrai.regression.automatedregressiontool.dag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowParser {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Configuration jsonPathConfig = Configuration.defaultConfiguration()
            .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

    public Workflow parseWorkflow(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        String name = root.get("name").asText();
        int version = root.get("version").asInt();
        Workflow workflow = new Workflow(name, version);
        String workflowId = workflow.getWorkflowId();

        JsonNode tasksNode = root.get("tasks");
        for (JsonNode taskNode : tasksNode) {
            Task task = parseTask(taskNode, workflowId);
            workflow.addTask(task);
        }

        return workflow;
    }

    private Task parseTask(JsonNode taskNode, String workflowId) {
        String taskReferenceName = taskNode.get("taskReferenceName") != null ? taskNode.get("taskReferenceName").asText() : null;
        String taskName = taskNode.get("taskName") != null ? taskNode.get("taskName").asText() : null;
        String type = taskNode.get("type").asText();

        if ("PARALLEL".equals(type)) {
            throw new IllegalArgumentException("PARALLEL task type is not supported");
        }

        if (taskReferenceName == null) {
            taskReferenceName = type + "_" + java.util.UUID.randomUUID().toString();
        }

        Task task = new Task(taskName, taskReferenceName, type);
        task.setWorkflowId(workflowId);

        if (taskNode.has("inputParameters")) {
            Map<String, Object> inputParams = objectMapper.convertValue(taskNode.get("inputParameters"), Map.class);
            task.setInputData(inputParams);
        }

        if (taskNode.has("inputMappings")) {
            Map<String, String> inputMappings = objectMapper.convertValue(taskNode.get("inputMappings"), Map.class);
            task.setInputMappings(inputMappings);
        }

        if (taskNode.has("outputMappings")) {
            Map<String, String> outputMappings = objectMapper.convertValue(taskNode.get("outputMappings"), Map.class);
            task.setOutputMappings(outputMappings);
        }

        if (taskNode.has("dependsOn")) {
            List<String> dependsOn = objectMapper.convertValue(taskNode.get("dependsOn"), List.class);
            task.setDependsOn(dependsOn);
        }

        if ("DECISION".equals(type) && taskNode.has("decisionCases")) {
            JsonNode decisionCasesNode = taskNode.get("decisionCases");
            decisionCasesNode.fields().forEachRemaining(entry -> {
                List<Task> caseTasks = new ArrayList<>();
                for (JsonNode caseTaskNode : entry.getValue()) {
                    Task caseTask = parseTask(caseTaskNode, workflowId);
                    caseTasks.add(caseTask);
                }
                task.addDecisionCase(entry.getKey(), caseTasks);
            });
            if (taskNode.has("defaultCase")) {
                List<Task> defaultTasks = new ArrayList<>();
                for (JsonNode defaultTaskNode : taskNode.get("defaultCase")) {
                    defaultTasks.add(parseTask(defaultTaskNode, workflowId));
                }
                task.setDefaultCase(defaultTasks);
            }
        }

        return task;
    }

    public Map<String, Object> resolveInputMappings(Task task, Map<String, Task> taskMap) {
        Map<String, Object> resolvedInputs = new HashMap<>();
        System.out.println("Resolving input mappings for task: " + task.getTaskReferenceName());

        // Resolve inputMappings
        Map<String, String> inputMappings = task.getInputMappings();
        if (inputMappings != null) {
            for (Map.Entry<String, String> mapping : inputMappings.entrySet()) {
                String key = mapping.getKey();
                String jsonPath = mapping.getValue();
                System.out.println("Mapping key: " + key + ", JSONPath: " + jsonPath);
                Object value = resolveJsonPath(jsonPath, taskMap);
                resolvedInputs.put(key, value);
            }
        }

        // Resolve JSONPath expressions in inputParameters
        Map<String, Object> inputParams = task.getInputData();
        if (inputParams != null) {
            for (Map.Entry<String, Object> entry : inputParams.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof String && ((String) value).startsWith("${") && ((String) value).endsWith("}")) {
                    String jsonPath = (String) value;
                    System.out.println("Resolving input parameter: " + key + ", JSONPath: " + jsonPath);
                    resolvedInputs.put(key, resolveJsonPath(jsonPath, taskMap));
                } else {
                    resolvedInputs.put(key, value);
                }
            }
        }

        System.out.println("Resolved inputs: " + resolvedInputs);
        return resolvedInputs;
    }

    public Map<String, Object> resolveOutputMappings(Task task) {
        Map<String, Object> resolvedOutputs = new HashMap<>();
        Map<String, String> outputMappings = task.getOutputMappings();
        if (outputMappings != null && !outputMappings.isEmpty()) {
            for (Map.Entry<String, String> mapping : outputMappings.entrySet()) {
                String key = mapping.getKey();
                String jsonPath = mapping.getValue();
                System.out.println("Resolving output mapping for task: " + task.getTaskReferenceName() + ", Key: " + key + ", JSONPath: " + jsonPath);
                try {
                    Object value = JsonPath.using(jsonPathConfig).parse(task.getOutputData()).read(jsonPath);
                    resolvedOutputs.put(key, value);
                    System.out.println("Resolved output for key " + key + ": " + value);
                } catch (Exception e) {
                    System.err.println("Error resolving output mapping for task: " + task.getTaskReferenceName() + ", JSONPath: " + jsonPath + ": " + e.getMessage());
                }
            }
        } else {
            resolvedOutputs.putAll(task.getOutputData());
        }
        System.out.println("Resolved outputs for task " + task.getTaskReferenceName() + ": " + resolvedOutputs);
        return resolvedOutputs;
    }

    private Object resolveJsonPath(String jsonPath, Map<String, Task> taskMap) {
        System.out.println("Resolving JSONPath: " + jsonPath);
        if (!jsonPath.startsWith("${") || !jsonPath.endsWith("}")) {
            System.out.println("JSONPath " + jsonPath + " not a dynamic expression, returning as-is");
            return jsonPath;
        }

        String path = jsonPath.substring(2, jsonPath.length() - 1);
        String[] parts = path.split("\\.");
        if (parts.length < 2) {
            System.err.println("Invalid JSONPath: " + jsonPath);
            return null;
        }

        String taskRef = parts[0];
        Task referencedTask = taskMap.get(taskRef);
        if (referencedTask == null || referencedTask.getOutputData() == null) {
            System.err.println("Task " + taskRef + " not found or has no output data for JSONPath: " + jsonPath);
            return null;
        }

        // Skip 'output' if present
        int startIndex = parts.length > 1 && parts[1].equals("output") ? 2 : 1;
        StringBuilder jsonPathExpr = new StringBuilder("$");
        for (int i = startIndex; i < parts.length; i++) {
            jsonPathExpr.append("['").append(parts[i]).append("']");
        }

        try {
            System.out.println("Task " + taskRef + " output data: " + referencedTask.getOutputData());
            System.out.println("Evaluating JSONPath: " + jsonPathExpr);
            Object result = JsonPath.using(jsonPathConfig).parse(referencedTask.getOutputData()).read(jsonPathExpr.toString());
            System.out.println("Resolved JSONPath " + jsonPath + " to: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("Error resolving JSONPath " + jsonPath + ": " + e.getMessage());
            return null;
        }
    }
}