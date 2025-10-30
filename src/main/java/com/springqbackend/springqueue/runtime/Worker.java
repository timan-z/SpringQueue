package com.springqbackend.springqueue.runtime;

import com.springqbackend.springqueue.service.QueueService;
import com.springqbackend.springqueue.models.Task;
import com.springqbackend.springqueue.enums.TaskStatus;
import java.util.Random;

public class Worker implements Runnable {
    // Fields:
    private final Task task;
    private final QueueService queue;
    private final Random rando = new Random();
    /* NOTE-TO-SELF: Remember, Java is pass-by-value BUT for objects that value is the reference itself.
    "queue" will point to the same Queue instantiated elsewhere (references point to the same location in the memory heap). */

    /* DEBUG: Removed fields as part of the ExecutorService Refactor:
    private final int id;
    */

    // Constructor:
    public Worker(Task task, QueueService queue) {
        this.task = task;
        this.queue = queue;
    }

    // "run" will basically be this project's version of GoQueue's StartWorker():
    @Override
    public void run() {
        try {
            /*DEBUG: Task t = queue.dequeue();   // This will block the queue if it's empty, pull something off for processing if not!

            if(t.getAttempts() == t.getMaxRetries()) {
                if(t.getStatus() == TaskStatus.FAILED) {
                    System.out.println("[Worker " + id + "] Task " + t.getId() + " (Type: " + t.getType() + ") failed permanently (max retries reached)");
                    break;
                }
            }*/
            // DEBUG: ^ All of that is no longer necessary because of the wonderful ExecutorService Refactor.
            task.setAttempts(task.getAttempts() + 1);
            task.setStatus(TaskStatus.INPROGRESS);
            System.out.printf("[Worker] Processing task %s (Attempt %d, Type: %s)%n", task.getId(), task.getAttempts(), task.getType());

            handleTaskType(task);  // Verbose logic in my worker.go file can just be ported to a helper method for cleanliness.
        } catch(Exception e) {
            System.err.printf("[Worker] Task %s failed due to error: %s%n", task.getId(), e.getMessage());
            task.setStatus(TaskStatus.FAILED);
        }
    }

    // NOTE-TO-SELF: Remember to use "private void" for methods I just want to call from the class' public methods...
    private void handleTaskType(Task t) throws InterruptedException {
        switch(t.getType()) {
            /*case "fail" -> {
                double succOdds = 0.25;
                if(t.getAttempts() <= t.getMaxRetries()) {
                    if (rando.nextDouble() <= succOdds) {
                        System.out.println("[Worker " + id + "] Task " + t.getId() + "(Type: fail - 0.25 success rate on retry) completed");
                        Thread.sleep(2000); // 2000 ms = 1 second.
                        t.setStatus(TaskStatus.COMPLETED);
                    } else {
                        if (t.getAttempts() != t.getMaxRetries()) {
                            System.out.println("[Worker " + id + "] Task " + t.getId() + "(Type: fail - 0.25 success rate on retry) failed! Retrying...");
                        } else {
                            System.out.println("[Worker " + id + "] Task " + t.getId() + "(Type: fail - 0.25 success rate on retry) failed! [No more retries!]");
                        }

                        Thread.sleep(1000);
                        t.setStatus(TaskStatus.FAILED);
                        if (t.getAttempts() != t.getMaxRetries()) queue.enqueue(t);   // re-attempt the failed task (if there are still attempts left).
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
            }*/
            case "fail" -> handleFailType(t);
            case "fail-absolute" -> handleAbsoluteFail(t);
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

    // DEBUG: Refactoring type "fail" and "fail-absolute" to be their private methods for cleanliness and modularity:
    // handleFailType:
    private void handleFailType(Task t) throws InterruptedException {
        double successChance = 0.25;
        if(rando.nextDouble() <= successChance) {
            Thread.sleep(2000);
            t.setStatus(TaskStatus.COMPLETED);
            System.out.printf("[Worker] Task %s (Type: fail - 0.25 success rate on retry) completed%n", t.getId());
        } else {
            Thread.sleep(1000);
            t.setStatus(TaskStatus.FAILED);
            if (t.getAttempts() < t.getMaxRetries()) {
                System.out.printf("[Worker] Task %s (Type: fail - 0.25 success rate on retry) failed! Retrying...%n", t.getId());
                queue.enqueue(t);
            } else {
                System.out.printf("[Worker] Task %s (Type: fail - 0.25 success rate on retry) failed permanently!%n", t.getId());
            }
        }
    }

    // handleAbsoluteFail:
    private void handleAbsoluteFail(Task t) throws InterruptedException {
        Thread.sleep(1000);
        t.setStatus(TaskStatus.FAILED);
        if (t.getAttempts() < t.getMaxRetries()) {
            System.out.printf("[Worker] Task %s (Type: fail-absolute) failed! Retrying...%n", t.getId());
            queue.enqueue(t);
        } else {
            System.out.printf("[Worker] Task %s (Type: fail-absolute) failed permanently!%n", t.getId());
        }
    }

    private void simulateWork(Task t, int durationMs, String type) throws InterruptedException {
        Thread.sleep(durationMs);
        t.setStatus(TaskStatus.COMPLETED);
        System.out.printf("[Worker] Task %s (Type: %s) completed%n", t.getId(), t.getType());
    }
}
