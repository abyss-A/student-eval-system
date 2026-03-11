package com.cqnu.eval.model.dto;

import javax.validation.constraints.NotNull;

public class AdminScoringConfigUpdateRequest {

    @NotNull(message = "德育权重不能为空")
    private Double wMoral;

    @NotNull(message = "智育权重不能为空")
    private Double wIntel;

    @NotNull(message = "体育权重不能为空")
    private Double wSport;

    @NotNull(message = "美育权重不能为空")
    private Double wArt;

    @NotNull(message = "劳育权重不能为空")
    private Double wLabor;

    @NotNull(message = "德育上限不能为空")
    private Double capMoral;

    @NotNull(message = "智育上限不能为空")
    private Double capIntel;

    @NotNull(message = "体育上限不能为空")
    private Double capSport;

    @NotNull(message = "美育上限不能为空")
    private Double capArt;

    @NotNull(message = "劳育上限不能为空")
    private Double capLabor;

    private Integer appealDays;
    private String precedenceMode;
    private String scoreModel;

    public Double getwMoral() {
        return wMoral;
    }

    public void setwMoral(Double wMoral) {
        this.wMoral = wMoral;
    }

    public Double getwIntel() {
        return wIntel;
    }

    public void setwIntel(Double wIntel) {
        this.wIntel = wIntel;
    }

    public Double getwSport() {
        return wSport;
    }

    public void setwSport(Double wSport) {
        this.wSport = wSport;
    }

    public Double getwArt() {
        return wArt;
    }

    public void setwArt(Double wArt) {
        this.wArt = wArt;
    }

    public Double getwLabor() {
        return wLabor;
    }

    public void setwLabor(Double wLabor) {
        this.wLabor = wLabor;
    }

    public Double getCapMoral() {
        return capMoral;
    }

    public void setCapMoral(Double capMoral) {
        this.capMoral = capMoral;
    }

    public Double getCapIntel() {
        return capIntel;
    }

    public void setCapIntel(Double capIntel) {
        this.capIntel = capIntel;
    }

    public Double getCapSport() {
        return capSport;
    }

    public void setCapSport(Double capSport) {
        this.capSport = capSport;
    }

    public Double getCapArt() {
        return capArt;
    }

    public void setCapArt(Double capArt) {
        this.capArt = capArt;
    }

    public Double getCapLabor() {
        return capLabor;
    }

    public void setCapLabor(Double capLabor) {
        this.capLabor = capLabor;
    }

    public Integer getAppealDays() {
        return appealDays;
    }

    public void setAppealDays(Integer appealDays) {
        this.appealDays = appealDays;
    }

    public String getPrecedenceMode() {
        return precedenceMode;
    }

    public void setPrecedenceMode(String precedenceMode) {
        this.precedenceMode = precedenceMode;
    }

    public String getScoreModel() {
        return scoreModel;
    }

    public void setScoreModel(String scoreModel) {
        this.scoreModel = scoreModel;
    }
}

