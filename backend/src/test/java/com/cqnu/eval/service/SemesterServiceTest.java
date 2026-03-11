package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.ScoringConfigMapper;
import com.cqnu.eval.mapper.SemesterMapper;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.model.dto.AdminSemesterCreateRequest;
import com.cqnu.eval.model.entity.ScoringConfigEntity;
import com.cqnu.eval.model.entity.SemesterEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private SemesterService semesterService;

    @BeforeEach
    void setUp() {
        semesterService = new SemesterService(semesterMapper, submissionMapper, scoringConfigMapper);
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
        sourceCfg.setwSport(0.12);
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
        assertEquals(sourceCfg.getwSport(), inserted.getwSport());
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
}

