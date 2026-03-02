package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.model.entity.SubmissionEntity;
import com.cqnu.eval.model.entity.UserEntity;
import com.cqnu.eval.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportService {

    private final SubmissionMapper submissionMapper;
    private final SubmissionService submissionService;
    private final ReportTemplateService reportTemplateService;
    private final String templateVersion;

    public ReportService(SubmissionMapper submissionMapper,
                         SubmissionService submissionService,
                         ReportTemplateService reportTemplateService,
                         @Value("${report.template.version:v1}") String templateVersion) {
        this.submissionMapper = submissionMapper;
        this.submissionService = submissionService;
        this.reportTemplateService = reportTemplateService;
        this.templateVersion = templateVersion;
    }

    public Map<String, Object> availability(Long submissionId, CurrentUser user) {
        SubmissionEntity submission = submissionMapper.findById(submissionId);
        if (submission == null) {
            throw new BizException(40401, "测评单不存在");
        }
        if (submissionMapper.checkOwner(submissionId, user.getId()) == 0) {
            throw new BizException(40301, "无权限访问该测评单");
        }

        boolean canExport = !"DRAFT".equalsIgnoreCase(submission.getStatus());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("canExport", canExport);
        map.put("allowedFormats", List.of("DOCX"));
        map.put("scoreVersion", "SUBMITTED".equalsIgnoreCase(submission.getStatus()) ? "SUBMIT_VERSION" : "EFFECTIVE_VERSION");
        map.put("layoutPolicy", "WORD_TEMPLATE_ONLY");
        map.put("templateVersion", templateVersion);
        return map;
    }

    public ExportFile export(Long submissionId, String format, CurrentUser user) {
        SubmissionEntity submission = submissionMapper.findById(submissionId);
        if (submission == null) {
            throw new BizException(40401, "测评单不存在");
        }
        if (submissionMapper.checkOwner(submissionId, user.getId()) == 0) {
            throw new BizException(40301, "无权限访问该测评单");
        }
        if ("DRAFT".equalsIgnoreCase(submission.getStatus())) {
            throw new BizException(40003, "仅已提交的测评单可以导出");
        }

        String normalized = format == null ? "" : format.trim().toUpperCase(Locale.ROOT);
        if (!"DOCX".equals(normalized)) {
            throw new BizException(40001, "当前仅支持导出Word(DOCX)");
        }

        Map<String, Object> detail = submissionService.getSubmissionDetail(submissionId, user);
        Map<String, Object> score = submissionService.getScore(submissionId, user);
        UserEntity student = castUser(detail.get("student"));
        String fileName = buildBaseFileName(student, submissionId) + ".docx";

        return new ExportFile(
                fileName,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                reportTemplateService.buildDocxByTemplate(detail, score, submission)
        );
    }

    private UserEntity castUser(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof UserEntity) {
            return (UserEntity) obj;
        }
        throw new BizException(50001, "学生信息类型异常: " + obj.getClass().getName());
    }

    private String buildBaseFileName(UserEntity student, Long submissionId) {
        String studentNo = safeFilePart(student == null ? null : student.getStudentNo());
        String realName = safeFilePart(student == null ? null : student.getRealName());
        if (studentNo.isEmpty() && realName.isEmpty()) {
            return "综合奖学金申请表_" + submissionId;
        }
        return "综合奖学金申请表_" + studentNo + "_" + realName;
    }

    private String safeFilePart(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return "";
        }
        return text.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }
}
