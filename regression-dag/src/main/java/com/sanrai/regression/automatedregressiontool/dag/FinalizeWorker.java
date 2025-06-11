package com.sanrai.regression.automatedregressiontool.dag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class FinalizeWorker implements Worker {

    private static final Logger logger = LogManager.getLogger(FinalizeWorker.class.getName());

    @Override
    public String getTaskName() {
        return "finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        logger.info("Finalizing workflow with inputs: " + task.getInputData());
        Map<String, Object> output = new HashMap<>();
        output.put("result", "Workflow finalized with results: " + task.getInputData());
        return new TaskResult(Task.TaskStatus.COMPLETED, output);
    }
}