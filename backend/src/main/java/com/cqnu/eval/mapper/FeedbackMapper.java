package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.FeedbackEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface FeedbackMapper {

    @Insert("insert into feedback(creator_id, title, content, screenshot_file_ids, status, created_at, updated_at) " +
            "values(#{creatorId}, #{title}, #{content}, #{screenshotFileIds}, #{status}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FeedbackEntity entity);

    @Select("select * from feedback where id = #{id} limit 1")
    FeedbackEntity findById(@Param("id") Long id);

    @Select("select f.*, c.real_name as creator_real_name, c.role as creator_role, " +
            "c.student_no, c.class_name, c.major_name, h.real_name as handler_real_name " +
            "from feedback f join sys_user c on f.creator_id=c.id " +
            "left join sys_user h on f.handler_id=h.id " +
            "where f.id = #{id} limit 1")
    Map<String, Object> findDetailById(@Param("id") Long id);

    @Select({
            "<script>",
            "select * from feedback",
            "where creator_id = #{creatorId}",
            " <if test='status != null and status != \"\"'> and status = #{status} </if>",
            "order by created_at desc, id desc",
            "</script>"
    })
    List<Map<String, Object>> listMy(@Param("creatorId") Long creatorId, @Param("status") String status);

    @Select({
            "<script>",
            "select f.*, c.real_name as creator_real_name, c.student_no, c.class_name, c.major_name",
            "from feedback f join sys_user c on f.creator_id=c.id",
            "<where>",
            " 1=1",
            " <if test='status != null and status != \"\"'> and f.status = #{status} </if>",
            " <if test='keyword != null and keyword != \"\"'>",
            "   and (f.title like concat('%', #{keyword}, '%') or f.content like concat('%', #{keyword}, '%'))",
            " </if>",
            "</where>",
            "order by f.created_at desc, f.id desc",
            "</script>"
    })
    List<Map<String, Object>> listAll(@Param("status") String status, @Param("keyword") String keyword);

    @Update("update feedback set status='REPLIED', handler_id=#{handlerId}, reply_content=#{replyContent}, replied_at=now(), updated_at=now() where id=#{id}")
    int handleReply(@Param("id") Long id, @Param("handlerId") Long handlerId, @Param("replyContent") String replyContent);

    @Update({
            "<script>",
            "update feedback",
            "<set>",
            " status='CLOSED',",
            " handler_id=#{handlerId},",
            " closed_at=now(),",
            " <if test='replyContent != null and replyContent != \"\"'>",
            " reply_content=#{replyContent},",
            " replied_at=IFNULL(replied_at, now()),",
            " </if>",
            " updated_at=now()",
            "</set>",
            "where id=#{id}",
            "</script>"
    })
    int handleClose(@Param("id") Long id, @Param("handlerId") Long handlerId, @Param("replyContent") String replyContent);
}

