package com.cqnu.eval.model.entity;

public class ScoringConfigEntity {
    private Long id;
    private Long semesterId;
    private Integer appealDays;
    private String precedenceMode;
    private String scoreModel;
    private Double wMoral;
    private Double wIntel;
    private Double intelCourseRatio;
    private Double intelInnovationRatio;
    private Double wSport;
    private Double sportUniversityPeRatio;
    private Double sportActivityRatio;
    private Double wArt;
    private Double wLabor;
    private Double capMoral;
    private Double capIntel;
    private Double capSport;
    private Double capArt;
    private Double capLabor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
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

    public Double getIntelCourseRatio() {
        return intelCourseRatio;
    }

    public void setIntelCourseRatio(Double intelCourseRatio) {
        this.intelCourseRatio = intelCourseRatio;
    }

    public Double getIntelInnovationRatio() {
        return intelInnovationRatio;
    }

    public void setIntelInnovationRatio(Double intelInnovationRatio) {
        this.intelInnovationRatio = intelInnovationRatio;
    }

    public Double getwSport() {
        return wSport;
    }

    public void setwSport(Double wSport) {
        this.wSport = wSport;
    }

    public Double getSportUniversityPeRatio() {
        return sportUniversityPeRatio;
    }

    public void setSportUniversityPeRatio(Double sportUniversityPeRatio) {
        this.sportUniversityPeRatio = sportUniversityPeRatio;
    }

    public Double getSportActivityRatio() {
        return sportActivityRatio;
    }

    public void setSportActivityRatio(Double sportActivityRatio) {
        this.sportActivityRatio = sportActivityRatio;
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
}
