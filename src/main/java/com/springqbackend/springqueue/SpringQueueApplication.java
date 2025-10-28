package com.springqbackend.springqueue;

import com.springqbackend.springqueue.models.queue.Queue;
import com.springqbackend.springqueue.system.worker.Worker;
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

    @Bean
    public Queue queue() {
        return new Queue(100);  // In my GoQueue project's main.go file: queue.NewQueue(100)
    }

    @Bean
    public CommandLineRunner initWorkers(Queue queue) {
        // This would be equivalent to the code block in my GoQueue's main.go where I spawn 3 worker goroutines (threads):
        return args -> {
            int workerCount = 3;
            for(int i = 1; i <= workerCount; i++) {
                Thread workerThread = new Thread(new Worker(i, queue));
                workerThread.start();
            }
            System.out.println("The 3 workers have been started. (Worker pool has been initialized).");
        };
    }

}
