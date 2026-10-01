package com.passport.rubric;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RubricScoreRepository extends JpaRepository<RubricScore, UUID> {
    List<RubricScore> findByVerificationId(UUID verificationId);
}
