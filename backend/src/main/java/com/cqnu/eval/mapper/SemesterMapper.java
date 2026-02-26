package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.SemesterEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SemesterMapper {

    @Select("select * from semester where is_active = 1 order by id desc limit 1")
    SemesterEntity findActive();
}
