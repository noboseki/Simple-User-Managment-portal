package com.noboseki.supportportal.repository;

import com.noboseki.supportportal.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findUserByUsername(String user);

    Optional<User> findUserByEmail(String email);
}
