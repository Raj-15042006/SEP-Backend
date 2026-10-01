package com.passport.taxonomy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByCategoryIgnoreCase(String category);
    List<Skill> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);
}
