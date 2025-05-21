package com.github.cgks;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin // Autorise les appels depuis React
public class MiningController {

    private final TaskManagementService taskManagementService;

    @Autowired
    public MiningController(TaskManagementService taskManagementService) {
        this.taskManagementService = taskManagementService;
    }

    @PostMapping
    public ResponseEntity<?> submitMiningTask(@RequestBody MiningRequest request) {
        // This API endpoint will respond quickly without waiting for the mining to complete
        Task existingTask = taskManagementService.getCurrentTaskStatus();
        if (existingTask != null &&
                (existingTask.getStatus() == TaskStatus.PENDING || existingTask.getStatus() == TaskStatus.PROCESSING)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A task is already processing. Please wait or cancel the current task.");
        }

        Task submittedTask = taskManagementService.submitTask(request);
        if (submittedTask != null) {
            return ResponseEntity.ok(submittedTask);
        } else {
            // This case should ideally be handled by the check above,
            // but as a fallback if submitTask returns null for other reasons.
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Failed to submit task. The service might be busy or unable to accept new tasks at the moment.");
        }
    }

    @GetMapping("/status")
    public CompletableFuture<ResponseEntity<Task>> getTaskStatus() {
        // This method runs in a separate thread pool from the mining tasks
        return taskManagementService.getTaskStatusAsync()
            .thenApply(currentTask -> {
                if (currentTask != null) {
                    return ResponseEntity.ok(currentTask);
                } else {
                    return ResponseEntity.ok(null);
                }
            });
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelTask() {
        boolean cancelled = taskManagementService.cancelCurrentTask();
        if (cancelled) {
            return ResponseEntity.ok("Cancellation request acknowledged. The task will be stopped.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No active task to cancel or task is already completed/cancelled.");
        }
    }

    @PostMapping("/acknowledge")
    public ResponseEntity<String> acknowledgeTask() {
        boolean cleared = taskManagementService.acknowledgeAndClearTask();
        if (cleared) {
            return ResponseEntity.ok("Task acknowledged and cleared.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No terminal task to acknowledge.");
        }
    }
}
