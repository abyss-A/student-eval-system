package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.ScoringConfigEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ScoringConfigMapper {

    @Select("select * from scoring_config where semester_id = #{semesterId} limit 1")
    ScoringConfigEntity findBySemesterId(@Param("semesterId") Long semesterId);

    @Insert("insert into scoring_config(semester_id) values(#{semesterId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDefault(@Param("semesterId") Long semesterId);

    @Insert("insert into scoring_config(" +
            "semester_id, appeal_days, precedence_mode, score_model, " +
            "w_moral, w_intel, w_sport, w_art, w_labor, " +
            "cap_moral, cap_intel, cap_sport, cap_art, cap_labor" +
            ") values (" +
            "#{semesterId}, #{appealDays}, #{precedenceMode}, #{scoreModel}, " +
            "#{wMoral}, #{wIntel}, #{wSport}, #{wArt}, #{wLabor}, " +
            "#{capMoral}, #{capIntel}, #{capSport}, #{capArt}, #{capLabor}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScoringConfigEntity entity);
}
