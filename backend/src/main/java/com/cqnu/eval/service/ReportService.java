package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.model.entity.ActivityItemEntity;
import com.cqnu.eval.model.entity.SubmissionEntity;
import com.cqnu.eval.model.entity.UserEntity;
import com.cqnu.eval.security.CurrentUser;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
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
        map.put("allowedFormats", Arrays.asList("DOCX", "PDF"));
        map.put("scoreVersion", "SUBMITTED".equalsIgnoreCase(submission.getStatus()) ? "SUBMIT_VERSION" : "EFFECTIVE_VERSION");
        map.put("layoutPolicy", "WORD_TEMPLATE_FIRST");
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

        Map<String, Object> detail = submissionService.getSubmissionDetail(submissionId, user);
        Map<String, Object> score = submissionService.getScore(submissionId, user);
        UserEntity student = castUser(detail.get("student"));
        String baseName = buildBaseFileName(student, submissionId);

        String normalized = format == null ? "" : format.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "DOCX":
                return new ExportFile(
                        baseName + ".docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        reportTemplateService.buildDocxByTemplate(detail, score, submission)
                );
            case "PDF":
                return new ExportFile(
                        baseName + ".pdf",
                        "application/pdf",
                        buildPdf(detail, score, submission)
                );
            default:
                throw new BizException(40001, "不支持的导出格式: " + format);
        }
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

    private byte[] buildPdf(Map<String, Object> detail, Map<String, Object> score, SubmissionEntity submission) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            UserEntity student = castUser(detail.get("student"));
            List<ActivityItemEntity> activities = castList(detail.get("activities"));

            Document document = new Document(PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = createPdfFont(16, Font.BOLD);
            Font normal = createPdfFont(10, Font.NORMAL);
            Font bold = createPdfFont(10, Font.BOLD);

            Paragraph title = new Paragraph("数学与大数据学院综合奖学金申请表", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph version = new Paragraph("状态：" + statusLabel(submission.getStatus()), normal);
            version.setAlignment(Element.ALIGN_CENTER);
            version.setSpacingAfter(10f);
            document.add(version);

            PdfPTable basic = new PdfPTable(4);
            basic.setWidthPercentage(100);
            addCell(basic, "姓名", bold);
            addCell(basic, student == null ? "" : str(student.getRealName()), normal);
            addCell(basic, "学号", bold);
            addCell(basic, student == null ? "" : str(student.getStudentNo()), normal);
            addCell(basic, "年级班级", bold);
            addCell(basic, student == null ? "" : str(student.getClassName()), normal);
            addCell(basic, "联系电话", bold);
            addCell(basic, student == null ? "" : str(student.getPhone()), normal);
            basic.setSpacingAfter(8f);
            document.add(basic);

            PdfPTable scoreTable = new PdfPTable(4);
            scoreTable.setWidthPercentage(100);
            addCell(scoreTable, "德育得分", bold);
            addCell(scoreTable, bd(readDecimal(score, "moralRaw")), normal);
            addCell(scoreTable, "智育得分", bold);
            addCell(scoreTable, bd(readDecimal(score, "intelRaw")), normal);
            addCell(scoreTable, "体育得分", bold);
            addCell(scoreTable, bd(readDecimal(score, "sportRaw")), normal);
            addCell(scoreTable, "美育得分", bold);
            addCell(scoreTable, bd(readDecimal(score, "artRaw")), normal);
            addCell(scoreTable, "劳育得分", bold);
            addCell(scoreTable, bd(readDecimal(score, "laborRaw")), normal);
            addCell(scoreTable, "综合测评分数", bold);
            addCell(scoreTable, bd(readDecimal(score, "totalScore")), normal);
            scoreTable.setSpacingAfter(8f);
            document.add(scoreTable);

            document.add(new Paragraph("专业技能与创新创业：" + joinModule(activities, "INTEL_PRO_INNOV"), normal));
            document.add(new Paragraph("德育申请事由：" + joinModule(activities, "MORAL"), normal));
            document.add(new Paragraph("体育申请事由：" + joinModule(activities, "SPORT_ACTIVITY"), normal));
            document.add(new Paragraph("美育申请事由：" + joinModule(activities, "ART"), normal));
            document.add(new Paragraph("劳育申请事由：" + joinModule(activities, "LABOR"), normal));
            document.add(new Paragraph("说明：PDF为功能性导出，版式基准请使用Word模板导出。", normal));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(50001, "PDF 导出失败: " + e.getMessage());
        }
    }

    private Font createPdfFont(float size, int style) {
        try {
            BaseFont base = BaseFont.createFont("C:/Windows/Fonts/simsun.ttc,0", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            return new Font(base, size, style);
        } catch (Exception ignore) {
            return new Font(Font.HELVETICA, size, style);
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private String joinModule(List<ActivityItemEntity> list, String module) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (ActivityItemEntity item : list) {
            if (item == null || !module.equalsIgnoreCase(item.getModuleType())) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("; ");
            }
            BigDecimal value = item.getFinalScore() == null ? item.getSelfScore() : item.getFinalScore();
            sb.append(index++)
                    .append(". ")
                    .append(str(item.getTitle()))
                    .append(" (")
                    .append(bd(value))
                    .append(")");
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    private BigDecimal readDecimal(Map<String, Object> score, String key) {
        Object value = score.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ignore) {
            return BigDecimal.ZERO;
        }
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

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object obj) {
        return (List<T>) obj;
    }

    private String statusLabel(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            return "-";
        }
        switch (code) {
            case "DRAFT":
                return "草稿";
            case "SUBMITTED":
                return "已提交";
            case "FINALIZED":
                return "已终审";
            case "PUBLISHED":
                return "已公示";
            default:
                return raw;
        }
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String bd(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
