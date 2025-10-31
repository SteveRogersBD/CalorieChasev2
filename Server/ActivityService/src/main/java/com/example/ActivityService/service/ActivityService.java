package com.example.ActivityService.service;

import com.example.ActivityService.repositories.ActivityRepo;
import com.example.ActivityService.entity.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityService {

    @Autowired
    private ActivityRepo activityRepo;

    public List<Session> getAllSessions() {
        return activityRepo.findAll();
    }

    public Session getSessionById(Long id) {
        return activityRepo.findById(id).orElseThrow();
    }

    public Session createSession(Session session) {
        return activityRepo.save(session);
    }

    public Session updateSession(Long id, Session session) {
        session.setId(id);
        return activityRepo.save(session);
    }

    public void deleteSession(Long id) {
        activityRepo.deleteById(id);
    }

}
