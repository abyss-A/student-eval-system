package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.ScoringConfigMapper;
import com.cqnu.eval.mapper.SemesterMapper;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.model.dto.AdminScoringConfigUpdateRequest;
import com.cqnu.eval.model.dto.AdminSemesterCreateRequest;
import com.cqnu.eval.model.entity.ScoringConfigEntity;
import com.cqnu.eval.model.entity.SemesterEntity;
import com.cqnu.eval.model.entity.SubmissionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemesterServiceTest {

    @Mock
    private SemesterMapper semesterMapper;

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private ScoringConfigMapper scoringConfigMapper;

    @Mock
    private SubmissionService submissionService;

    private SemesterService semesterService;

    @BeforeEach
    void setUp() {
        semesterService = new SemesterService(semesterMapper, submissionMapper, scoringConfigMapper, submissionService);
    }

    @Test
    void activateSemester_blocksWhenCurrentHasSubmitted() {
        SemesterEntity active = new SemesterEntity();
        active.setId(1L);
        when(semesterMapper.findActive()).thenReturn(active);
        when(submissionMapper.countSubmittedBySemester(1L)).thenReturn(3L);

        BizException ex = assertThrows(BizException.class, () -> semesterService.activateSemester(2L));

        assertEquals(40901, ex.getCode());
        assertTrue(ex.getMessage().contains("3"));
        verify(semesterMapper, never()).deactivateAll();
        verify(semesterMapper, never()).activateById(any());
        verify(scoringConfigMapper, never()).insert(any());
        verify(scoringConfigMapper, never()).insertDefault(any());
    }

    @Test
    void activateSemester_copiesScoringConfigWhenTargetMissing() {
        SemesterEntity active = new SemesterEntity();
        active.setId(1L);
        when(semesterMapper.findActive()).thenReturn(active);
        when(submissionMapper.countSubmittedBySemester(1L)).thenReturn(0L);

        SemesterEntity target = new SemesterEntity();
        target.setId(2L);
        target.setIsActive(0);
        when(semesterMapper.findById(2L)).thenReturn(target);

        when(scoringConfigMapper.findBySemesterId(2L)).thenReturn(null);
        ScoringConfigEntity sourceCfg = new ScoringConfigEntity();
        sourceCfg.setId(10L);
        sourceCfg.setSemesterId(1L);
        sourceCfg.setAppealDays(12);
        sourceCfg.setPrecedenceMode("COLLEGE_FIRST");
        sourceCfg.setScoreModel("STRICT_FORMULA");
        sourceCfg.setwMoral(0.11);
        sourceCfg.setwIntel(0.66);
        sourceCfg.setIntelCourseRatio(0.90);
        sourceCfg.setIntelInnovationRatio(0.10);
        sourceCfg.setwSport(0.12);
        sourceCfg.setSportUniversityPeRatio(0.80);
        sourceCfg.setSportActivityRatio(0.20);
        sourceCfg.setwArt(0.05);
        sourceCfg.setwLabor(0.06);
        sourceCfg.setCapMoral(90.0);
        sourceCfg.setCapIntel(91.0);
        sourceCfg.setCapSport(92.0);
        sourceCfg.setCapArt(93.0);
        sourceCfg.setCapLabor(94.0);
        when(scoringConfigMapper.findBySemesterId(1L)).thenReturn(sourceCfg);
        when(scoringConfigMapper.insert(any(ScoringConfigEntity.class))).thenReturn(1);

        when(semesterMapper.deactivateAll()).thenReturn(1);
        when(semesterMapper.activateById(2L)).thenReturn(1);

        SemesterEntity result = semesterService.activateSemester(2L);

        assertNotNull(result);
        assertEquals(1, result.getIsActive());

        ArgumentCaptor<ScoringConfigEntity> cfgCaptor = ArgumentCaptor.forClass(ScoringConfigEntity.class);
        verify(scoringConfigMapper, times(1)).insert(cfgCaptor.capture());
        ScoringConfigEntity inserted = cfgCaptor.getValue();
        assertEquals(2L, inserted.getSemesterId());
        assertEquals(sourceCfg.getAppealDays(), inserted.getAppealDays());
        assertEquals(sourceCfg.getPrecedenceMode(), inserted.getPrecedenceMode());
        assertEquals(sourceCfg.getScoreModel(), inserted.getScoreModel());
        assertEquals(sourceCfg.getwMoral(), inserted.getwMoral());
        assertEquals(sourceCfg.getwIntel(), inserted.getwIntel());
        assertEquals(sourceCfg.getIntelCourseRatio(), inserted.getIntelCourseRatio());
        assertEquals(sourceCfg.getIntelInnovationRatio(), inserted.getIntelInnovationRatio());
        assertEquals(sourceCfg.getwSport(), inserted.getwSport());
        assertEquals(sourceCfg.getSportUniversityPeRatio(), inserted.getSportUniversityPeRatio());
        assertEquals(sourceCfg.getSportActivityRatio(), inserted.getSportActivityRatio());
        assertEquals(sourceCfg.getwArt(), inserted.getwArt());
        assertEquals(sourceCfg.getwLabor(), inserted.getwLabor());
        assertEquals(sourceCfg.getCapMoral(), inserted.getCapMoral());
        assertEquals(sourceCfg.getCapIntel(), inserted.getCapIntel());
        assertEquals(sourceCfg.getCapSport(), inserted.getCapSport());
        assertEquals(sourceCfg.getCapArt(), inserted.getCapArt());
        assertEquals(sourceCfg.getCapLabor(), inserted.getCapLabor());

        verify(scoringConfigMapper, never()).insertDefault(any());
    }

    @Test
    void activateSemester_insertsDefaultConfigWhenNoSourceConfig() {
        SemesterEntity active = new SemesterEntity();
        active.setId(1L);
        when(semesterMapper.findActive()).thenReturn(active);
        when(submissionMapper.countSubmittedBySemester(1L)).thenReturn(0L);

        SemesterEntity target = new SemesterEntity();
        target.setId(2L);
        when(semesterMapper.findById(2L)).thenReturn(target);

        when(scoringConfigMapper.findBySemesterId(2L)).thenReturn(null);
        when(scoringConfigMapper.findBySemesterId(1L)).thenReturn(null);
        when(scoringConfigMapper.insertDefault(2L)).thenReturn(1);

        semesterService.activateSemester(2L);

        verify(scoringConfigMapper, times(1)).insertDefault(2L);
        verify(scoringConfigMapper, never()).insert(any());
    }

    @Test
    void createSemester_setsInactiveByDefault() {
        doAnswer(invocation -> {
            SemesterEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        }).when(semesterMapper).insert(any(SemesterEntity.class));

        AdminSemesterCreateRequest request = new AdminSemesterCreateRequest();
        request.setYear(2026);
        request.setSeason("SPRING");
        request.setName(" 2026年春季学期 ");

        SemesterEntity created = semesterService.createSemester(request);

        assertEquals(99L, created.getId());
        assertEquals(0, created.getIsActive());
        assertEquals(2026, created.getYear());
        assertEquals(1, created.getTerm());
        assertEquals("2026年春季学期", created.getName());

        ArgumentCaptor<SemesterEntity> captor = ArgumentCaptor.forClass(SemesterEntity.class);
        verify(semesterMapper).insert(captor.capture());
        SemesterEntity inserted = captor.getValue();
        assertEquals(0, inserted.getIsActive());
        assertEquals("2026年春季学期", inserted.getName());
        assertEquals(2026, inserted.getYear());
        assertEquals(1, inserted.getTerm());
    }

    @Test
    void updateName_validatesAndUpdates() {
        SemesterEntity existing = new SemesterEntity();
        existing.setId(1L);
        existing.setName("2026年春季学期");
        SemesterEntity updated = new SemesterEntity();
        updated.setId(1L);
        updated.setName("2026年春季学期（更名）");
        when(semesterMapper.findById(1L)).thenReturn(existing, updated);

        SemesterEntity result = semesterService.renameSemester(1L, " 2026年春季学期（更名） ");

        assertNotNull(result);
        assertEquals("2026年春季学期（更名）", result.getName());
        verify(semesterMapper, times(1)).updateName(1L, "2026年春季学期（更名）");
    }

    @Test
    void deleteSemester_blocksWhenActiveOrHasSubmissions() {
        SemesterEntity active = new SemesterEntity();
        active.setId(1L);
        active.setIsActive(1);
        when(semesterMapper.findById(1L)).thenReturn(active);

        BizException ex = assertThrows(BizException.class, () -> semesterService.deleteSemester(1L));

        assertEquals(40902, ex.getCode());
        verify(scoringConfigMapper, never()).deleteBySemesterId(any());
        verify(semesterMapper, never()).deleteById(any());

        SemesterEntity nonActive = new SemesterEntity();
        nonActive.setId(2L);
        nonActive.setIsActive(0);
        when(semesterMapper.findById(2L)).thenReturn(nonActive);
        when(submissionMapper.countBySemester(2L)).thenReturn(5L);

        BizException ex2 = assertThrows(BizException.class, () -> semesterService.deleteSemester(2L));

        assertEquals(40903, ex2.getCode());
        assertTrue(ex2.getMessage().contains("5"));
        verify(scoringConfigMapper, never()).deleteBySemesterId(eq(2L));
        verify(semesterMapper, never()).deleteById(eq(2L));
    }

    @Test
    void upsertScoringConfig_insertsWhenMissing_updatesWhenExists() {
        SemesterEntity semester = new SemesterEntity();
        semester.setId(1L);
        when(semesterMapper.findById(1L)).thenReturn(semester);

        ScoringConfigEntity existing = new ScoringConfigEntity();
        existing.setId(10L);
        existing.setSemesterId(1L);
        existing.setAppealDays(10);
        existing.setPrecedenceMode("COLLEGE_FIRST");
        existing.setScoreModel("STRICT_FORMULA");
        existing.setwMoral(0.15);
        existing.setwIntel(0.60);
        existing.setIntelCourseRatio(0.85);
        existing.setIntelInnovationRatio(0.15);
        existing.setwSport(0.10);
        existing.setSportUniversityPeRatio(0.85);
        existing.setSportActivityRatio(0.15);
        existing.setwArt(0.075);
        existing.setwLabor(0.075);
        existing.setCapMoral(100.0);
        existing.setCapIntel(100.0);
        existing.setCapSport(100.0);
        existing.setCapArt(100.0);
        existing.setCapLabor(100.0);

        AdminScoringConfigUpdateRequest request = new AdminScoringConfigUpdateRequest();
        request.setwMoral(0.10);
        request.setwIntel(0.60);
        request.setIntelCourseRatio(0.90);
        request.setIntelInnovationRatio(0.10);
        request.setwSport(0.10);
        request.setSportUniversityPeRatio(0.80);
        request.setSportActivityRatio(0.20);
        request.setwArt(0.10);
        request.setwLabor(0.10);
        request.setCapMoral(90.0);
        request.setCapIntel(91.0);
        request.setCapSport(92.0);
        request.setCapArt(93.0);
        request.setCapLabor(94.0);

        when(scoringConfigMapper.findBySemesterId(1L)).thenReturn(null, existing, existing);
        when(scoringConfigMapper.insertDefault(1L)).thenReturn(1);
        when(scoringConfigMapper.update(any(ScoringConfigEntity.class))).thenReturn(1);

        ScoringConfigEntity result = semesterService.upsertScoringConfig(1L, request);

        assertNotNull(result);
        verify(scoringConfigMapper, times(1)).insertDefault(1L);

        ArgumentCaptor<ScoringConfigEntity> captor = ArgumentCaptor.forClass(ScoringConfigEntity.class);
        verify(scoringConfigMapper, times(1)).update(captor.capture());
        ScoringConfigEntity updatedEntity = captor.getValue();
        assertEquals(1L, updatedEntity.getSemesterId());
        assertEquals(0.10, updatedEntity.getwMoral());
        assertEquals(0.60, updatedEntity.getwIntel());
        assertEquals(0.90, updatedEntity.getIntelCourseRatio());
        assertEquals(0.10, updatedEntity.getIntelInnovationRatio());
        assertEquals(0.10, updatedEntity.getwSport());
        assertEquals(0.80, updatedEntity.getSportUniversityPeRatio());
        assertEquals(0.20, updatedEntity.getSportActivityRatio());
        assertEquals(0.10, updatedEntity.getwArt());
        assertEquals(0.10, updatedEntity.getwLabor());
        assertEquals(90.0, updatedEntity.getCapMoral());
        assertEquals(91.0, updatedEntity.getCapIntel());
        assertEquals(92.0, updatedEntity.getCapSport());
        assertEquals(93.0, updatedEntity.getCapArt());
        assertEquals(94.0, updatedEntity.getCapLabor());
    }

    @Test
    void upsertScoringConfig_blocksWhenSubRatiosInvalid() {
        SemesterEntity semester = new SemesterEntity();
        semester.setId(1L);
        when(semesterMapper.findById(1L)).thenReturn(semester);

        AdminScoringConfigUpdateRequest request = new AdminScoringConfigUpdateRequest();
        request.setwMoral(0.15);
        request.setwIntel(0.60);
        request.setwSport(0.10);
        request.setwArt(0.075);
        request.setwLabor(0.075);
        request.setIntelCourseRatio(0.90);
        request.setIntelInnovationRatio(0.20);
        request.setSportUniversityPeRatio(0.85);
        request.setSportActivityRatio(0.15);
        request.setCapMoral(100.0);
        request.setCapIntel(100.0);
        request.setCapSport(100.0);
        request.setCapArt(100.0);
        request.setCapLabor(100.0);

        BizException ex = assertThrows(BizException.class, () -> semesterService.upsertScoringConfig(1L, request));

        assertEquals(40001, ex.getCode());
        assertTrue(ex.getMessage().contains("智育二级占比"));
        verify(scoringConfigMapper, never()).insertDefault(any());
        verify(scoringConfigMapper, never()).update(any());
    }

    @Test
    void recalculateSemester_callsRecalculateForEachSubmissionId() {
        SemesterEntity semester = new SemesterEntity();
        semester.setId(1L);
        when(semesterMapper.findById(1L)).thenReturn(semester);
        when(submissionMapper.listIdsForRecalculate(1L)).thenReturn(List.of(10L, 11L));
        when(submissionService.recalculate(10L)).thenReturn(new SubmissionEntity());
        when(submissionService.recalculate(11L)).thenReturn(new SubmissionEntity());

        Map<String, Object> result = semesterService.recalculateSemester(1L);

        assertEquals(2, result.get("total"));
        assertEquals(2, result.get("updated"));
        verify(submissionService, times(1)).recalculate(10L);
        verify(submissionService, times(1)).recalculate(11L);
    }
}
