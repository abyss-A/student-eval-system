package com.cqnu.eval.controller;

import com.cqnu.eval.common.ApiResponse;
import com.cqnu.eval.mapper.SemesterMapper;
import com.cqnu.eval.model.entity.SemesterEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/semesters")
public class SemesterController {

    private final SemesterMapper semesterMapper;

    public SemesterController(SemesterMapper semesterMapper) {
        this.semesterMapper = semesterMapper;
    }

    @GetMapping
    public ApiResponse<List<SemesterEntity>> list() {
        return ApiResponse.ok(semesterMapper.listAll());
    }

    @GetMapping("/active")
    public ApiResponse<SemesterEntity> active() {
        return ApiResponse.ok(semesterMapper.findActive());
    }

    @GetMapping("/current")
    public ApiResponse<SemesterEntity> current() {
        return active();
    }
}
