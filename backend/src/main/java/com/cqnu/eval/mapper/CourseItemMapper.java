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

    @Update("update course_item set review_status='APPROVED', reviewer_score=score, reviewer_comment=#{reason}, updated_at=now() " +
            "where id=#{id} and review_status='PENDING'")
    int approveIfPending(@Param("id") Long id, @Param("reason") String reason);

    @Update("update course_item set review_status='REJECTED', reviewer_score=0, reviewer_comment=#{reason}, updated_at=now() " +
            "where id=#{id} and review_status='PENDING'")
    int rejectIfPending(@Param("id") Long id, @Param("reason") String reason);

    @Update("update course_item set review_status='PENDING', reviewer_score=score, reviewer_comment=null, updated_at=now() " +
            "where id=#{id} and review_status in ('APPROVED','REJECTED')")
    int undoIfReviewed(@Param("id") Long id);

    @Update("update course_item set delete_state='DELETE_REQUESTED', updated_at=now() " +
            "where id=#{id} and review_status='REJECTED' and coalesce(delete_state,'NONE')='NONE'")
    int requestDeleteIfRejected(@Param("id") Long id);

    @Update("update course_item set delete_state='DELETED', updated_at=now() " +
            "where id=#{id} and review_status='REJECTED' and delete_state='DELETE_REQUESTED'")
    int approveDeleteIfRequested(@Param("id") Long id);

    @Update("update course_item set delete_state='NONE', updated_at=now() " +
            "where id=#{id} and review_status='REJECTED' and delete_state='DELETE_REQUESTED'")
    int rejectDeleteIfRequested(@Param("id") Long id);

    @Update("update course_item set delete_state='NONE', review_status='REJECTED', updated_at=now() " +
            "where id=#{id} and delete_state='DELETED'")
    int undoDeletedToRejected(@Param("id") Long id);

    @Select("select count(1) from course_item where submission_id = #{submissionId}")
    int countBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("select count(1) from course_item where submission_id = #{submissionId} and review_status <> 'PENDING'")
    int countReviewedBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("select count(1) from course_item where submission_id = #{submissionId} and review_status = #{reviewStatus}")
    int countBySubmissionIdAndStatus(@Param("submissionId") Long submissionId, @Param("reviewStatus") String reviewStatus);

    @Update("update course_item set course_name=#{courseName}, course_type=#{courseType}, score=#{score}, credit=#{credit}, evidence_file_id=#{evidenceFileId}, reviewer_score=#{reviewerScore}, updated_at=now() where id=#{id} and submission_id=#{submissionId}")
    int updateEditableFields(CourseItemEntity entity);

    @Update("update course_item set review_status='PENDING', reviewer_score=score, reviewer_comment=null, updated_at=now() " +
            "where submission_id=#{submissionId} and review_status='REJECTED' and coalesce(delete_state,'NONE')='NONE'")
    int reopenRejectedBySubmissionId(@Param("submissionId") Long submissionId);

    @Update("update course_item set review_status='PENDING', reviewer_score=score, reviewer_comment=null, updated_at=now() " +
            "where submission_id=#{submissionId} and coalesce(delete_state,'NONE')='NONE'")
    int resetReviewBySubmissionId(@Param("submissionId") Long submissionId);
}
