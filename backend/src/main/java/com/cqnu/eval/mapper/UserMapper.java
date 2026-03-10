package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.UserEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("select * from sys_user where id = #{id} limit 1")
    UserEntity findById(@Param("id") Long id);

    @Select("select * from sys_user where account_no = #{accountNo} and enabled = 1 limit 1")
    UserEntity findByAccountNo(@Param("accountNo") String accountNo);

    @Select("select * from sys_user where account_no = #{accountNo} and enabled = 1 limit 1")
    UserEntity findEnabledByAccountNo(@Param("accountNo") String accountNo);

    @Select("select * from sys_user where account_no = #{accountNo} limit 1")
    UserEntity findAnyByAccountNo(@Param("accountNo") String accountNo);

    @Insert("insert into sys_user(password_hash, role, account_no, real_name, gender, phone, class_name, enabled) " +
            "values(#{passwordHash}, #{role}, #{accountNo}, #{realName}, #{gender}, #{phone}, #{className}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity entity);

    @Update("update sys_user set phone = #{phone} where id = #{id}")
    int updatePhoneById(@Param("id") Long id, @Param("phone") String phone);

    @Update("update sys_user set password_hash = #{passwordHash} where id = #{id}")
    int updatePasswordById(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Update("update sys_user set real_name = #{realName}, gender = #{gender}, phone = #{phone}, class_name = #{className}, enabled = #{enabled} where id = #{id}")
    int updateManagedFields(UserEntity entity);

    @Update("update sys_user set enabled = #{enabled} where id = #{id}")
    int updateEnabledById(@Param("id") Long id, @Param("enabled") Integer enabled);

    @Delete("delete from sys_user where id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select({
            "<script>",
            "select u.id, u.account_no as accountNo, u.real_name as realName, coalesce(sc.scope_count, 0) as scopeCount",
            "from sys_user u",
            "left join (",
            "  select counselor_id, count(1) as scope_count",
            "  from counselor_class_scope",
            "  group by counselor_id",
            ") sc on sc.counselor_id = u.id",
            "where u.role = 'COUNSELOR' and u.enabled = 1",
            "<if test='keyword != null and keyword != \"\"'>",
            "  and (u.account_no like concat('%', #{keyword}, '%') or u.real_name like concat('%', #{keyword}, '%'))",
            "</if>",
            "order by u.id asc",
            "</script>"
    })
    List<Map<String, Object>> listCounselors(@Param("keyword") String keyword);

    @Select({
            "<script>",
            "select u.id, u.role as role, u.account_no as accountNo, u.real_name as realName, u.gender as gender,",
            "u.phone as phone, u.class_name as className, u.enabled as enabled, u.created_at as createdAt",
            "from sys_user u",
            "where u.role in ('STUDENT', 'COUNSELOR')",
            "<if test='role != null and role != \"\"'>",
            "  and u.role = #{role}",
            "</if>",
            "<if test='enabled != null'>",
            "  and u.enabled = #{enabled}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "  and (u.account_no like concat('%', #{keyword}, '%') or u.real_name like concat('%', #{keyword}, '%') or ifnull(u.class_name, '') like concat('%', #{keyword}, '%'))",
            "</if>",
            "order by u.id desc",
            "</script>"
    })
    List<Map<String, Object>> listManagedAccounts(@Param("role") String role,
                                                  @Param("enabled") Integer enabled,
                                                  @Param("keyword") String keyword);

    @Select("select count(1) from submission where student_id = #{userId}")
    long countSubmissionRefs(@Param("userId") Long userId);

    @Select("select count(1) from attachment where uploader_id = #{userId}")
    long countAttachmentUploadRefs(@Param("userId") Long userId);

    @Select("select count(1) from notice where publisher_id = #{userId}")
    long countNoticePublisherRefs(@Param("userId") Long userId);

    @Select("select count(1) from counselor_class_scope where counselor_id = #{userId}")
    long countCounselorScopeRefs(@Param("userId") Long userId);

    @Select("select count(1) from counselor_class_scope where assigned_by = #{userId}")
    long countScopeAssignmentRefs(@Param("userId") Long userId);

    @Select("select count(1) from feedback where creator_id = #{userId}")
    long countFeedbackCreatorRefs(@Param("userId") Long userId);

    @Select("select count(1) from feedback where handler_id = #{userId}")
    long countFeedbackHandlerRefs(@Param("userId") Long userId);

    @Select("select distinct trim(class_name) as class_name from sys_user " +
            "where role='STUDENT' and enabled=1 and class_name is not null and trim(class_name) <> '' " +
            "order by trim(class_name) asc")
    List<String> listDistinctStudentClasses();
}
