package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.SemesterEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SemesterMapper {

    @Select("select id, name, year_num as year, term_num as term, is_active, created_at from semester where is_active = 1 order by id desc limit 1")
    SemesterEntity findActive();

    @Select("select id, name, year_num as year, term_num as term, is_active, created_at from semester order by id desc")
    List<SemesterEntity> listAll();

    @Select("select id, name, year_num as year, term_num as term, is_active, created_at from semester where id = #{id} limit 1")
    SemesterEntity findById(@Param("id") Long id);

    @Insert("insert into semester(name, year_num, term_num, is_active) values(#{name}, #{year}, #{term}, #{isActive})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SemesterEntity entity);

    @Update("update semester set is_active = 0 where is_active = 1")
    int deactivateAll();

    @Update("update semester set is_active = 1 where id = #{id}")
    int activateById(@Param("id") Long id);

    @Update("update semester set name = #{name} where id = #{id}")
    int updateName(@Param("id") Long id, @Param("name") String name);

    @Delete("delete from semester where id = #{id}")
    int deleteById(@Param("id") Long id);
}
