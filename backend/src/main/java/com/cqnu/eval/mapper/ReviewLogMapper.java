package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.ReviewLogEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface ReviewLogMapper {

    @Insert("insert into review_log(item_type, item_id, submission_id, action, score_before, score_after, reason, reviewer_id, created_at) " +
            "values(#{itemType}, #{itemId}, #{submissionId}, #{action}, #{scoreBefore}, #{scoreAfter}, #{reason}, #{reviewerId}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReviewLogEntity entity);
}
