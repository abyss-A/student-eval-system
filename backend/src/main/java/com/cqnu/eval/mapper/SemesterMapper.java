package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.SemesterEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SemesterMapper {

    @Select("select id, name, year_num as year, term_num as term, is_active, created_at from semester where is_active = 1 order by id desc limit 1")
    SemesterEntity findActive();
}