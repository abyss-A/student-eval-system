package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.ScoringConfigMapper;
import com.cqnu.eval.mapper.SemesterMapper;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.model.dto.AdminScoringConfigUpdateRequest;
import com.cqnu.eval.model.dto.AdminSemesterCreateRequest;
import com.cqnu.eval.model.entity.ScoringConfigEntity;
import com.cqnu.eval.model.entity.SemesterEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final SubmissionService submissionService;

    public SemesterService(SemesterMapper semesterMapper,
                           SubmissionMapper submissionMapper,
                           ScoringConfigMapper scoringConfigMapper,
                           SubmissionService submissionService) {
        this.semesterMapper = semesterMapper;
        this.submissionMapper = submissionMapper;
        this.scoringConfigMapper = scoringConfigMapper;
        this.submissionService = submissionService;
    }

    public List<SemesterEntity> listAll() {
        return semesterMapper.listAll();
    }

    public SemesterEntity findActive() {
        return semesterMapper.findActive();
    }

    public ScoringConfigEntity getScoringConfig(Long semesterId) {
        SemesterEntity semester = requireSemester(semesterId);
        ScoringConfigEntity cfg = scoringConfigMapper.findBySemesterId(semester.getId());
        if (cfg != null) {
            return cfg;
        }
        return defaultScoringConfig(semester.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ScoringConfigEntity upsertScoringConfig(Long semesterId, AdminScoringConfigUpdateRequest request) {
        SemesterEntity semester = requireSemester(semesterId);
        if (request == null) {
            throw new BizException(40001, "参数不能为空");
        }
        validateScoringConfig(request);

        ScoringConfigEntity existing = scoringConfigMapper.findBySemesterId(semester.getId());
        if (existing == null) {
            scoringConfigMapper.insertDefault(semester.getId());
            existing = scoringConfigMapper.findBySemesterId(semester.getId());
            if (existing == null) {
                existing = defaultScoringConfig(semester.getId());
            }
        }

        existing.setwMoral(request.getwMoral());
        existing.setwIntel(request.getwIntel());
        existing.setIntelCourseRatio(request.getIntelCourseRatio());
        existing.setIntelInnovationRatio(request.getIntelInnovationRatio());
        existing.setwSport(request.getwSport());
        existing.setSportUniversityPeRatio(request.getSportUniversityPeRatio());
        existing.setSportActivityRatio(request.getSportActivityRatio());
        existing.setwArt(request.getwArt());
        existing.setwLabor(request.getwLabor());
        existing.setCapMoral(request.getCapMoral());
        existing.setCapIntel(request.getCapIntel());
        existing.setCapSport(request.getCapSport());
        existing.setCapArt(request.getCapArt());
        existing.setCapLabor(request.getCapLabor());

        if (request.getAppealDays() != null) {
            existing.setAppealDays(request.getAppealDays());
        }
        if (request.getPrecedenceMode() != null && !request.getPrecedenceMode().trim().isEmpty()) {
            existing.setPrecedenceMode(request.getPrecedenceMode().trim());
        }
        if (request.getScoreModel() != null && !request.getScoreModel().trim().isEmpty()) {
            existing.setScoreModel(request.getScoreModel().trim());
        }

        scoringConfigMapper.update(existing);
        ScoringConfigEntity updated = scoringConfigMapper.findBySemesterId(semester.getId());
        return updated == null ? existing : updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public SemesterEntity renameSemester(Long semesterId, String name) {
        SemesterEntity semester = requireSemester(semesterId);
        String normalized = normalizeName(name);
        semesterMapper.updateName(semester.getId(), normalized);
        SemesterEntity updated = semesterMapper.findById(semester.getId());
        if (updated != null) {
            return updated;
        }
        semester.setName(normalized);
        return semester;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSemester(Long semesterId) {
        SemesterEntity semester = requireSemester(semesterId);
        if (semester.getIsActive() != null && semester.getIsActive() == 1) {
            throw new BizException(40902, "当前学期不能删除");
        }

        long submissionCount = submissionMapper.countBySemester(semester.getId());
        if (submissionCount > 0) {
            throw new BizException(40903, "该学期下存在" + submissionCount + "份测评单，无法删除");
        }

        scoringConfigMapper.deleteBySemesterId(semester.getId());
        semesterMapper.deleteById(semester.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recalculateSemester(Long semesterId) {
        SemesterEntity semester = requireSemester(semesterId);
        List<Long> ids = submissionMapper.listIdsForRecalculate(semester.getId());
        List<Long> safeIds = ids == null ? new ArrayList<>() : ids;

        int updated = 0;
        for (Long id : safeIds) {
            if (id == null) {
                continue;
            }
            submissionService.recalculate(id);
            updated += 1;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", safeIds.size());
        out.put("updated", updated);
        return out;
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
        copied.setIntelCourseRatio(sourceCfg.getIntelCourseRatio() == null ? 0.85 : sourceCfg.getIntelCourseRatio());
        copied.setIntelInnovationRatio(sourceCfg.getIntelInnovationRatio() == null ? 0.15 : sourceCfg.getIntelInnovationRatio());
        copied.setwSport(sourceCfg.getwSport());
        copied.setSportUniversityPeRatio(sourceCfg.getSportUniversityPeRatio() == null ? 0.85 : sourceCfg.getSportUniversityPeRatio());
        copied.setSportActivityRatio(sourceCfg.getSportActivityRatio() == null ? 0.15 : sourceCfg.getSportActivityRatio());
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

    private SemesterEntity requireSemester(Long semesterId) {
        if (semesterId == null) {
            throw new BizException(40001, "学期ID不能为空");
        }
        SemesterEntity semester = semesterMapper.findById(semesterId);
        if (semester == null) {
            throw new BizException(40401, "学期不存在");
        }
        return semester;
    }

    private ScoringConfigEntity defaultScoringConfig(Long semesterId) {
        ScoringConfigEntity cfg = new ScoringConfigEntity();
        cfg.setSemesterId(semesterId);
        cfg.setwMoral(0.15);
        cfg.setwIntel(0.60);
        cfg.setIntelCourseRatio(0.85);
        cfg.setIntelInnovationRatio(0.15);
        cfg.setwSport(0.10);
        cfg.setSportUniversityPeRatio(0.85);
        cfg.setSportActivityRatio(0.15);
        cfg.setwArt(0.075);
        cfg.setwLabor(0.075);
        cfg.setCapMoral(100.0);
        cfg.setCapIntel(100.0);
        cfg.setCapSport(100.0);
        cfg.setCapArt(100.0);
        cfg.setCapLabor(100.0);
        cfg.setScoreModel("STRICT_FORMULA");
        cfg.setPrecedenceMode("COLLEGE_FIRST");
        cfg.setAppealDays(10);
        return cfg;
    }

    private void validateScoringConfig(AdminScoringConfigUpdateRequest request) {
        requireInRange(request.getwMoral(), 0, 1, "德育权重需在0~1之间");
        requireInRange(request.getwIntel(), 0, 1, "智育权重需在0~1之间");
        requireInRange(request.getwSport(), 0, 1, "体育权重需在0~1之间");
        requireInRange(request.getwArt(), 0, 1, "美育权重需在0~1之间");
        requireInRange(request.getwLabor(), 0, 1, "劳育权重需在0~1之间");

        requireInRange(request.getIntelCourseRatio(), 0, 1, "智育课程占比需在0~1之间");
        requireInRange(request.getIntelInnovationRatio(), 0, 1, "智育创新占比需在0~1之间");
        double intelRatioSum = request.getIntelCourseRatio() + request.getIntelInnovationRatio();
        if (Math.abs(intelRatioSum - 1.0) > 1e-6) {
            throw new BizException(40001, "智育二级占比之和需约等于1，当前为 " + intelRatioSum);
        }

        requireInRange(request.getSportUniversityPeRatio(), 0, 1, "大学体育占比需在0~1之间");
        requireInRange(request.getSportActivityRatio(), 0, 1, "体育活动占比需在0~1之间");
        double sportRatioSum = request.getSportUniversityPeRatio() + request.getSportActivityRatio();
        if (Math.abs(sportRatioSum - 1.0) > 1e-6) {
            throw new BizException(40001, "体育二级占比之和需约等于1，当前为 " + sportRatioSum);
        }

        double sum = request.getwMoral()
                + request.getwIntel()
                + request.getwSport()
                + request.getwArt()
                + request.getwLabor();
        if (Math.abs(sum - 1.0) > 1e-6) {
            throw new BizException(40001, "五项权重之和需约等于1，当前为 " + sum);
        }

        requireInRange(request.getCapMoral(), 0, 1000, "德育上限需在0~1000之间");
        requireInRange(request.getCapIntel(), 0, 1000, "智育上限需在0~1000之间");
        requireInRange(request.getCapSport(), 0, 1000, "体育上限需在0~1000之间");
        requireInRange(request.getCapArt(), 0, 1000, "美育上限需在0~1000之间");
        requireInRange(request.getCapLabor(), 0, 1000, "劳育上限需在0~1000之间");

        if (request.getAppealDays() != null) {
            int days = request.getAppealDays();
            if (days < 1 || days > 365) {
                throw new BizException(40001, "申诉天数需在1~365之间");
            }
        }
    }

    private void requireInRange(Double value, double min, double max, String message) {
        if (value == null || value.isNaN() || value < min || value > max) {
            throw new BizException(40001, message);
        }
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
