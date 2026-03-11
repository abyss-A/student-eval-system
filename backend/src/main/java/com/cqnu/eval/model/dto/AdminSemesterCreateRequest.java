package com.cqnu.eval.model.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class AdminSemesterCreateRequest {

    @NotNull(message = "年份不能为空")
    @Min(value = 2000, message = "年份不合法")
    @Max(value = 2100, message = "年份不合法")
    private Integer year;

    @NotBlank(message = "学期季节不能为空")
    @Pattern(regexp = "^(SPRING|AUTUMN)$", message = "学期季节不合法")
    private String season;

    @NotBlank(message = "学期名称不能为空")
    @Size(max = 64, message = "学期名称不能超过64位")
    private String name;

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

