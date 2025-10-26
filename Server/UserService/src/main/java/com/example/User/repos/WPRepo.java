package com.example.User.repos;

import com.example.User.entities.Waypoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WPRepo extends JpaRepository<Waypoint,Long> {
}
