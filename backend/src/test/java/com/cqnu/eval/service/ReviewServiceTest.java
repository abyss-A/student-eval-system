package com.cqnu.eval.service;

import com.cqnu.eval.mapper.ActivityItemMapper;
import com.cqnu.eval.mapper.CourseItemMapper;
import com.cqnu.eval.mapper.ReviewLogMapper;
import com.cqnu.eval.mapper.SemesterMapper;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.mapper.UserMapper;
import com.cqnu.eval.model.entity.SemesterEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private CourseItemMapper courseItemMapper;

    @Mock
    private ActivityItemMapper activityItemMapper;

    @Mock
    private ReviewLogMapper reviewLogMapper;

    @Mock
    private SubmissionService submissionService;

    @Mock
    private SemesterMapper semesterMapper;

    @Mock
    private UserMapper userMapper;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                submissionMapper,
                courseItemMapper,
                activityItemMapper,
                reviewLogMapper,
                submissionService,
                semesterMapper,
                userMapper
        );
    }

    @Test
    void listClassOverview_mergesBaseAndPhaseCounts() {
        long counselorId = 2L;
        SemesterEntity active = new SemesterEntity();
        active.setId(1L);
        when(semesterMapper.findActive()).thenReturn(active);

        List<Map<String, Object>> base = new ArrayList<>();
        base.add(new HashMap<>(Map.of(
                "className", "2022级1班",
                "studentTotal", 30L,
                "notSubmittedCount", 2L,
                "submittedToAdminCount", 1L
        )));
        base.add(new HashMap<>(Map.of(
                "className", "2022级2班",
                "studentTotal", 25L,
                "notSubmittedCount", 0L,
                "submittedToAdminCount", 0L
        )));
        when(userMapper.listCounselorClassOverviewBase(eq(counselorId), eq(1L))).thenReturn(base);

        List<Map<String, Object>> tasks = List.of(
                new HashMap<>(Map.of("status", "SUBMITTED", "semester_id", 1L, "class_name", "2022级1班", "review_phase", "NOT_REVIEWED")),
                new HashMap<>(Map.of("status", "SUBMITTED", "semester_id", 1L, "class_name", "2022级1班", "review_phase", "READY_TO_SUBMIT")),
                new HashMap<>(Map.of("status", "SUBMITTED", "semester_id", 1L, "class_name", "2022级2班", "review_phase", "IN_PROGRESS")),
                new HashMap<>(Map.of("status", "COUNSELOR_REVIEWED", "semester_id", 1L, "class_name", "2022级2班", "review_phase", "READY_TO_SUBMIT")),
                new HashMap<>(Map.of("status", "SUBMITTED", "semester_id", 2L, "class_name", "2022级1班", "review_phase", "REVIEWED"))
        );
        when(submissionMapper.listSubmittedTasks(eq(counselorId))).thenReturn(tasks);

        List<Map<String, Object>> out = reviewService.listClassOverview(counselorId);

        assertEquals(2, out.size());

        Map<String, Object> c1 = out.get(0);
        assertEquals("2022级1班", c1.get("className"));
        assertEquals(30L, c1.get("studentTotal"));
        assertEquals(2L, c1.get("notSubmittedCount"));
        assertEquals(1L, c1.get("unreviewedCount"));
        assertEquals(0L, c1.get("inProgressCount"));
        assertEquals(0L, c1.get("reviewedCount"));
        assertEquals(1L, c1.get("readyToSubmitCount"));
        assertEquals(1L, c1.get("submittedToAdminCount"));

        Map<String, Object> c2 = out.get(1);
        assertEquals("2022级2班", c2.get("className"));
        assertEquals(25L, c2.get("studentTotal"));
        assertEquals(0L, c2.get("notSubmittedCount"));
        assertEquals(0L, c2.get("unreviewedCount"));
        assertEquals(1L, c2.get("inProgressCount"));
        assertEquals(0L, c2.get("reviewedCount"));
        assertEquals(0L, c2.get("readyToSubmitCount"));
        assertEquals(0L, c2.get("submittedToAdminCount"));

        verify(userMapper).listCounselorClassOverviewBase(eq(counselorId), eq(1L));
        verify(submissionMapper).listSubmittedTasks(eq(counselorId));
    }
}

