package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.ScoringConfigMapper;
import com.cqnu.eval.mapper.SemesterMapper;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.model.dto.AdminSemesterCreateRequest;
import com.cqnu.eval.model.entity.ScoringConfigEntity;
import com.cqnu.eval.model.entity.SemesterEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class SemesterService {

    private final SemesterMapper semesterMapper;
    private final SubmissionMapper submissionMapper;
    private final ScoringConfigMapper scoringConfigMapper;

    public SemesterService(SemesterMapper semesterMapper,
                           SubmissionMapper submissionMapper,
                           ScoringConfigMapper scoringConfigMapper) {
        this.semesterMapper = semesterMapper;
        this.submissionMapper = submissionMapper;
        this.scoringConfigMapper = scoringConfigMapper;
    }

    public List<SemesterEntity> listAll() {
        return semesterMapper.listAll();
    }

    public SemesterEntity findActive() {
        return semesterMapper.findActive();
    }

    public Map<String, Object> getAdminOverview() {
        SemesterEntity active = semesterMapper.findActive();
        long pending = 0;
        if (active != null && active.getId() != null) {
            pending = submissionMapper.countSubmittedBySemester(active.getId());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("activeSemester", active);
        out.put("submittedPendingCount", pending);
        out.put("semesters", semesterMapper.listAll());
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public SemesterEntity createSemester(AdminSemesterCreateRequest request) {
        if (request == null) {
            throw new BizException(40001, "参数不能为空");
        }

        String name = normalizeName(request.getName());
        String season = normalizeSeason(request.getSeason());
        int termNum = seasonToTermNum(season);

        SemesterEntity entity = new SemesterEntity();
        entity.setName(name);
        entity.setYear(request.getYear());
        entity.setTerm(termNum);
        entity.setIsActive(0);
        semesterMapper.insert(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public SemesterEntity activateSemester(Long semesterId) {
        if (semesterId == null) {
            throw new BizException(40001, "学期ID不能为空");
        }

        SemesterEntity currentActive = semesterMapper.findActive();
        if (currentActive != null && Objects.equals(currentActive.getId(), semesterId)) {
            ensureScoringConfigExists(semesterId, currentActive);
            return currentActive;
        }

        if (currentActive != null && currentActive.getId() != null) {
            long pending = submissionMapper.countSubmittedBySemester(currentActive.getId());
            if (pending > 0) {
                throw new BizException(40901, "当前学期仍有" + pending + "份待审核，无法切换");
            }
        }

        SemesterEntity target = semesterMapper.findById(semesterId);
        if (target == null) {
            throw new BizException(40401, "学期不存在");
        }

        ensureScoringConfigExists(semesterId, currentActive);

        semesterMapper.deactivateAll();
        semesterMapper.activateById(semesterId);

        SemesterEntity activated = semesterMapper.findById(semesterId);
        if (activated != null) {
            activated.setIsActive(1);
        }
        return activated == null ? target : activated;
    }

    private void ensureScoringConfigExists(Long targetSemesterId, SemesterEntity sourceSemester) {
        if (targetSemesterId == null) {
            return;
        }

        ScoringConfigEntity target = scoringConfigMapper.findBySemesterId(targetSemesterId);
        if (target != null) {
            return;
        }

        ScoringConfigEntity sourceCfg = null;
        if (sourceSemester != null && sourceSemester.getId() != null) {
            sourceCfg = scoringConfigMapper.findBySemesterId(sourceSemester.getId());
        }

        if (sourceCfg == null) {
            scoringConfigMapper.insertDefault(targetSemesterId);
            return;
        }

        ScoringConfigEntity copied = new ScoringConfigEntity();
        copied.setSemesterId(targetSemesterId);
        copied.setAppealDays(sourceCfg.getAppealDays());
        copied.setPrecedenceMode(sourceCfg.getPrecedenceMode());
        copied.setScoreModel(sourceCfg.getScoreModel());
        copied.setwMoral(sourceCfg.getwMoral());
        copied.setwIntel(sourceCfg.getwIntel());
        copied.setwSport(sourceCfg.getwSport());
        copied.setwArt(sourceCfg.getwArt());
        copied.setwLabor(sourceCfg.getwLabor());
        copied.setCapMoral(sourceCfg.getCapMoral());
        copied.setCapIntel(sourceCfg.getCapIntel());
        copied.setCapSport(sourceCfg.getCapSport());
        copied.setCapArt(sourceCfg.getCapArt());
        copied.setCapLabor(sourceCfg.getCapLabor());
        scoringConfigMapper.insert(copied);
    }

    private String normalizeName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw new BizException(40001, "学期名称不能为空");
        }
        if (value.length() > 64) {
            throw new BizException(40001, "学期名称不能超过64位");
        }
        return value;
    }

    private String normalizeSeason(String season) {
        String value = season == null ? "" : season.trim().toUpperCase(Locale.ROOT);
        if (!"SPRING".equals(value) && !"AUTUMN".equals(value)) {
            throw new BizException(40001, "学期季节不合法");
        }
        return value;
    }

    private int seasonToTermNum(String season) {
        if ("SPRING".equals(season)) return 1;
        if ("AUTUMN".equals(season)) return 2;
        return 1;
    }
}

