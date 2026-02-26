package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.ScoringConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ScoringConfigMapper {

    @Select("select * from scoring_config where semester_id = #{semesterId} limit 1")
    ScoringConfigEntity findBySemesterId(@Param("semesterId") Long semesterId);
}
