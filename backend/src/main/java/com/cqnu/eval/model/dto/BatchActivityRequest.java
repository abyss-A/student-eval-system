package com.cqnu.eval.model.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public class BatchActivityRequest {

    @Valid
    @NotEmpty(message = "活动列表不能为空")
    private List<ActivityItemInput> items;

    public List<ActivityItemInput> getItems() {
        return items;
    }

    public void setItems(List<ActivityItemInput> items) {
        this.items = items;
    }
}
