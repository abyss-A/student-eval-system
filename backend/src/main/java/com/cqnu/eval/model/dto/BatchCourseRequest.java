package com.cqnu.eval.model.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class BatchCourseRequest {

    @Valid
    @NotEmpty(message = "课程列表不能为空")
    private List<CourseItemInput> items;

    public List<CourseItemInput> getItems() {
        return items;
    }

    public void setItems(List<CourseItemInput> items) {
        this.items = items;
    }
}
