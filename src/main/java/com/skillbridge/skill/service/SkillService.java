package com.skillbridge.skill.service;

import java.util.List;
import java.util.UUID;

import com.skillbridge.common.ApiException;
import com.skillbridge.common.PageParams;
import com.skillbridge.common.PagedResult;
import com.skillbridge.security.CurrentUser;
import com.skillbridge.skill.domain.Skill;
import com.skillbridge.skill.dto.CreateSkillRequest;
import com.skillbridge.skill.dto.SkillResponse;
import com.skillbridge.skill.repository.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SkillService {

    private final SkillRepository skills;

    public SkillService(SkillRepository skills) {
        this.skills = skills;
    }

    @Transactional(readOnly = true)
    public PagedResult<SkillResponse> list(Boolean active, PageParams paging) {
        Pageable pageable = paging.pageable(PageParams.BY_NAME);
        Page<Skill> page = active == null
                ? skills.findAll(pageable)
                : skills.findByActive(active, pageable);
        return new PagedResult<>(page.getContent().stream().map(SkillResponse::from).toList(),
                page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public SkillResponse get(UUID id) {
        return skills.findById(id).map(SkillResponse::from)
                .orElseThrow(() -> ApiException.notFound("Skill not found"));
    }

    public SkillResponse create(CurrentUser actor, CreateSkillRequest req) {
        if (!actor.isAdmin()) {
            throw ApiException.forbidden();
        }
        if (skills.existsByNameIgnoreCase(req.name())) {
            throw ApiException.conflict("SKILL_NAME_TAKEN", "A skill with this name already exists");
        }
        return SkillResponse.from(skills.saveAndFlush(new Skill(req.name(), req.description(), req.category())));
    }
}