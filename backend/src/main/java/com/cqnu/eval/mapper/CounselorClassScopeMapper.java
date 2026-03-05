package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.CounselorClassScopeEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CounselorClassScopeMapper {

    @Select("select * from counselor_class_scope where counselor_id = #{counselorId} order by class_name asc, id asc")
    List<CounselorClassScopeEntity> listByCounselorId(@Param("counselorId") Long counselorId);

    @Select("select class_name from counselor_class_scope where counselor_id = #{counselorId} order by class_name asc")
    List<String> listClassNamesByCounselorId(@Param("counselorId") Long counselorId);

    @Delete("delete from counselor_class_scope where counselor_id = #{counselorId}")
    int deleteByCounselorId(@Param("counselorId") Long counselorId);

    @Insert("insert into counselor_class_scope(counselor_id, class_name, assigned_by, created_at, updated_at) " +
            "values(#{counselorId}, #{className}, #{assignedBy}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CounselorClassScopeEntity entity);

    @Select("select count(1) from counselor_class_scope where trim(class_name) = trim(#{className})")
    int countByClassName(@Param("className") String className);

    @Select("select count(1) from counselor_class_scope where counselor_id = #{counselorId}")
    int countByCounselorId(@Param("counselorId") Long counselorId);

    @Select("select count(1) from counselor_class_scope where counselor_id = #{counselorId} and trim(class_name) = trim(#{className})")
    int countByCounselorIdAndClassName(@Param("counselorId") Long counselorId,
                                       @Param("className") String className);

    @Select({
            "<script>",
            "select s.class_name as className, s.counselor_id as counselorId, u.real_name as counselorName, u.account_no as counselorAccountNo",
            "from counselor_class_scope s",
            "join sys_user u on u.id = s.counselor_id",
            "where s.class_name in",
            "<foreach collection='classNames' item='className' open='(' separator=',' close=')'>",
            "  #{className}",
            "</foreach>",
            "and s.counselor_id &lt;&gt; #{excludeCounselorId}",
            "order by s.class_name asc",
            "</script>"
    })
    List<Map<String, Object>> listConflicts(@Param("classNames") List<String> classNames,
                                            @Param("excludeCounselorId") Long excludeCounselorId);
}
