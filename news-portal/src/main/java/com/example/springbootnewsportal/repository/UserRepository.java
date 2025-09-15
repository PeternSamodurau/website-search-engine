package com.example.springbootnewsportal.repository;

import com.example.springbootnewsportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
