package com.example.ActivityService.controller;

import com.example.ActivityService.entity.Session;
import com.example.ActivityService.response.ApiResponse;
import com.example.ActivityService.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/session")
public class SessionController {
    
    @Autowired
    private ActivityService activityService;

    @PostMapping
    public ApiResponse<Session> createSession(@RequestBody Session session) {
        return ApiResponse.onSuccess("Session created successfully!!!",
                activityService.createSession(session));
    }

    @GetMapping
    public ApiResponse<List<Session>> getAllSessions() {
        return ApiResponse.onSuccess("Sessions retrieved successfully!!!",
                activityService.getAllSessions());
    }

    @GetMapping("/{id}")
    public ApiResponse<Session> getSessionById(@PathVariable Long id) {
        return ApiResponse.onSuccess("Session retrieved successfully!!!",
                activityService.getSessionById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Session> updateSession(@PathVariable Long id, @RequestBody Session session) {
        return ApiResponse.onSuccess("Session updated successfully!!!",
                activityService.updateSession(id, session));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        activityService.deleteSession(id);
        return ApiResponse.onSuccess("Session deleted successfully!!!", null);
    }
}
