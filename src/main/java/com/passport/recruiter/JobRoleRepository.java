package com.passport.recruiter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRoleRepository extends JpaRepository<JobRole, UUID> {
}
