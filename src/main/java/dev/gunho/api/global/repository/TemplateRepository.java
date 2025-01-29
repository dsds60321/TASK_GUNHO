package dev.gunho.api.global.repository;

import dev.gunho.api.global.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    Template getById(String id);
}
