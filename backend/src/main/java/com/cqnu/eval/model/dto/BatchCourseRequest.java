package com.cqnu.eval.model.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class BatchCourseRequest {

    @Valid
    @NotNull(message = "\u8BFE\u7A0B\u5217\u8868\u4E0D\u80FD\u4E3A\u7A7A")
    private List<CourseItemInput> items;

    public List<CourseItemInput> getItems() {
        return items;
    }

    public void setItems(List<CourseItemInput> items) {
        this.items = items;
    }
}
