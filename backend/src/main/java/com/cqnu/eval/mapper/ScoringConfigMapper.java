package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.ScoringConfigEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ScoringConfigMapper {

    @Select("select * from scoring_config where semester_id = #{semesterId} limit 1")
    ScoringConfigEntity findBySemesterId(@Param("semesterId") Long semesterId);

    @Insert("insert into scoring_config(semester_id) values(#{semesterId})")
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

    @Update("update scoring_config set " +
            "appeal_days=#{appealDays}, precedence_mode=#{precedenceMode}, score_model=#{scoreModel}, " +
            "w_moral=#{wMoral}, w_intel=#{wIntel}, w_sport=#{wSport}, w_art=#{wArt}, w_labor=#{wLabor}, " +
            "cap_moral=#{capMoral}, cap_intel=#{capIntel}, cap_sport=#{capSport}, cap_art=#{capArt}, cap_labor=#{capLabor} " +
            "where semester_id=#{semesterId}")
    int update(ScoringConfigEntity entity);

    @Delete("delete from scoring_config where semester_id = #{semesterId}")
    int deleteBySemesterId(@Param("semesterId") Long semesterId);
}
