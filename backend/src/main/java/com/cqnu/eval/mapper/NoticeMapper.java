package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.NoticeEntity;
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
public interface NoticeMapper {

    @Insert("insert into notice(title, content, status, audience_scope, publisher_id, created_at, updated_at) " +
            "values(#{title}, #{content}, #{status}, #{audienceScope}, #{publisherId}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NoticeEntity entity);

    @Select("select * from notice where id = #{id} limit 1")
    NoticeEntity findById(@Param("id") Long id);

    @Select("select n.*, u.real_name as publisher_real_name, u.role as publisher_role " +
            "from notice n join sys_user u on n.publisher_id=u.id where n.id = #{id} limit 1")
    Map<String, Object> findDetailById(@Param("id") Long id);

    @Update("update notice set title=#{title}, content=#{content}, updated_at=now() where id=#{id}")
    int updateContent(NoticeEntity entity);

    @Update("update notice set status='PUBLISHED', audience_scope=#{audienceScope}, published_at=now(), offline_at=null, updated_at=now() where id=#{id}")
    int publish(@Param("id") Long id, @Param("audienceScope") String audienceScope);

    @Update("update notice set status='OFFLINE', offline_at=now(), updated_at=now() where id=#{id}")
    int offline(@Param("id") Long id);

    @Delete("delete from notice where id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select({
            "<script>",
            "select n.*, u.real_name as publisher_real_name, u.role as publisher_role",
            "from notice n join sys_user u on n.publisher_id=u.id",
            "<where>",
            " 1=1",
            " <if test='status != null and status != \"\"'> and n.status = #{status} </if>",
            " <if test='keyword != null and keyword != \"\"'> and n.title like concat('%', #{keyword}, '%') </if>",
            "</where>",
            "order by n.updated_at desc, n.id desc",
            "</script>"
    })
    List<Map<String, Object>> list(@Param("status") String status, @Param("keyword") String keyword);

    @Select({
            "<script>",
            "select n.*, u.real_name as publisher_real_name, u.role as publisher_role",
            "from notice n join sys_user u on n.publisher_id=u.id",
            "<where>",
            " (n.publisher_id = #{counselorId} or u.role = 'ADMIN')",
            " <if test='status != null and status != \"\"'> and n.status = #{status} </if>",
            " <if test='keyword != null and keyword != \"\"'> and n.title like concat('%', #{keyword}, '%') </if>",
            "</where>",
            "order by n.updated_at desc, n.id desc",
            "</script>"
    })
    List<Map<String, Object>> listForCounselor(@Param("counselorId") Long counselorId,
                                                @Param("status") String status,
                                                @Param("keyword") String keyword);

    @Select({
            "<script>",
            "select n.*, u.real_name as publisher_real_name, u.role as publisher_role",
            "from notice n join sys_user u on n.publisher_id=u.id",
            "<where>",
            " n.status='PUBLISHED'",
            " and (",
            "   n.audience_scope = 'ALL_COLLEGE'",
            "   <if test='className != null and className != \"\"'>",
            "     or exists (",
            "       select 1 from notice_target_class ntc",
            "       where ntc.notice_id = n.id and trim(ntc.class_name) = trim(#{className})",
            "     )",
            "   </if>",
            " )",
            "</where>",
            "order by n.published_at desc, n.id desc",
            "</script>"
    })
    List<Map<String, Object>> listPublishedForStudent(@Param("className") String className);
}
