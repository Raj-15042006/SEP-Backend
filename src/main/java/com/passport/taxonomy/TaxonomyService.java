package com.passport.taxonomy;

import com.passport.common.Audited;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxonomyService {

    private final SkillRepository skillRepository;

    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    public List<Skill> getSkillsByCategory(String category) {
        if ("ALL".equalsIgnoreCase(category)) {
            return skillRepository.findAll();
        }
        return skillRepository.findByCategoryIgnoreCase(category);
    }

    public List<Skill> searchSkills(String query) {
        return skillRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
    }

    public Skill getSkillById(UUID id) {
        return skillRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Skill not found with ID: " + id));
    }

    @Audited(action = "SKILL_CREATED", resourceType = "TAXONOMY")
    @Transactional
    public Skill createSkill(Skill skill) {
        return skillRepository.save(skill);
    }
}
