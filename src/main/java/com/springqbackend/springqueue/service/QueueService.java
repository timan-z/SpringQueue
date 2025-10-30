package com.springqbackend.springqueue.service;

import com.springqbackend.springqueue.models.Task;
import com.springqbackend.springqueue.enums.TaskStatus;
import com.springqbackend.springqueue.runtime.Worker;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/* NOTE:: Initially did not have the @Service annotation because I was injecting this as a @Bean in SpringQueueApplication.java,
but as part of my ExecutorService Refactor, the @Bean has been removed so we should just have @Service here.
*/
@Service
public class QueueService {
    // Fields:
    private final ExecutorService executor;
    private final ConcurrentHashMap<String,Task> jobs;
    private final Lock lock;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();

    // Constructor:
    public QueueService() {
        this.jobs = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.executor = Executors.newFixedThreadPool(3);
    }

    // Methods:
    // 1. Translating GoQueue's "func (q * Queue) Enqueue(t task.Task) {...}" function:
    public void enqueue(Task t) {
        t.setStatus(TaskStatus.QUEUED);
        if(t.getAttempts() == 0) {
            t.setMaxRetries(3);
        }
        lock.lock();
        try {
            jobs.put(t.getId(), t); // GoQueue: q.jobs[t.ID] = &t;
            executor.submit(new Worker(t, this));   // Submit an instance of a Worker to the ExecutorService (executor pool).
        } finally {
            lock.unlock();
        }
    }

    /* NOTE: With the incorporation of ExecutorService, GoQueue's "func (q * Queue) Dequeue() * task.Task {...}" function is no longer
    necessary given that ExecutorService handles queuing internally. (That means I don't need to manage that myself anymore). */
    // 2. Translating GoQueue's "func (q * Queue) Dequeue() * task.Task {...}" function:
    /*public Task dequeue() throws InterruptedException {
        return tasks.take();    // "Blocks if the queue is still empty until an element becomes available."
    }
    */

    // 2. Translating GoQueue's "func (q * Queue) Clear() {...}" function:
    // (This is the method for "emptying the queue").
    public void clear() {
        lock.lock();
        try {
            jobs.clear();
            //tasks.clear();
            /* NOTE: ^ In my queue.go file, instead of tasks.clear(); I used a loop that would iterate while
            tasks wasn't empty and repeatedly pull values out of the Queue per iteration. I did that initially
            here w/ while(!tasks.isEmpty()) { tasks.take(); }. But it's not necessary here since the Queue's
            internal .clear() function does the work better for me AND there's potential for the thread to block
            indefinitely w/ .take(); (that's also why this function initially needed a catch (InterruptedException e) {...} block. */
        } finally {
            lock.unlock();
        }
    }

    // 3. Translating GoQueue's "func (q * Queue) GetJobs() []*task.Task {...}" function:
    // This is the method for returning a copy of all the Jobs (Tasks) we have:
    public Task[] getJobs() {
        readLock.lock();    // "read lock" only (in GoQueue: q.mu.RLock();)
        Task[] allTasks = new Task[jobs.size()];
        try {
            int i = 0;
            for(String key : jobs.keySet()) {
                allTasks[i] = jobs.get(key);
                i++;
            }
        } finally {
            readLock.unlock();
        }
        return allTasks;
    }

    // 5. Translating GoQueue's "func (q * Queue) GetJobByID(id String) (*task.Task, bool)" function:
    // This is the method for returning a specific Job (Task) by ID:
    public Task getJobById(String id) {
        /* In my GoQueue version of this function, I returned a bool and the Task, but the reasons for that
        were entirely superfluous and I can just return null like a normal human being if Task isn't found. */
        readLock.lock();
        Task t = null;
        try {
            if(jobs.containsKey(id)) {
                t = jobs.get(id);
            }
        } finally {
            readLock.unlock();
        }
        return t;
    }

    // 6. Translating GoQueue's "func (q * Queue) DeleteJob(id string) bool" function:
    // This is the method for deleting a specific Job (Task) by ID:
    public boolean deleteJob(String id) {
        lock.lock();
        boolean res = false;
        try {
            if(jobs.containsKey(id)) {
                jobs.remove(id);
                res = true;
            }
        } finally {
            lock.unlock();
        }
        return res;
    }

    // HELPER-METHOD(S): Might be helpful for monitoring endpoints if necessary...
    public int getJobMapCount() {
        return jobs.size();
    }

    /* NOTE-TO-SELF:
    - The ExecutorService needs explicit shutdown (else Spring Boot might hang on exit).
    - This ensures clean terminal (very important when the service is deployed on Railway or whatever).
    */
    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if(!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
