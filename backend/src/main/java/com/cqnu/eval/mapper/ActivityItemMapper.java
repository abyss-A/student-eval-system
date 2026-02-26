package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.ActivityItemEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ActivityItemMapper {

    @Select("select * from activity_item where submission_id = #{submissionId} order by id asc")
    List<ActivityItemEntity> listBySubmissionId(@Param("submissionId") Long submissionId);

    @Delete("delete from activity_item where submission_id = #{submissionId}")
    int deleteBySubmissionId(@Param("submissionId") Long submissionId);

    @Insert("insert into activity_item(submission_id, module_type, title, description, self_score, final_score, evidence_file_ids, review_status, reviewer_comment, created_at, updated_at) " +
            "values(#{submissionId}, #{moduleType}, #{title}, #{description}, #{selfScore}, #{finalScore}, #{evidenceFileIds}, #{reviewStatus}, #{reviewerComment}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ActivityItemEntity entity);

    @Select("select * from activity_item where id = #{id} limit 1")
    ActivityItemEntity findById(@Param("id") Long id);

    @Update("update activity_item set review_status=#{reviewStatus}, final_score=#{finalScore}, reviewer_comment=#{reviewerComment}, updated_at=now() where id=#{id}")
    int updateReview(ActivityItemEntity entity);
}
