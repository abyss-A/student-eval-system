package com.cqnu.eval.model.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class BatchActivityRequest {

    @Valid
    @NotNull(message = "\u6D3B\u52A8\u5217\u8868\u4E0D\u80FD\u4E3A\u7A7A")
    private List<ActivityItemInput> items;

    public List<ActivityItemInput> getItems() {
        return items;
    }

    public void setItems(List<ActivityItemInput> items) {
        this.items = items;
    }
}
