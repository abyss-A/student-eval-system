package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.ActivityItemEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ActivityItemMapper {

    @Select("select * from activity_item where submission_id = #{submissionId} order by id asc")
    List<ActivityItemEntity> listBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("select * from activity_item where submission_id = #{submissionId} and module_type = #{moduleType} order by id asc")
    List<ActivityItemEntity> listBySubmissionIdAndModule(@Param("submissionId") Long submissionId,
                                                         @Param("moduleType") String moduleType);

    @Delete("delete from activity_item where submission_id = #{submissionId}")
    int deleteBySubmissionId(@Param("submissionId") Long submissionId);

    @Delete("delete from activity_item where submission_id = #{submissionId} and module_type = #{moduleType}")
    int deleteBySubmissionIdAndModule(@Param("submissionId") Long submissionId,
                                      @Param("moduleType") String moduleType);

    @Insert("insert into activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment, created_at, updated_at) " +
            "values(#{submissionId}, #{moduleType}, #{title}, #{description}, #{selfScore}, #{finalScore}, #{evidenceFileIds}, #{reviewStatus}, #{reviewerComment}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ActivityItemEntity entity);

    @Select("select * from activity_item where id = #{id} limit 1")
    ActivityItemEntity findById(@Param("id") Long id);

    @Update("update activity_item set review_status=#{reviewStatus}, final_score=#{finalScore}, reviewer_comment=#{reviewerComment}, updated_at=now() where id=#{id}")
    int updateReview(ActivityItemEntity entity);

    @Update("update activity_item set review_status='APPROVED', final_score=self_score, reviewer_comment=#{reason}, updated_at=now() " +
            "where id=#{id} and review_status='PENDING'")
    int approveIfPending(@Param("id") Long id, @Param("reason") String reason);

    @Update("update activity_item set review_status='REJECTED', final_score=0, reviewer_comment=#{reason}, updated_at=now() " +
            "where id=#{id} and review_status='PENDING'")
    int rejectIfPending(@Param("id") Long id, @Param("reason") String reason);

    @Update("update activity_item set review_status='PENDING', final_score=self_score, reviewer_comment=null, updated_at=now() " +
            "where id=#{id} and review_status in ('APPROVED','REJECTED')")
    int undoIfReviewed(@Param("id") Long id);

    @Update("update activity_item set delete_state='DELETE_REQUESTED', updated_at=now() " +
            "where id=#{id} and review_status='REJECTED' and coalesce(delete_state,'NONE')='NONE'")
    int requestDeleteIfRejected(@Param("id") Long id);

    @Update("update activity_item set delete_state='DELETED', updated_at=now() " +
            "where id=#{id} and review_status='REJECTED' and delete_state='DELETE_REQUESTED'")
    int approveDeleteIfRequested(@Param("id") Long id);

    @Update("update activity_item set delete_state='NONE', updated_at=now() " +
            "where id=#{id} and review_status='REJECTED' and delete_state='DELETE_REQUESTED'")
    int rejectDeleteIfRequested(@Param("id") Long id);

    @Update("update activity_item set delete_state='NONE', review_status='REJECTED', updated_at=now() " +
            "where id=#{id} and delete_state='DELETED'")
    int undoDeletedToRejected(@Param("id") Long id);

    @Select("select count(1) from activity_item where submission_id = #{submissionId}")
    int countBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("select count(1) from activity_item where submission_id = #{submissionId} and review_status <> 'PENDING'")
    int countReviewedBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("select count(1) from activity_item where submission_id = #{submissionId} and review_status = #{reviewStatus}")
    int countBySubmissionIdAndStatus(@Param("submissionId") Long submissionId, @Param("reviewStatus") String reviewStatus);

    @Update("update activity_item set title=#{title}, description=#{description}, self_score=#{selfScore}, evidence_file_ids=#{evidenceFileIds}, final_score=#{finalScore}, updated_at=now() where id=#{id} and submission_id=#{submissionId}")
    int updateEditableFields(ActivityItemEntity entity);

    @Update("update activity_item set review_status='PENDING', final_score=self_score, reviewer_comment=null, updated_at=now() " +
            "where submission_id=#{submissionId} and review_status='REJECTED' and coalesce(delete_state,'NONE')='NONE'")
    int reopenRejectedBySubmissionId(@Param("submissionId") Long submissionId);

    @Update("update activity_item set review_status='PENDING', final_score=self_score, reviewer_comment=null, updated_at=now() " +
            "where submission_id=#{submissionId} and coalesce(delete_state,'NONE')='NONE'")
    int resetReviewBySubmissionId(@Param("submissionId") Long submissionId);
}
