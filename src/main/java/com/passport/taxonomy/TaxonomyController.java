package com.passport.taxonomy;

import com.passport.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/taxonomy")
@RequiredArgsConstructor
@Tag(name = "Skill Taxonomy", description = "Versioned academic skill definitions and catalog")
public class TaxonomyController {

    private final TaxonomyService taxonomyService;

    @GetMapping("/skills")
    @Operation(summary = "Retrieve all active skills in taxonomy")
    public ResponseEntity<ApiResponse<List<Skill>>> listSkills(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String query
    ) {
        if (query != null && !query.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(taxonomyService.searchSkills(query)));
        }
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(taxonomyService.getSkillsByCategory(category)));
        }
        return ResponseEntity.ok(ApiResponse.ok(taxonomyService.getAllSkills()));
    }

    @GetMapping("/skills/{id}")
    @Operation(summary = "Get single skill node by UUID")
    public ResponseEntity<ApiResponse<Skill>> getSkill(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(taxonomyService.getSkillById(id)));
    }

    @PostMapping("/skills")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new skill definition (Admin only)")
    public ResponseEntity<ApiResponse<Skill>> createSkill(@RequestBody Skill skill) {
        Skill created = taxonomyService.createSkill(skill);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Skill created", created));
    }
}
