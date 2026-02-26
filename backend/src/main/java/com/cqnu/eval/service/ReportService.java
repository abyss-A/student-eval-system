package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.mapper.SubmissionMapper;
import com.cqnu.eval.model.entity.ActivityItemEntity;
import com.cqnu.eval.model.entity.CourseItemEntity;
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
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
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

        boolean canExport = !"DRAFT".equalsIgnoreCase(submission.getStatus());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("canExport", canExport);
        map.put("allowedFormats", Arrays.asList("DOCX", "PDF"));
        map.put("scoreVersion", "SUBMITTED".equalsIgnoreCase(submission.getStatus()) ? "SUBMIT_VERSION" : "EFFECTIVE_VERSION");
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
        if ("DRAFT".equalsIgnoreCase(submission.getStatus())) {
            throw new BizException(40003, "Only submitted records can be exported");
        }

        Map<String, Object> detail = submissionService.getSubmissionDetail(submissionId, user);
        Map<String, Object> score = submissionService.getScore(submissionId, user);

        String normalized = format == null ? "" : format.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "DOCX":
                return new ExportFile(
                        "report_" + submissionId + ".docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        buildDocx(detail, score, submission)
                );
            case "PDF":
                return new ExportFile(
                        "report_" + submissionId + ".pdf",
                        "application/pdf",
                        buildPdf(detail, score, submission)
                );
            default:
                throw new BizException(40001, "Unsupported format: " + format);
        }
    }

    private byte[] buildDocx(Map<String, Object> detail, Map<String, Object> score, SubmissionEntity submission) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            UserEntity student = castUser(detail.get("student"));
            List<CourseItemEntity> courses = castList(detail.get("courses"));
            List<ActivityItemEntity> activities = castList(detail.get("activities"));

            int fontSize = chooseFontSize(activities);

            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontFamily("SimSun");
            titleRun.setFontSize(16);
            titleRun.setText("Comprehensive Evaluation Report");

            XWPFParagraph subtitle = doc.createParagraph();
            subtitle.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subtitleRun = subtitle.createRun();
            subtitleRun.setFontFamily("SimSun");
            subtitleRun.setFontSize(10);
            subtitleRun.setText("Version: " + submission.getStatus());

            XWPFTable basic = doc.createTable(2, 4);
            setCell(basic, 0, 0, "Name", fontSize, true);
            setCell(basic, 0, 1, student == null ? "" : str(student.getRealName()), fontSize, false);
            setCell(basic, 0, 2, "Student No", fontSize, true);
            setCell(basic, 0, 3, student == null ? "" : str(student.getStudentNo()), fontSize, false);
            setCell(basic, 1, 0, "Class", fontSize, true);
            setCell(basic, 1, 1, student == null ? "" : str(student.getClassName()), fontSize, false);
            setCell(basic, 1, 2, "Major", fontSize, true);
            setCell(basic, 1, 3, student == null ? "" : str(student.getMajorName()), fontSize, false);

            XWPFParagraph courseTitle = doc.createParagraph();
            XWPFRun courseTitleRun = courseTitle.createRun();
            courseTitleRun.setBold(true);
            courseTitleRun.setFontFamily("SimSun");
            courseTitleRun.setFontSize(fontSize + 1);
            courseTitleRun.setText("Course Scores");

            XWPFTable courseTable = doc.createTable(courses.size() + 1, 4);
            setCell(courseTable, 0, 0, "Course Name", fontSize, true);
            setCell(courseTable, 0, 1, "Type", fontSize, true);
            setCell(courseTable, 0, 2, "Score", fontSize, true);
            setCell(courseTable, 0, 3, "Credit", fontSize, true);
            for (int i = 0; i < courses.size(); i++) {
                CourseItemEntity c = courses.get(i);
                setCell(courseTable, i + 1, 0, str(c.getCourseName()), fontSize, false);
                setCell(courseTable, i + 1, 1, str(c.getCourseType()), fontSize, false);
                setCell(courseTable, i + 1, 2, bd(c.getReviewerScore() == null ? c.getScore() : c.getReviewerScore()), fontSize, false);
                setCell(courseTable, i + 1, 3, bd(c.getCredit()), fontSize, false);
            }

            XWPFParagraph moduleTitle = doc.createParagraph();
            XWPFRun moduleTitleRun = moduleTitle.createRun();
            moduleTitleRun.setBold(true);
            moduleTitleRun.setFontFamily("SimSun");
            moduleTitleRun.setFontSize(fontSize + 1);
            moduleTitleRun.setText("Activity Modules");

            XWPFTable moduleTable = doc.createTable(5, 2);
            setCell(moduleTable, 0, 0, "Moral", fontSize, true);
            setCell(moduleTable, 0, 1, joinModule(activities, "MORAL"), fontSize, false);
            setCell(moduleTable, 1, 0, "Intel Innovation", fontSize, true);
            setCell(moduleTable, 1, 1, joinModule(activities, "INTEL_PRO_INNOV"), fontSize, false);
            setCell(moduleTable, 2, 0, "Sport", fontSize, true);
            setCell(moduleTable, 2, 1, joinModule(activities, "SPORT_ACTIVITY"), fontSize, false);
            setCell(moduleTable, 3, 0, "Art", fontSize, true);
            setCell(moduleTable, 3, 1, joinModule(activities, "ART"), fontSize, false);
            setCell(moduleTable, 4, 0, "Labor", fontSize, true);
            setCell(moduleTable, 4, 1, joinModule(activities, "LABOR"), fontSize, false);

            XWPFParagraph scoreTitle = doc.createParagraph();
            XWPFRun scoreTitleRun = scoreTitle.createRun();
            scoreTitleRun.setBold(true);
            scoreTitleRun.setFontFamily("SimSun");
            scoreTitleRun.setFontSize(fontSize + 1);
            scoreTitleRun.setText("Score Summary");

            XWPFTable scoreTable = doc.createTable(3, 4);
            setCell(scoreTable, 0, 0, "Moral", fontSize, true);
            setCell(scoreTable, 0, 1, bd(readDecimal(score, "moralRaw")), fontSize, false);
            setCell(scoreTable, 0, 2, "Intel", fontSize, true);
            setCell(scoreTable, 0, 3, bd(readDecimal(score, "intelRaw")), fontSize, false);
            setCell(scoreTable, 1, 0, "Sport", fontSize, true);
            setCell(scoreTable, 1, 1, bd(readDecimal(score, "sportRaw")), fontSize, false);
            setCell(scoreTable, 1, 2, "Art", fontSize, true);
            setCell(scoreTable, 1, 3, bd(readDecimal(score, "artRaw")), fontSize, false);
            setCell(scoreTable, 2, 0, "Labor", fontSize, true);
            setCell(scoreTable, 2, 1, bd(readDecimal(score, "laborRaw")), fontSize, false);
            setCell(scoreTable, 2, 2, "Total", fontSize, true);
            setCell(scoreTable, 2, 3, bd(readDecimal(score, "totalScore")), fontSize, false);

            XWPFParagraph note = doc.createParagraph();
            XWPFRun noteRun = note.createRun();
            noteRun.setFontFamily("SimSun");
            noteRun.setFontSize(Math.max(7, fontSize - 1));
            noteRun.setText("Note: Evidence files are not included in this export. Formula: " + str(score.get("formula")));

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(50001, "DOCX export failed: " + e.getMessage());
        }
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

            Paragraph title = new Paragraph("Comprehensive Evaluation Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph version = new Paragraph("Version: " + submission.getStatus(), normal);
            version.setAlignment(Element.ALIGN_CENTER);
            version.setSpacingAfter(10f);
            document.add(version);

            PdfPTable basic = new PdfPTable(4);
            basic.setWidthPercentage(100);
            addCell(basic, "Name", bold);
            addCell(basic, student == null ? "" : str(student.getRealName()), normal);
            addCell(basic, "Student No", bold);
            addCell(basic, student == null ? "" : str(student.getStudentNo()), normal);
            addCell(basic, "Class", bold);
            addCell(basic, student == null ? "" : str(student.getClassName()), normal);
            addCell(basic, "Major", bold);
            addCell(basic, student == null ? "" : str(student.getMajorName()), normal);
            basic.setSpacingAfter(8f);
            document.add(basic);

            PdfPTable scoreTable = new PdfPTable(4);
            scoreTable.setWidthPercentage(100);
            addCell(scoreTable, "Moral", bold);
            addCell(scoreTable, bd(readDecimal(score, "moralRaw")), normal);
            addCell(scoreTable, "Intel", bold);
            addCell(scoreTable, bd(readDecimal(score, "intelRaw")), normal);
            addCell(scoreTable, "Sport", bold);
            addCell(scoreTable, bd(readDecimal(score, "sportRaw")), normal);
            addCell(scoreTable, "Art", bold);
            addCell(scoreTable, bd(readDecimal(score, "artRaw")), normal);
            addCell(scoreTable, "Labor", bold);
            addCell(scoreTable, bd(readDecimal(score, "laborRaw")), normal);
            addCell(scoreTable, "Total", bold);
            addCell(scoreTable, bd(readDecimal(score, "totalScore")), normal);
            scoreTable.setSpacingAfter(8f);
            document.add(scoreTable);

            document.add(new Paragraph("Moral: " + joinModule(activities, "MORAL"), normal));
            document.add(new Paragraph("Intel Innovation: " + joinModule(activities, "INTEL_PRO_INNOV"), normal));
            document.add(new Paragraph("Sport: " + joinModule(activities, "SPORT_ACTIVITY"), normal));
            document.add(new Paragraph("Art: " + joinModule(activities, "ART"), normal));
            document.add(new Paragraph("Labor: " + joinModule(activities, "LABOR"), normal));
            document.add(new Paragraph("Note: Evidence files are not included in this export.", normal));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(50001, "PDF export failed: " + e.getMessage());
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

    private int chooseFontSize(List<ActivityItemEntity> acts) {
        int len = 0;
        for (ActivityItemEntity act : acts) {
            len += safeLen(act.getTitle()) + safeLen(act.getDescription()) + 10;
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

    private int safeLen(String value) {
        return value == null ? 0 : value.length();
    }

    private String joinModule(List<ActivityItemEntity> list, String module) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (ActivityItemEntity item : list) {
            if (module.equalsIgnoreCase(item.getModuleType())) {
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
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    private void setCell(XWPFTable table, int row, int col, String text, int fontSize, boolean bold) {
        XWPFTableCell cell = table.getRow(row).getCell(col);
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("SimSun");
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setText(text == null ? "" : text);
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
        throw new BizException(50001, "Unexpected student object type: " + obj.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object obj) {
        return (List<T>) obj;
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