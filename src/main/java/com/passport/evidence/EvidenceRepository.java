package com.passport.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {
    List<Evidence> findByStudentIdOrderBySubmittedAtDesc(UUID studentId);
    List<Evidence> findByStatusOrderBySubmittedAtDesc(EvidenceStatus status);
    List<Evidence> findBySkillId(UUID skillId);
}
