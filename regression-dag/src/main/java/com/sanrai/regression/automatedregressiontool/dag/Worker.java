package com.sanrai.regression.automatedregressiontool.dag;

public interface Worker {

    String getTaskName();

    TaskResult execute(Task task);
}