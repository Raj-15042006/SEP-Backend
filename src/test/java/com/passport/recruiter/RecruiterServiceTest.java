package com.passport.recruiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruiterServiceTest {

    @Mock
    private JobRoleRepository jobRoleRepository;

    @InjectMocks
    private RecruiterService recruiterService;

    @Test
    @DisplayName("Should search candidates filtered by verified evidence and sort by match score")
    void testSearchCandidatesVerifiedOnly() {
        RecruiterService.SearchCriteria criteria = RecruiterService.SearchCriteria.builder()
            .verifiedOnly(true)
            .minMatchScore(80)
            .build();

        List<RecruiterService.CandidateSummaryDto> results = recruiterService.searchCandidates(criteria);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(c -> c.getVerifiedCount() > 0));
        assertTrue(results.stream().allMatch(c -> c.getMatchPercentage() >= 80));

        // Verify descending order by match percentage
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getMatchPercentage() >= results.get(i + 1).getMatchPercentage());
        }
    }

    @Test
    @DisplayName("Should calculate explainable role readiness with strengths and gap diagnostics")
    void testCalculateRoleReadiness() {
        UUID roleId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        JobRole role = JobRole.builder()
            .id(roleId)
            .title("Distributed Systems Engineer")
            .department("Core Infrastructure")
            .build();

        when(jobRoleRepository.findById(roleId)).thenReturn(Optional.of(role));

        RecruiterService.DetailedReadinessDto readiness = recruiterService.calculateRoleReadiness(studentId, roleId);

        assertNotNull(readiness);
        assertEquals("Distributed Systems Engineer", readiness.getRoleTitle());
        assertNotNull(readiness.getSkillDiagnostics());
        assertFalse(readiness.getSkillDiagnostics().isEmpty());
        assertNotNull(readiness.getKeyStrengths());
        assertNotNull(readiness.getActionableGaps());
        assertTrue(readiness.getOverallMatchPercentage() >= 0 && readiness.getOverallMatchPercentage() <= 100);
    }
}
