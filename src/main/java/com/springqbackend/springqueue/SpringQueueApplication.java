package com.springqbackend.springqueue;

import com.springqbackend.springqueue.service.QueueService;
import com.springqbackend.springqueue.runtime.Worker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// NOTE-TO-SELF: In the context of remaking my GoQueue project, this would be my main.go file.

@SpringBootApplication
public class SpringQueueApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringQueueApplication.class, args);
	}

    /*@Bean
    public QueueService queue() {
        return new QueueService(100);  // In my GoQueue project's main.go file: queue.NewQueue(100)
    }*/
    // DEBUG: ^ No longer needed as part of the ExecutorService refactor (have @Service now in QueueService).

    /*@Bean
    public CommandLineRunner initWorkers(QueueService queue) {
        // This would be equivalent to the code block in my GoQueue's main.go where I spawn 3 worker goroutines (threads):
        return args -> {
            int workerCount = 3;
            for(int i = 1; i <= workerCount; i++) {
                Thread workerThread = new Thread(new Worker(i, queue));
                workerThread.start();
            }
            System.out.println("The 3 workers have been started. (Worker pool has been initialized).");
        };
    }*/
    /* DEBUG: ^ As part of the ExecutorService Refactor, this is no longer necessary (the ExecutorService
    field in the QueueService class will abstract / do this automatically when instantiated / injected by SpringBoot).
    */

}
