package com.springqbackend.springqueue.models.queue;

// NOTE-TO-SELF: In the context of remaking my GoQueue project, this would be my Queue.go file.
/* Some other notes about translating the logic of my Queue.go file:
"Java's BlockingQueue implementations, such as ArrayBlockingQueue and LinkedBlockingQueue, are designed to be thread-safe
and internally manage their own synchronization using locks and condition variables. Therefore, explicitly locking a BlockingQueue
with a separate Lock object in your application code is generally not necessary for basic put() and take() operations.
However, there might be specific scenarios where you want to add an additional layer of synchronization or coordinate access to the
BlockingQueue with other shared resources. In such cases, you can use a java.util.concurrent.locks.Lock object, like ReentrantLock,
to protect a section of code that involves BlockingQueue operations and potentially other critical sections." (From Google AI).
-- pretty sure I still need the lock/mutex for the Jobs map though.
*/

import com.springqbackend.springqueue.models.task.Task;
import com.springqbackend.springqueue.enums.TaskStatus;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// NOTE: Pretty sure this class should be a service! <-- Check later when refactoring to see if I should do that...
// NOTE: ^ In which case I should *maybe* rename this file to QueueService?

public class Queue {
    // Fields:
    //private int tasksCapacity;
    private final BlockingQueue<Task> tasks;
    private final ConcurrentHashMap<String,Task> jobs;
    private final Lock lock;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();

    //private ReentrantReadWriteLock lock;
    // ^ In my GoQueue project this was "mu" for mutex but doesn't make sense here.

    // Constructor:
    Queue(int tasksCapacity) {
        //this.tasksCapacity = tasksCapacity;
        this.tasks = new LinkedBlockingQueue<>(tasksCapacity);
        this.jobs = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
    }

    /* DEBUG:+NOTE: Just focus on translating the concepts first.
    Figure out later how the getter and setter methods etc would work for a class like this... */

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
            throw new RuntimeException(e);  // <-- NOTE: This extra "catch(InterruptedException e)" block was added auto to fix error. Need more clarity on its inclusion. (Maybe it should be in the function instead).
        } finally {
            lock.unlock();
        }
    }

    // 2. Translating GoQueue's "func (q * Queue) Dequeue() * task.Task {...}" function:
    public Task Dequeue() throws InterruptedException {
        return tasks.take();    // "Blocks if the queue is still empty until an element becomes available."
    }

    // 3. Translating GoQueue's "func (q * Queue) Clear() {...}" function:
    // (This is the method for "emptying the queue").
    public void clear() {
        lock.lock();
        try {
            jobs.clear();   // This will clear the ConcurrentHashMap.
            /* NOTE: In Go, I used a loop that would pull out a value from the Queue per iteration
            and basically repeat until it was empty. I think the loop below should basically suffice? */
            while (!tasks.isEmpty()) {
                tasks.take();
            }
        } catch(InterruptedException e) {
          throw new RuntimeException(e);  // <-- NOTE: This extra "catch(InterruptedException e)" block was added auto to fix error. Need more clarity on its inclusion. (Maybe it should be in the function instead).
        } finally {
            lock.unlock();
        }
    }

    // 4. Translating GoQueue's "func (q * Queue) GetJobs() []*task.Task {...}" function:
    // This is the method for returning a copy of all the Jobs (Tasks) we have:
    public Task[] getJobs() {
        readLock.lock();    // "read lock" only (in GoQueue: q.mu.RLock();)
        Task[] allTasks = new Task[tasks.size()];
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

    // TO-DO: Fill in the mandatory-practice methods (e.g., setters and getters) latter -- to practice muscle memory.
}
