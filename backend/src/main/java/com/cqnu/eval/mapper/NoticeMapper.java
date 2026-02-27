package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.NoticeEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface NoticeMapper {

    @Insert("insert into notice(title, content, status, publisher_id, created_at, updated_at) " +
            "values(#{title}, #{content}, #{status}, #{publisherId}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NoticeEntity entity);

    @Select("select * from notice where id = #{id} limit 1")
    NoticeEntity findById(@Param("id") Long id);

    @Select("select n.*, u.real_name as publisher_real_name from notice n join sys_user u on n.publisher_id=u.id where n.id = #{id} limit 1")
    Map<String, Object> findDetailById(@Param("id") Long id);

    @Update("update notice set title=#{title}, content=#{content}, updated_at=now() where id=#{id}")
    int updateContent(NoticeEntity entity);

    @Update("update notice set status='PUBLISHED', published_at=now(), offline_at=null, updated_at=now() where id=#{id}")
    int publish(@Param("id") Long id);

    @Update("update notice set status='OFFLINE', offline_at=now(), updated_at=now() where id=#{id}")
    int offline(@Param("id") Long id);

    @Delete("delete from notice where id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select({
            "<script>",
            "select n.*, u.real_name as publisher_real_name",
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

    @Select("select n.*, u.real_name as publisher_real_name " +
            "from notice n join sys_user u on n.publisher_id=u.id " +
            "where n.status='PUBLISHED' order by n.published_at desc, n.id desc")
    List<Map<String, Object>> listPublished();
}

