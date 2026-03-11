package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.SubmissionEntity;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SubmissionMapper {

    @Select("select * from submission where semester_id = #{semesterId} and student_id = #{studentId} limit 1")
    SubmissionEntity findBySemesterAndStudent(@Param("semesterId") Long semesterId, @Param("studentId") Long studentId);

    @Select("select * from submission where id = #{id} limit 1")
    SubmissionEntity findById(@Param("id") Long id);

    @Insert("insert into submission(semester_id, student_id, status, moral_raw, intel_raw, sport_raw, art_raw, labor_raw, total_score, created_at, updated_at) " +
            "values(#{semesterId}, #{studentId}, #{status}, #{moralRaw}, #{intelRaw}, #{sportRaw}, #{artRaw}, #{laborRaw}, #{totalScore}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SubmissionEntity entity);

    @Update("update submission set status=#{status}, moral_raw=#{moralRaw}, intel_raw=#{intelRaw}, sport_raw=#{sportRaw}, art_raw=#{artRaw}, labor_raw=#{laborRaw}, total_score=#{totalScore}, submitted_at=#{submittedAt}, finalized_at=#{finalizedAt}, published_at=#{publishedAt}, updated_at=now() where id=#{id}")
    int updateScoresAndStatus(SubmissionEntity entity);

    @Select("select s.*, u.account_no, u.real_name, u.class_name from submission s join sys_user u on s.student_id=u.id where s.semester_id=#{semesterId} and s.status in ('SUBMITTED','COUNSELOR_REVIEWED','FINALIZED','PUBLISHED') order by s.total_score desc")
    List<java.util.Map<String, Object>> listForRanking(@Param("semesterId") Long semesterId);

    @Select("select count(1) from submission where semester_id = #{semesterId} and status = 'SUBMITTED'")
    long countSubmittedBySemester(@Param("semesterId") Long semesterId);

    @Select("select count(1) from submission where semester_id = #{semesterId}")
    long countBySemester(@Param("semesterId") Long semesterId);

    @Select("select id from submission where semester_id = #{semesterId} and status in ('SUBMITTED','COUNSELOR_REVIEWED','FINALIZED','PUBLISHED') order by id asc")
    List<Long> listIdsForRecalculate(@Param("semesterId") Long semesterId);

    @Select("select " +
            "s.*, u.account_no, u.real_name, u.class_name, " +
            "(coalesce(ci.total_count,0) + coalesce(ai.total_count,0)) as review_total_count, " +
            "(coalesce(ci.done_count,0) + coalesce(ai.done_count,0)) as review_done_count, " +
            "(coalesce(ci.pending_count,0) + coalesce(ai.pending_count,0)) as review_pending_count, " +
            "(coalesce(ci.rejected_count,0) + coalesce(ai.rejected_count,0)) as review_rejected_count, " +
            "(coalesce(ci.approved_count,0) + coalesce(ai.approved_count,0)) as review_approved_count, " +
            "(coalesce(ci.delete_requested_count,0) + coalesce(ai.delete_requested_count,0)) as review_delete_requested_count, " +
            "case " +
            "  when (coalesce(ci.total_count,0) + coalesce(ai.total_count,0)) = 0 then 'READY_TO_SUBMIT' " +
            "  when (coalesce(ci.done_count,0) + coalesce(ai.done_count,0)) = 0 " +
            "       and (coalesce(ci.delete_requested_count,0) + coalesce(ai.delete_requested_count,0)) = 0 then 'NOT_REVIEWED' " +
            "  when (coalesce(ci.done_count,0) + coalesce(ai.done_count,0)) < (coalesce(ci.total_count,0) + coalesce(ai.total_count,0)) then 'IN_PROGRESS' " +
            "  when (coalesce(ci.rejected_count,0) + coalesce(ai.rejected_count,0)) > 0 then 'REVIEWED' " +
            "  else 'READY_TO_SUBMIT' " +
            "end as review_phase, " +
            "case " +
            "  when (coalesce(ci.total_count,0) + coalesce(ai.total_count,0)) = 0 then 'DONE' " +
            "  when (coalesce(ci.done_count,0) + coalesce(ai.done_count,0)) = 0 " +
            "       and (coalesce(ci.delete_requested_count,0) + coalesce(ai.delete_requested_count,0)) = 0 then 'NOT_REVIEWED' " +
            "  when (coalesce(ci.done_count,0) + coalesce(ai.done_count,0)) < (coalesce(ci.total_count,0) + coalesce(ai.total_count,0)) then 'IN_PROGRESS' " +
            "  else 'DONE' " +
            "end as review_progress " +
            "from submission s " +
            "join sys_user u on s.student_id = u.id " +
            "left join (" +
            "  select submission_id, " +
            "         sum(case when coalesce(delete_state,'NONE') <> 'DELETED' then 1 else 0 end) as total_count, " +
            "         sum(case " +
            "               when coalesce(delete_state,'NONE') in ('DELETED','DELETE_REQUESTED') then 0 " +
            "               when review_status <> 'PENDING' then 1 else 0 end) as done_count, " +
            "         sum(case " +
            "               when coalesce(delete_state,'NONE') = 'DELETED' then 0 " +
            "               when coalesce(delete_state,'NONE') = 'DELETE_REQUESTED' then 1 " +
            "               when review_status = 'PENDING' then 1 else 0 end) as pending_count, " +
            "         sum(case when coalesce(delete_state,'NONE') = 'NONE' and review_status = 'REJECTED' then 1 else 0 end) as rejected_count, " +
            "         sum(case when coalesce(delete_state,'NONE') = 'NONE' and review_status = 'APPROVED' then 1 else 0 end) as approved_count, " +
            "         sum(case when coalesce(delete_state,'NONE') = 'DELETE_REQUESTED' then 1 else 0 end) as delete_requested_count " +
            "  from course_item group by submission_id" +
            ") ci on ci.submission_id = s.id " +
            "left join (" +
            "  select submission_id, " +
            "         sum(case when coalesce(delete_state,'NONE') <> 'DELETED' then 1 else 0 end) as total_count, " +
            "         sum(case " +
            "               when coalesce(delete_state,'NONE') in ('DELETED','DELETE_REQUESTED') then 0 " +
            "               when review_status <> 'PENDING' then 1 else 0 end) as done_count, " +
            "         sum(case " +
            "               when coalesce(delete_state,'NONE') = 'DELETED' then 0 " +
            "               when coalesce(delete_state,'NONE') = 'DELETE_REQUESTED' then 1 " +
            "               when review_status = 'PENDING' then 1 else 0 end) as pending_count, " +
            "         sum(case when coalesce(delete_state,'NONE') = 'NONE' and review_status = 'REJECTED' then 1 else 0 end) as rejected_count, " +
            "         sum(case when coalesce(delete_state,'NONE') = 'NONE' and review_status = 'APPROVED' then 1 else 0 end) as approved_count, " +
            "         sum(case when coalesce(delete_state,'NONE') = 'DELETE_REQUESTED' then 1 else 0 end) as delete_requested_count " +
            "  from activity_item group by submission_id" +
            ") ai on ai.submission_id = s.id " +
            "join counselor_class_scope ccs on trim(ccs.class_name) = trim(u.class_name) and ccs.counselor_id = #{counselorId} " +
            "where s.status in ('SUBMITTED','COUNSELOR_REVIEWED') " +
            "order by case when s.status='SUBMITTED' then 0 else 1 end asc, coalesce(s.submitted_at, s.updated_at) desc")
    List<java.util.Map<String, Object>> listSubmittedTasks(@Param("counselorId") Long counselorId);

    @Select("select s.*, u.account_no, u.real_name, u.class_name, " +
            "coalesce(s.counselor_ready_at, s.updated_at) as passTime " +
            "from submission s " +
            "join sys_user u on s.student_id=u.id " +
            "where s.status='COUNSELOR_REVIEWED' " +
            "order by s.updated_at desc")
    List<java.util.Map<String, Object>> listCounselorReviewedTasks();

    @Select("select trim(u.class_name) from submission s join sys_user u on s.student_id=u.id where s.id=#{submissionId} limit 1")
    String findStudentClassNameBySubmissionId(@Param("submissionId") Long submissionId);

    @Select("select count(1) from submission s " +
            "join sys_user u on s.student_id=u.id " +
            "join counselor_class_scope ccs on trim(ccs.class_name)=trim(u.class_name) and ccs.counselor_id=#{counselorId} " +
            "where s.id=#{submissionId}")
    int countCounselorScopeForSubmission(@Param("submissionId") Long submissionId,
                                         @Param("counselorId") Long counselorId);

    @Select("select count(1) from submission where id=#{id} and student_id=#{studentId}")
    int checkOwner(@Param("id") Long id, @Param("studentId") Long studentId);

    @Update("update submission set status=#{targetStatus}, updated_at=now() where id=#{id} and status=#{currentStatus}")
    int updateStatusIfCurrent(@Param("id") Long id,
                              @Param("currentStatus") String currentStatus,
                              @Param("targetStatus") String targetStatus);

    @Update("update submission set counselor_ready_at=#{counselorReadyAt}, updated_at=now() where id=#{id}")
    int updateCounselorReadyAt(@Param("id") Long id, @Param("counselorReadyAt") LocalDateTime counselorReadyAt);
}

