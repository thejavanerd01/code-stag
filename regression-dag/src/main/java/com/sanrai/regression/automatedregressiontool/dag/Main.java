package com.sanrai.regression.automatedregressiontool.dag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            // Initialize ApplicationContext with userInput
            Map<String, Object> userInput = new HashMap<>();
            userInput.put("username", "john_doe");
            userInput.put("email", "john@example.com");
            ApplicationContext.getInstance().setValue("userInput", userInput);

            // Initialize orchestrator
            Orchestrator orchestrator = new Orchestrator();

            // Register workers
            orchestrator.registerWorker(new RegisterWorker());
            orchestrator.registerWorker(new ProfileInfoWorker());
            orchestrator.registerWorker(new SavePreferenceWorker());
            orchestrator.registerWorker(new UploadStatusWorker());
            orchestrator.registerWorker(new FailureWorker());

            // Load JSON workflow from file
            String jsonWorkflow = new String(Files.readAllBytes(Paths.get("workflow.json")));
            System.out.println("Loaded workflow JSON");

            // Parse JSON to Workflow
            WorkflowParser parser = new WorkflowParser();
            Workflow workflow = parser.parseWorkflow(jsonWorkflow);
            System.out.println("Parsed workflow: " + workflow.getName());

            // Start workflow
            String workflowId = orchestrator.startWorkflow(workflow);
            System.out.println("Started workflow with ID: " + workflowId);

            // Wait for workflow completion
            Workflow completedWorkflow = orchestrator.getWorkflow(workflowId);
            int elapsedSeconds = 0;
            while (completedWorkflow.getStatus() == Workflow.WorkflowStatus.RUNNING) {
                Thread.sleep(1000);
                elapsedSeconds++;
                System.out.println("Checking workflow status after " + elapsedSeconds + " seconds: " + completedWorkflow.getStatus());
                completedWorkflow = orchestrator.getWorkflow(workflowId);
            }

            // Check final workflow status
            System.out.println("Final Workflow Status: " + completedWorkflow.getStatus());
            completedWorkflow.getAllTasks().forEach(task -> {
                if ("DECISION".equals(task.getType())) {
                    System.out.println("Task " + task.getTaskReferenceName() + ": " + task.getStatus());
                } else {
                    System.out.println("Task " + task.getTaskReferenceName() + ": " + task.getStatus() + ", Output: " + task.getOutputData());
                }
            });

            // Shutdown orchestrator
            orchestrator.shutdown();
        } catch (IOException e) {
            System.err.println("Error loading workflow.json: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Interrupted exception: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
