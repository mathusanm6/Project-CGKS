package com.github.cgks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class Task {
    @JsonProperty("id")
    private String id;
    @JsonProperty("status")
    private TaskStatus status;
    @JsonProperty("parameters")
    private MiningRequest parameters;
    @JsonProperty("result")
    private List<MiningResult> result;
    @JsonProperty("error")
    private String error;
    @JsonProperty("cancellationRequested")
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
