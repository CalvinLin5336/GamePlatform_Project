package com.example.demo.modules.user.entity;

public class Status {
    private Long id;
    private String statusName;

    public Status() {}

    public Status(Long id, String statusName) {
        this.id = id;
        this.statusName = statusName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
}
