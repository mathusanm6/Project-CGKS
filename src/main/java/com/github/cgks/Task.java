package com.github.cgks;

import java.util.List;
import java.util.UUID;

public class Task {
    private String id;
    private TaskStatus status;
    private MiningRequest parameters;
    private List<MiningResult> result;
    private String error;
    private boolean cancellationRequested;

    public Task(MiningRequest parameters) {
        this.id = UUID.randomUUID().toString();
        this.parameters = parameters;
        this.status = TaskStatus.PENDING;
        this.cancellationRequested = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public MiningRequest getParameters() {
        return parameters;
    }

    public List<MiningResult> getResult() {
        return result;
    }

    public void setResult(List<MiningResult> result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isCancellationRequested() {
        return cancellationRequested;
    }

    public void setCancellationRequested(boolean cancellationRequested) {
        this.cancellationRequested = cancellationRequested;
    }
}
