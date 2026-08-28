package com.example.demo.modules.user.entity;

public class OperationLog {
    private Long id;
    private String account;
    private String action;
    private Long targetId;
    private String role;
    private String description;
    private String createdAt;

    public OperationLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
