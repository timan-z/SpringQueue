package com.springqbackend.springqueue.service;

import com.springqbackend.springqueue.models.Task;
import com.springqbackend.springqueue.enums.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/* NOTE-TO-SELF: In the context of remaking my GoQueue project, this would be my Queue.go file.
**********************************************************************************************
* MORE NOTES (for my own learning, more related to Spring Boot semantics and best practices):
** This should definitely be a @Service (or @Component). This does get injected by Spring's container since:
*** This queue is long-lived and shared across the entire program.
*** It's not just a data class; it's a stateful SERVICE managing concurrency and global access.
*** It needs to be injected into Worker, ProducerController, and so on via dependency injection.
* Going w/ @Service over @Component because it better conveys business logic.
* This is a Spring Bean, so I don't need setters, getters, etc. (Not a POJO, purely focused on business logic).
*/

//@Service
// NOTE: ^ This is not needed since I have @Bean in my SpringQueueApplication.java file.
public class QueueService {
    // Fields:
    private final BlockingQueue<Task> tasks;
    private final ConcurrentHashMap<String,Task> jobs;
    private final Lock lock;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();

    // Constructor:
    public QueueService(int tasksCapacity) {
        this.tasks = new LinkedBlockingQueue<>(tasksCapacity);
        this.jobs = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
    }

    // Methods:
    // 1. Translating GoQueue's "func (q * Queue) Enqueue(t task.Task) {...}" function:
    public void enqueue(Task t) {
        // NOTE: Slight change from my Queue.go file, but I reckon these actions can be done before the lock:
        t.setStatus(TaskStatus.QUEUED);
        if(t.getAttempts() == 0) {
            t.setMaxRetries(3);
        }
        lock.lock();
        try {
            jobs.put(t.getId(), t); // GoQueue: q.jobs[t.ID] = &t;
            tasks.put(t);   // GoQueue: q.Tasks <- &t;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // NOTE: This line here is the proper "canonical" way to handle these scenarios in Java.
            throw new RuntimeException("Thread interrupted while enqueueing: ", e);
        } finally {
            lock.unlock();
        }
    }

    // 2. Translating GoQueue's "func (q * Queue) Dequeue() * task.Task {...}" function:
    public Task dequeue() throws InterruptedException {
        return tasks.take();    // "Blocks if the queue is still empty until an element becomes available."
    }

    // 3. Translating GoQueue's "func (q * Queue) Clear() {...}" function:
    // (This is the method for "emptying the queue").
    public void clear() {
        lock.lock();
        try {
            jobs.clear();
            tasks.clear();
            /* NOTE: ^ In my queue.go file, instead of tasks.clear(); I used a loop that would iterate while
            tasks wasn't empty and repeatedly pull values out of the Queue per iteration. I did that initially
            here w/ while(!tasks.isEmpty()) { tasks.take(); }. But it's not necessary here since the Queue's
            internal .clear() function does the work better for me AND there's potential for the thread to block
            indefinitely w/ .take(); (that's also why this function initially needed a catch (InterruptedException e) {...} block. */
        } finally {
            lock.unlock();
        }
    }

    // 4. Translating GoQueue's "func (q * Queue) GetJobs() []*task.Task {...}" function:
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

    // HELPER-METHODS: Might be helpful for monitoring endpoints if necessary...
    public int getPendingTaskCount() {
        return tasks.size();
    }
    public int getJobMapCount() {
        return jobs.size();
    }
}
