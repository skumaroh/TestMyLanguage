package com.infotech.lang.application.data.repository;

import com.infotech.lang.application.data.entity.Status;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, Integer> {
    
    Optional<Status> findByName(String name);

}
