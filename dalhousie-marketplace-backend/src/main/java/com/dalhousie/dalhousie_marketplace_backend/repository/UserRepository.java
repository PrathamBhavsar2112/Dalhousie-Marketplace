package com.dalhousie.dalhousie_marketplace_backend.repository;
import com.dalhousie.dalhousie_marketplace_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByBannerId(String netId);
}

