package com.example.ActivityService.repositories;

import com.example.ActivityService.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepo extends JpaRepository<Session, Long> {


}
