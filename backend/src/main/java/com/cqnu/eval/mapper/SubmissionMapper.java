package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.SubmissionEntity;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
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

    @Select("select s.*, u.real_name, u.class_name, u.major_name from submission s join sys_user u on s.student_id=u.id where s.semester_id=#{semesterId} and s.status in ('SUBMITTED','COUNSELOR_REVIEWED','FINALIZED','PUBLISHED') order by s.total_score desc")
    List<java.util.Map<String, Object>> listForRanking(@Param("semesterId") Long semesterId);

    @Select("select s.*, u.student_no, u.real_name, u.class_name, u.major_name from submission s join sys_user u on s.student_id=u.id where s.status='SUBMITTED' order by s.submitted_at desc")
    List<java.util.Map<String, Object>> listSubmittedTasks();

    @Select("select count(1) from submission where id=#{id} and student_id=#{studentId}")
    int checkOwner(@Param("id") Long id, @Param("studentId") Long studentId);
}
