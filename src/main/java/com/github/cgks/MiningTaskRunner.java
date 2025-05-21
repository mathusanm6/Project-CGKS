package com.github.cgks;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class MiningTaskRunner {

    /**
     * Runs the mining process in a separate thread
     * @param task The task to run
     * @return A CompletableFuture that will be completed when the mining is done
     */
    @Async("miningTaskExecutor")
    public CompletableFuture<Task> runMiningTask(Task task) {
        task.setStatus(TaskStatus.PROCESSING);
        MiningEngine engine = new MiningEngine();
        
        try {
            // Execute the mining operation
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
        }
        
        return CompletableFuture.completedFuture(task);
    }
}
