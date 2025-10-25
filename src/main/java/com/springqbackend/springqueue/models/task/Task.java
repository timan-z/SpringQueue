package com.springqbackend.springqueue.models.task;

// NOTE-TO-SELF: In the context of remaking my GoQueue project, this would be my Task.go file.

import com.springqbackend.springqueue.enums.TaskStatus;
import java.util.Objects;

public class Task {
    // Fields:
    private String id;
    private String payload;
    private String type;    // <-- NOTE: Probably change this to Enum or something.
    private TaskStatus status;
    private int attempts;
    private int maxRetries;
    private String createdAt;

    // Constructor:
    public Task(String id, String payload, String type, TaskStatus status, int attempts, int maxRetries, String createdAt) {
        this.id = id;
        this.payload = payload;
        this.type = type;
        this.status = status;
        this.attempts = attempts;
        this.maxRetries = maxRetries;
        this.createdAt = createdAt;
    }

    // Getter methods:
    public String getId() {
        return id;
    }
    public String getPayload() {
        return payload;
    }
    public String getType() {
        return type;
    }
    public TaskStatus getStatus() {
        return status;
    }
    public int getAttempts() {
        return attempts;
    }
    public int getMaxRetries() {
        return maxRetries;
    }
    public String getCreatedAt() {
        return createdAt;
    }

    // Setter methods:
    public void setId(String id) {
        this.id = id;
    }
    public void setPayload(String payload) {
        this.payload = payload;
    }
    public void setType(String type) {
        this.type = type;
    }
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // toString():
    @Override
    public String toString() {
        return "Task: [id: " + id + ", payload: " + payload + ", type: " + type + ", status: " + status + ", attempts: " + attempts + ", maxRetries: " + maxRetries + ", createdAt: " + createdAt + "]";
    }
    // hashCode():
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (id == null ? 0: id.hashCode()); // NOTE: Not a numerical id! (stored in String for ar reason).
        hash = 31 * hash + (payload == null ? 0: payload.hashCode());
        hash = 31 * hash + (type == null ? 0: type.hashCode());
        hash = 31 * hash + (status == null ? 0: status.hashCode());
        hash = 31 * hash + attempts;
        hash = 31 * hash + maxRetries;
        hash = 31 * hash + (createdAt == null ? 0: createdAt.hashCode());
        return hash;
    }
    // equals():
    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id) && Objects.equals(payload, task.payload) && Objects.equals(type, task.type) && status == task.status && attempts == task.attempts && maxRetries == task.maxRetries && Objects.equals(createdAt, task.createdAt);
    }
}
