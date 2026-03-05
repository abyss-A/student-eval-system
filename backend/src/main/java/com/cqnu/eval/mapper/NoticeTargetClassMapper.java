package com.cqnu.eval.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoticeTargetClassMapper {

    @Delete("delete from notice_target_class where notice_id = #{noticeId}")
    int deleteByNoticeId(@Param("noticeId") Long noticeId);

    @Insert("insert into notice_target_class(notice_id, class_name, created_at) values(#{noticeId}, #{className}, now())")
    int insert(@Param("noticeId") Long noticeId, @Param("className") String className);

    @Select("select count(1) from notice_target_class where notice_id = #{noticeId} and trim(class_name) = trim(#{className})")
    int countByNoticeIdAndClassName(@Param("noticeId") Long noticeId, @Param("className") String className);

    @Select("select class_name from notice_target_class where notice_id = #{noticeId} order by class_name asc")
    List<String> listClassNamesByNoticeId(@Param("noticeId") Long noticeId);
}
