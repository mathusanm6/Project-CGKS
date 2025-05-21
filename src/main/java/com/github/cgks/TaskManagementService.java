package com.github.cgks;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CompletableFuture;

@Service
public class TaskManagementService {

    private final AtomicReference<Task> currentTask = new AtomicReference<>();
    private final MiningTaskRunner miningTaskRunner;

    @Autowired
    public TaskManagementService(MiningTaskRunner miningTaskRunner) {
        this.miningTaskRunner = miningTaskRunner;
    }

    public Optional<Task> submitTask(MiningRequest request) {
        Task newTask = new Task(request);
        if (currentTask.compareAndSet(null, newTask)) {
            newTask.setStatus(TaskStatus.PENDING);
            // This will run in a separate thread immediately
            miningTaskRunner.runMiningTask(newTask);
            return Optional.of(newTask);
        } else {
            // Another task is already processing or pending
            return Optional.empty();
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Task> getTaskStatusAsync() {
        Task task = currentTask.get();
        return CompletableFuture.completedFuture(task);
    }

    public Task getCurrentTaskStatus() {
        return currentTask.get();
    }

    public boolean cancelCurrentTask() {
        Task task = currentTask.get();
        if (task != null && (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.PROCESSING)) {
            task.setCancellationRequested(true);
            // The mining task runner will check this flag and stop the processing
            return true;
        }
        return false;
    }

    // Called by the frontend to acknowledge and clear a terminal task
    public boolean acknowledgeAndClearTask() {
        Task task = currentTask.get();
        if (task != null && (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.FAILED || task.getStatus() == TaskStatus.CANCELLED)) {
            return currentTask.compareAndSet(task, null);
        }
        return false;
    }
}
