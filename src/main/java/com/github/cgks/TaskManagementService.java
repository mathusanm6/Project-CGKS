package com.github.cgks;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TaskManagementService {

    private final AtomicReference<Task> currentTask = new AtomicReference<>();

    public Task submitTask(MiningRequest request) {
        Task newTask = new Task(request);
        if (currentTask.compareAndSet(null, newTask)) {
            newTask.setStatus(TaskStatus.PENDING);
            processTask(newTask);
            return newTask;
        } else {
            // Another task is already processing or pending
            return null; // Or throw an exception, or return the existing task
        }
    }

    @Async("taskExecutor")
    public void processTask(Task task) {
        task.setStatus(TaskStatus.PROCESSING);
        MiningEngine engine = new MiningEngine();
        try {
            // Simulate long processing
            // In a real scenario, the MiningEngine would need to check for cancellation
            // For example, by periodically checking task.isCancellationRequested()
            // or Thread.currentThread().isInterrupted()
            List<MiningResult> results = engine.runMining(task.getParameters(), task::isCancellationRequested);
            if (task.isCancellationRequested()) {
                task.setStatus(TaskStatus.CANCELLED);
            } else {
                task.setResult(results);
                task.setStatus(TaskStatus.COMPLETED);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve interrupt status
            task.setStatus(TaskStatus.CANCELLED);
            task.setError("Task was cancelled by user.");
        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED);
            task.setError(e.getMessage());
        } finally {
            // Only clear the current task if it's the one we just processed
            currentTask.compareAndSet(task, null);
        }
    }

    public Task getCurrentTaskStatus() {
        return currentTask.get();
    }

    public boolean cancelCurrentTask() {
        Task task = currentTask.get();
        if (task != null && (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.PROCESSING)) {
            task.setCancellationRequested(true);
            // The running task (processTask method) should check this flag and stop.
            // If it's PENDING, it might not even start or be removed from a queue.
            // For immediate effect on PROCESSING, the MiningEngine needs to support interruption.
            return true;
        }
        return false;
    }
}
