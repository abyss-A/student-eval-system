package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.AttachmentEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AttachmentMapper {

    @Insert("insert into attachment(biz_type, biz_id, uploader_id, file_name, file_path, file_size, mime_type, created_at) " +
            "values(#{bizType}, #{bizId}, #{uploaderId}, #{fileName}, #{filePath}, #{fileSize}, #{mimeType}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AttachmentEntity entity);

    @Select("select * from attachment where id = #{id} limit 1")
    AttachmentEntity findById(@Param("id") Long id);

    @Select("select * from attachment where uploader_id = #{uploaderId} order by id desc")
    List<AttachmentEntity> listByUploader(@Param("uploaderId") Long uploaderId);

    @Select("select count(1) from course_item c join submission s on c.submission_id=s.id " +
            "where c.evidence_file_id = #{id} and s.status <> 'DRAFT'")
    int countReferencedByCourse(@Param("id") Long id);

    @Select("select count(1) from activity_item a join submission s on a.submission_id=s.id " +
            "where a.evidence_file_ids is not null and a.evidence_file_ids <> '' and " +
            "find_in_set(#{id}, a.evidence_file_ids) and s.status <> 'DRAFT'")
    int countReferencedByActivity(@Param("id") Long id);
}
