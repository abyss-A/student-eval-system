package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.model.entity.ActivityItemEntity;
import com.cqnu.eval.model.entity.CourseItemEntity;
import com.cqnu.eval.model.entity.SubmissionEntity;
import com.cqnu.eval.security.CurrentUser;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportService {

    private final SubmissionMapper submissionMapper;
    private final SubmissionService submissionService;

    public ReportService(SubmissionMapper submissionMapper, SubmissionService submissionService) {
        this.submissionMapper = submissionMapper;
        this.submissionService = submissionService;
    }

    public Map<String, Object> availability(Long submissionId, CurrentUser user) {
        SubmissionEntity submission = submissionMapper.findById(submissionId);
        if (submission == null) {
            throw new BizException(40401, "Submission not found");
        }
        if (submissionMapper.checkOwner(submissionId, user.getId()) == 0) {
            throw new BizException(40301, "Forbidden");
        }
        boolean canExport = !"DRAFT".equals(submission.getStatus());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("canExport", canExport);
        map.put("allowedFormats", Arrays.asList("DOCX", "PDF"));
        map.put("scoreVersion", "SUBMITTED".equals(submission.getStatus()) ? "SUBMIT_VERSION" : "EFFECTIVE_VERSION");
        map.put("layoutPolicy", "ONE_PAGE_FIRST_MIN_7PT_THEN_TWO_PAGES");
        return map;
    }

    public ExportFile export(Long submissionId, String format, CurrentUser user) {
        SubmissionEntity submission = submissionMapper.findById(submissionId);
        if (submission == null) {
            throw new BizException(40401, "Submission not found");
        }
        if (submissionMapper.checkOwner(submissionId, user.getId()) == 0) {
            throw new BizException(40301, "Forbidden");
        }
        if ("DRAFT".equals(submission.getStatus())) {
            throw new BizException(40003, "Only submitted record can be exported");
        }

        Map<String, Object> detail = submissionService.getSubmissionDetail(submissionId, user);
        Map<String, Object> score = submissionService.getScore(submissionId, user);

        if ("DOCX".equalsIgnoreCase(format)) {
            byte[] data = buildDocx(detail, score, submission);
            return new ExportFile("report_" + submissionId + ".docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", data);
        }
        if ("PDF".equalsIgnoreCase(format)) {
            byte[] data = buildPdf(detail, score, submission);
            return new ExportFile("report_" + submissionId + ".pdf", "application/pdf", data);
        }
        throw new BizException(40001, "Unsupported format");
    }

    private byte[] buildDocx(Map<String, Object> detail, Map<String, Object> score, SubmissionEntity submission) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Map<String, Object> student = castMap(detail.get("student"));
            List<CourseItemEntity> courses = castList(detail.get("courses"));
            List<ActivityItemEntity> acts = castList(detail.get("activities"));

            int fontSize = chooseFontSize(acts);

            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = title.createRun();
            run.setBold(true);
            run.setFontFamily("宋体");
            run.setFontSize(16);
            run.setText("综合测评报告");

            XWPFParagraph subtitle = doc.createParagraph();
            subtitle.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subRun = subtitle.createRun();
            subRun.setFontFamily("宋体");
            subRun.setFontSize(10);
            subRun.setText("版本: " + ("SUBMITTED".equals(submission.getStatus()) ? "提交版" : "终审版"));

            XWPFTable basic = doc.createTable(2, 4);
            setCell(basic, 0, 0, "姓名", fontSize, true);
            setCell(basic, 0, 1, str(student.get("realName")), fontSize, false);
            setCell(basic, 0, 2, "学号", fontSize, true);
            setCell(basic, 0, 3, str(student.get("studentNo")), fontSize, false);
            setCell(basic, 1, 0, "班级", fontSize, true);
            setCell(basic, 1, 1, str(student.get("className")), fontSize, false);
            setCell(basic, 1, 2, "专业", fontSize, true);
            setCell(basic, 1, 3, str(student.get("majorName")), fontSize, false);

            XWPFParagraph cTitle = doc.createParagraph();
            XWPFRun cRun = cTitle.createRun();
            cRun.setFontFamily("宋体");
            cRun.setBold(true);
            cRun.setFontSize(fontSize + 1);
            cRun.setText("课程成绩");

            XWPFTable cTable = doc.createTable(courses.size() + 1, 4);
            setCell(cTable, 0, 0, "课程名称", fontSize, true);
            setCell(cTable, 0, 1, "类型", fontSize, true);
            setCell(cTable, 0, 2, "成绩", fontSize, true);
            setCell(cTable, 0, 3, "学分", fontSize, true);
            for (int i = 0; i < courses.size(); i++) {
                CourseItemEntity c = courses.get(i);
                setCell(cTable, i + 1, 0, str(c.getCourseName()), fontSize, false);
                setCell(cTable, i + 1, 1, str(c.getCourseType()), fontSize, false);
                setCell(cTable, i + 1, 2, bd(c.getReviewerScore() == null ? c.getScore() : c.getReviewerScore()), fontSize, false);
                setCell(cTable, i + 1, 3, bd(c.getCredit()), fontSize, false);
            }

            XWPFParagraph aTitle = doc.createParagraph();
            XWPFRun aRun = aTitle.createRun();
            aRun.setFontFamily("宋体");
            aRun.setBold(true);
            aRun.setFontSize(fontSize + 1);
            aRun.setText("板块申请事由");

            XWPFTable moduleTable = doc.createTable(5, 2);
            setCell(moduleTable, 0, 0, "德育", fontSize, true);
            setCell(moduleTable, 0, 1, joinModule(acts, "MORAL"), fontSize, false);
            setCell(moduleTable, 1, 0, "智育(专创)", fontSize, true);
            setCell(moduleTable, 1, 1, joinModule(acts, "INTEL_PRO_INNOV"), fontSize, false);
            setCell(moduleTable, 2, 0, "体育", fontSize, true);
            setCell(moduleTable, 2, 1, joinModule(acts, "SPORT_ACTIVITY"), fontSize, false);
            setCell(moduleTable, 3, 0, "美育", fontSize, true);
            setCell(moduleTable, 3, 1, joinModule(acts, "ART"), fontSize, false);
            setCell(moduleTable, 4, 0, "劳动", fontSize, true);
            setCell(moduleTable, 4, 1, joinModule(acts, "LABOR"), fontSize, false);

            XWPFParagraph sTitle = doc.createParagraph();
            XWPFRun sRun = sTitle.createRun();
            sRun.setFontFamily("宋体");
            sRun.setBold(true);
            sRun.setFontSize(fontSize + 1);
            sRun.setText("计分结果");

            XWPFTable scoreTable = doc.createTable(3, 4);
            setCell(scoreTable, 0, 0, "德育", fontSize, true);
            setCell(scoreTable, 0, 1, bd((BigDecimal) score.get("moralRaw")), fontSize, false);
            setCell(scoreTable, 0, 2, "智育", fontSize, true);
            setCell(scoreTable, 0, 3, bd((BigDecimal) score.get("intelRaw")), fontSize, false);
            setCell(scoreTable, 1, 0, "体育", fontSize, true);
            setCell(scoreTable, 1, 1, bd((BigDecimal) score.get("sportRaw")), fontSize, false);
            setCell(scoreTable, 1, 2, "美育", fontSize, true);
            setCell(scoreTable, 1, 3, bd((BigDecimal) score.get("artRaw")), fontSize, false);
            setCell(scoreTable, 2, 0, "劳动", fontSize, true);
            setCell(scoreTable, 2, 1, bd((BigDecimal) score.get("laborRaw")), fontSize, false);
            setCell(scoreTable, 2, 2, "综合分", fontSize, true);
            setCell(scoreTable, 2, 3, bd((BigDecimal) score.get("totalScore")), fontSize, false);

            XWPFParagraph note = doc.createParagraph();
            XWPFRun noteRun = note.createRun();
            noteRun.setFontFamily("宋体");
            noteRun.setFontSize(Math.max(7, fontSize - 1));
            noteRun.setText("说明: 报告仅包含主体信息，证明材料仍在系统内查看。公式: " + score.get("formula"));

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(50001, "DOCX export failed: " + e.getMessage());
        }
    }

    private byte[] buildPdf(Map<String, Object> detail, Map<String, Object> score, SubmissionEntity submission) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(PageSize.A4, 30, 30, 30, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = createFont(16, Font.BOLD);
            Font normal = createFont(10, Font.NORMAL);
            Font bold = createFont(10, Font.BOLD);

            Paragraph title = new Paragraph("综合测评报告", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            Paragraph version = new Paragraph("版本: " + ("SUBMITTED".equals(submission.getStatus()) ? "提交版" : "终审版"), normal);
            version.setAlignment(Element.ALIGN_CENTER);
            version.setSpacingAfter(10f);
            doc.add(version);

            Map<String, Object> student = castMap(detail.get("student"));
            PdfPTable basic = new PdfPTable(4);
            basic.setWidthPercentage(100);
            addCell(basic, "姓名", bold);
            addCell(basic, str(student.get("realName")), normal);
            addCell(basic, "学号", bold);
            addCell(basic, str(student.get("studentNo")), normal);
            addCell(basic, "班级", bold);
            addCell(basic, str(student.get("className")), normal);
            addCell(basic, "专业", bold);
            addCell(basic, str(student.get("majorName")), normal);
            basic.setSpacingAfter(8f);
            doc.add(basic);

            PdfPTable scoreTable = new PdfPTable(4);
            scoreTable.setWidthPercentage(100);
            addCell(scoreTable, "德育", bold);
            addCell(scoreTable, bd((BigDecimal) score.get("moralRaw")), normal);
            addCell(scoreTable, "智育", bold);
            addCell(scoreTable, bd((BigDecimal) score.get("intelRaw")), normal);
            addCell(scoreTable, "体育", bold);
            addCell(scoreTable, bd((BigDecimal) score.get("sportRaw")), normal);
            addCell(scoreTable, "美育", bold);
            addCell(scoreTable, bd((BigDecimal) score.get("artRaw")), normal);
            addCell(scoreTable, "劳动", bold);
            addCell(scoreTable, bd((BigDecimal) score.get("laborRaw")), normal);
            addCell(scoreTable, "综合分", bold);
            addCell(scoreTable, bd((BigDecimal) score.get("totalScore")), normal);
            scoreTable.setSpacingAfter(8f);
            doc.add(scoreTable);

            List<ActivityItemEntity> acts = castList(detail.get("activities"));
            doc.add(new Paragraph("德育: " + joinModule(acts, "MORAL"), normal));
            doc.add(new Paragraph("智育(专创): " + joinModule(acts, "INTEL_PRO_INNOV"), normal));
            doc.add(new Paragraph("体育: " + joinModule(acts, "SPORT_ACTIVITY"), normal));
            doc.add(new Paragraph("美育: " + joinModule(acts, "ART"), normal));
            doc.add(new Paragraph("劳动: " + joinModule(acts, "LABOR"), normal));
            doc.add(new Paragraph("说明: 证明材料不在该导出文件中。", normal));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(50001, "PDF export failed: " + e.getMessage());
        }
    }

    private Font createFont(float size, int style) {
        try {
            BaseFont bf = BaseFont.createFont("C:/Windows/Fonts/simsun.ttc,0", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style);
        } catch (Exception ignore) {
            return new Font(Font.HELVETICA, size, style);
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private int chooseFontSize(List<ActivityItemEntity> acts) {
        int len = 0;
        for (ActivityItemEntity a : acts) {
            len += safeLen(a.getTitle()) + safeLen(a.getDescription()) + 10;
        }
        if (len <= 1200) {
            return 11;
        }
        if (len <= 1800) {
            return 9;
        }
        if (len <= 2400) {
            return 8;
        }
        return 7;
    }

    private int safeLen(String s) {
        return s == null ? 0 : s.length();
    }

    private String joinModule(List<ActivityItemEntity> list, String module) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (ActivityItemEntity a : list) {
            if (module.equalsIgnoreCase(a.getModuleType())) {
                if (sb.length() > 0) {
                    sb.append("；");
                }
                sb.append(i++)
                        .append(".")
                        .append(str(a.getTitle()))
                        .append("(")
                        .append(bd(a.getFinalScore() == null ? a.getSelfScore() : a.getFinalScore()))
                        .append("分)");
            }
        }
        return sb.length() == 0 ? "无" : sb.toString();
    }

    private void setCell(XWPFTable table, int row, int col, String text, int fontSize, boolean bold) {
        XWPFTableCell cell = table.getRow(row).getCell(col);
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        XWPFRun r = p.createRun();
        r.setFontFamily("宋体");
        r.setFontSize(fontSize);
        r.setBold(bold);
        r.setText(text == null ? "" : text);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object obj) {
        return (List<T>) obj;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String bd(BigDecimal b) {
        return b == null ? "0.00" : b.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
