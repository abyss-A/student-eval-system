package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.CourseItemEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseItemMapper {

    @Select("select * from course_item where submission_id = #{submissionId} order by id asc")
    List<CourseItemEntity> listBySubmissionId(@Param("submissionId") Long submissionId);

    @Delete("delete from course_item where submission_id = #{submissionId}")
    int deleteBySubmissionId(@Param("submissionId") Long submissionId);

    @Insert("insert into course_item(submission_id, course_name, course_type, score, credit, evidence_file_id, review_status, reviewer_score, reviewer_comment, created_at, updated_at) " +
            "values(#{submissionId}, #{courseName}, #{courseType}, #{score}, #{credit}, #{evidenceFileId}, #{reviewStatus}, #{reviewerScore}, #{reviewerComment}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseItemEntity entity);

    @Select("select * from course_item where id = #{id} limit 1")
    CourseItemEntity findById(@Param("id") Long id);

    @Update("update course_item set review_status=#{reviewStatus}, reviewer_score=#{reviewerScore}, reviewer_comment=#{reviewerComment}, updated_at=now() where id=#{id}")
    int updateReview(CourseItemEntity entity);
}
