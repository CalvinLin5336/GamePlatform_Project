package com.example.demo.modules.user.service;

import com.example.demo.modules.user.dto.DashboardResponse;
import com.example.demo.modules.user.repository.DashboardRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardResponse summary() {
        return new DashboardResponse(
                dashboardRepository.countUsers(),
                dashboardRepository.countActiveUsers(),
                dashboardRepository.countDisabledUsers(),
                dashboardRepository.countAdmins(),
                dashboardRepository.countTodayOperations()
        );
    }
}
