package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotNull;
import java.util.List;

public class CounselorScopeUpdateRequest {

    @NotNull(message = "classNames cannot be null")
    private List<String> classNames;

    public List<String> getClassNames() {
        return classNames;
    }

    public void setClassNames(List<String> classNames) {
        this.classNames = classNames;
    }
}
