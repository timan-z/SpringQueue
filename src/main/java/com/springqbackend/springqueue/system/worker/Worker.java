package com.springqbackend.springqueue.system.worker;

import com.springqbackend.springqueue.models.queue.Queue;
import com.springqbackend.springqueue.models.task.Task;
import com.springqbackend.springqueue.enums.TaskStatus;

import java.util.Random;

public class Worker implements Runnable {
    // Fields:
    private final int id;
    private final Queue queue;
    private final Random rando = new Random();
    /* NOTE-TO-SELF: Remember, Java is pass-by-value BUT for objects that value is the reference itself.
    "queue" will point to the same Queue instantiated elsewhere (references point to the same location in the memory heap). */

    // Constructor:
    public Worker(int id, Queue queue) {
        this.id = id;
        this.queue = queue;
    } // DEBUG: Maybe queue should be autowired in? I have no idea. <-- Dependency Injection is automatic maybe I should add for good practice, I have no idea.

    // "run" will basically be this project's version of GoQueue's StartWorker():
    @Override
    public void run() {
        while(true) {
            try {
                Task t = queue.Dequeue();   // This will block the queue if it's empty, pull something off for processing if not!

                if(t.getAttempts() == t.getMaxRetries()) {
                    if(t.getStatus() == TaskStatus.FAILED) {
                        System.out.println("[Worker " + id + "] Task " + t.getId() + " (Type: " + t.getType() + ") failed permanently (max retries reached)");
                        continue;
                    }
                }
                t.setAttempts(t.getAttempts() + 1);
                t.setStatus(TaskStatus.INPROGRESS);
                System.out.println("[StartWorker]:[Worker " + id + "] Processing task: " + t.getId() + " (Attempt " + t.getAttempts() + " - " + t.getStatus() + ", Type: " + t.getType() + ")");

                handleTaskType(t);  // Verbose logic in my worker.go file can just be ported to a helper method for cleanliness.

            } catch(Exception e) {
                System.out.println("There was an issue in the Worker thread loop: " + e);
                continue;   // DEBUG:+NOTE: Not actually sure how I should handle this but this will do for now.
            }
        }
    }

    // NOTE-TO-SELF: Remember to use "private void" for methods I just want to call from the class' public methods...
    private void handleTaskType(Task t) throws InterruptedException {
        switch(t.getType()) {
            case "fail" -> {
                double succOdds = 0.25;
                if(t.getAttempts() <= t.getMaxRetries()) {
                    if (rando.nextDouble() <= succOdds) {
                        System.out.println("[Worker " + id + "] Task " + t.getId() + "(Type: fail - 0.25 success rate on retry) completed");
                        Thread.sleep(2000); // 2000 ms = 1 second.
                        t.setStatus(TaskStatus.COMPLETED);
                    } else {
                        System.out.println("[Worker " + id + "] Task " + t.getId() + "(Type: fail - 0.25 success rate on retry) failed! Retrying...");
                        Thread.sleep(1000);
                        t.setStatus(TaskStatus.FAILED);
                        queue.enqueue(t);   // re-attempt the failed task.
                    }
                }
            }
            case "fail-absolute" -> {
                if(t.getAttempts() <= t.getMaxRetries()) {
                    System.out.println("[Worker " + id + "] Task " + t.getId() + "(Type: fail-absolute) failed! Retrying...");
                    Thread.sleep(1000);
                    t.setStatus(TaskStatus.FAILED);
                    queue.enqueue(t);
                } else {
                    t.setStatus(TaskStatus.FAILED);
                    System.out.println("[Worker " + id + "] Task " + t.getId() + "(Type: fail-absolute) failed permanently (max retries reached)");
                }
            }
            // NOTE-TO-SELF: WAy smarter to simplify the rest of the cases...
            case "email" -> simulateWork(t, 2000, "email");
            case "report" -> simulateWork(t, 5000, "report");
            case "data-cleanup" -> simulateWork(t, 3000, "data-cleanup");
            case "sms" -> simulateWork(t, 1000, "sms");
            case "newsletter" -> simulateWork(t, 4000, "newsletter");
            case "takes-long" -> simulateWork(t, 10000, "takes-long");
            default -> simulateWork(t, 2000, "undefined");
        }
    }

    private void simulateWork(Task t, int durationMs, String type) throws InterruptedException {
        Thread.sleep(durationMs);
        t.setStatus(TaskStatus.COMPLETED);
        System.out.println("[Worker " + id + "] Task " + t.getId() + "(Type: " + t.getType() + ") completed");
    }
}
