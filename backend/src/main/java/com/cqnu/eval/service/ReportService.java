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
            throw new BizException(40401, "Submission not found");
        }
        if (submissionMapper.checkOwner(submissionId, user.getId()) == 0) {
            throw new BizException(40301, "No permission to access this submission");
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
            throw new BizException(40401, "Submission not found");
        }
        if (submissionMapper.checkOwner(submissionId, user.getId()) == 0) {
            throw new BizException(40301, "No permission to access this submission");
        }
        if ("DRAFT".equalsIgnoreCase(submission.getStatus())) {
            throw new BizException(40003, "Draft submission cannot export. Please submit first.");
        }

        String normalized = format == null ? "" : format.trim().toUpperCase(Locale.ROOT);
        if (!"DOCX".equals(normalized)) {
            throw new BizException(40001, "Only DOCX export is supported");
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
        throw new BizException(50001, "闂佽瀛╅崘缁樹繆閸ヮ剙鏋侀柟鎯ь嚟閳瑰秹鏌嶉埡浣告殨缂佽鲸鐗滅槐鎺楊敃閵夘喖娈梺璇查閸㈡煡顢氶敐澶婄＜婵ê宕悡? " + obj.getClass().getName());
    }

    private String buildBaseFileName(UserEntity student, Long submissionId) {
        String accountNo = safeFilePart(student == null ? null : student.getAccountNo());
        String realName = safeFilePart(student == null ? null : student.getRealName());
        if (accountNo.isEmpty() && realName.isEmpty()) {
            return "Scholarship_Form_" + submissionId;
        }
        return "Scholarship_Form_" + accountNo + "_" + realName;
    }

    private String safeFilePart(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return "";
        }
        return text.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }
}

