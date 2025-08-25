package com.dalhousie.dalhousie_marketplace_backend.repository;
import com.dalhousie.dalhousie_marketplace_backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Category findByName(String name);
}
