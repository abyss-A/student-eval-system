package com.cqnu.eval.service;

import com.cqnu.eval.common.BizException;
import com.cqnu.eval.model.entity.ActivityItemEntity;
import com.cqnu.eval.model.entity.CourseItemEntity;
import com.cqnu.eval.model.entity.SubmissionEntity;
import com.cqnu.eval.model.entity.UserEntity;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportTemplateService {

    private static final BigDecimal WEIGHT_MORAL = new BigDecimal("0.15");
    private static final BigDecimal WEIGHT_INTEL = new BigDecimal("0.60");
    private static final BigDecimal WEIGHT_SPORT = new BigDecimal("0.10");
    private static final BigDecimal WEIGHT_ART = new BigDecimal("0.075");
    private static final BigDecimal WEIGHT_LABOR = new BigDecimal("0.075");

    private final String templateName;

    public ReportTemplateService(@Value("${report.template.name:scholarship_form_v1.docx}") String templateName) {
        this.templateName = templateName;
    }

    public byte[] buildDocxByTemplate(Map<String, Object> detail, Map<String, Object> score, SubmissionEntity submission) {
        UserEntity student = castUser(detail.get("student"));
        List<CourseItemEntity> courses = castList(detail.get("courses"));
        List<ActivityItemEntity> activities = castList(detail.get("activities"));

        try (InputStream in = loadTemplate();
             XWPFDocument doc = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            if (doc.getTables().isEmpty()) {
                throw new BizException(50001, "模板格式不正确：未找到表格结构");
            }
            XWPFTable table = doc.getTables().get(0);
            if (table.getNumberOfRows() < 18) {
                throw new BizException(50001, "模板格式不正确：关键行数量不足");
            }

            int fillFontSize = resolveFillFontSize(courses, activities);

            fillBaseInfo(table, student, fillFontSize);
            fillCourseSummary(table, courses, fillFontSize);
            fillCourses(table, courses, fillFontSize);
            fillCourseFormula(table, courses, fillFontSize);
            fillActivities(table, activities, score, fillFontSize);
            fillFinalScore(table, score, fillFontSize);
            removeNoteSection(doc);

            doc.write(out);
            return out.toByteArray();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(50001, "模板填充失败: " + ex.getMessage());
        }
    }

    private InputStream loadTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("report-templates/" + templateName);
            if (!resource.exists()) {
                throw new BizException(50001, "模板不存在: " + templateName);
            }
            return resource.getInputStream();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(50001, "读取模板失败: " + ex.getMessage());
        }
    }

    private int resolveFillFontSize(List<CourseItemEntity> courses, List<ActivityItemEntity> activities) {
        int courseLen = 0;
        for (CourseItemEntity c : courses) {
            if (c == null) {
                continue;
            }
            courseLen += safeLen(c.getCourseName()) + safeLen(c.getCourseType()) + 8;
        }

        int activityLen = 0;
        for (ActivityItemEntity a : activities) {
            if (a == null) {
                continue;
            }
            activityLen += safeLen(a.getTitle()) + safeLen(a.getDescription()) + 10;
        }

        int density = courseLen + activityLen * 2;
        if (density <= 500) {
            return 10;
        }
        if (density <= 900) {
            return 9;
        }
        if (density <= 1300) {
            return 8;
        }
        if (density <= 1800) {
            return 7;
        }
        return 6;
    }

    private void fillBaseInfo(XWPFTable table, UserEntity student, int fillFontSize) {
        setCellText(table, 0, 1, str(student == null ? null : student.getRealName()), fillFontSize);
        setCellText(table, 0, 3, normalizeGender(student == null ? null : student.getGender()), fillFontSize);
        setCellText(table, 0, 5, str(student == null ? null : student.getStudentNo()), fillFontSize);
        setCellText(table, 1, 1, "本科", fillFontSize);
        setCellText(table, 1, 3, str(student == null ? null : student.getClassName()), fillFontSize);

        String phone = str(student == null ? null : student.getPhone()).trim();
        setCellText(table, 1, 5, phone.isEmpty() ? "-" : phone, fillFontSize);

        // 智育得分仅保留竖排标题，不再把分数写在这里
        setCellText(table, 2, 0, "智\n育\n得\n分", fillFontSize);
    }

    private void fillCourseSummary(XWPFTable table, List<CourseItemEntity> courses, int fillFontSize) {
        int total = 0;
        int required = 0;
        int elective = 0;
        int retake = 0;
        int relearn = 0;
        int failed = 0;

        for (CourseItemEntity c : courses) {
            if (c == null) {
                continue;
            }
            total++;
            String type = upper(c.getCourseType());
            if ("REQUIRED".equals(type)) {
                required++;
            } else if ("ELECTIVE".equals(type)) {
                elective++;
            } else if ("RETAKE".equals(type)) {
                retake++;
            } else if ("RELEARN".equals(type)) {
                relearn++;
            }

            BigDecimal value = c.getReviewerScore() == null ? c.getScore() : c.getReviewerScore();
            if (safe(value).compareTo(new BigDecimal("60")) < 0) {
                failed++;
            }
        }

        String summary = String.format(
                "本学期共修 %s 门课，其中必修课 %s 门，选修课 %s 门，重修 %s 门，再修 %s 门；不及格 %s 门。",
                total, required, elective, retake, relearn, failed
        );
        setCellText(table, 2, 1, summary, fillFontSize);
    }

    private void fillCourses(XWPFTable table, List<CourseItemEntity> courses, int fillFontSize) {
        final int courseStartRow = 4;
        final int courseEndRow = 9;
        final int slotsPerRow = 3;
        final int maxSlots = (courseEndRow - courseStartRow + 1) * slotsPerRow;

        for (int i = 0; i < maxSlots; i++) {
            int row = courseStartRow + i / slotsPerRow;
            int block = i % slotsPerRow;
            int colName = 1 + block * 3;
            int colScore = colName + 1;
            int colCredit = colName + 2;

            if (i < courses.size()) {
                CourseItemEntity c = courses.get(i);
                BigDecimal score = c.getReviewerScore() == null ? c.getScore() : c.getReviewerScore();
                setCellText(table, row, colName, str(c.getCourseName()), fillFontSize);
                setCellText(table, row, colScore, formatScore(score), fillFontSize);
                setCellText(table, row, colCredit, formatScore(c.getCredit()), fillFontSize);
            } else {
                setCellText(table, row, colName, "", fillFontSize);
                setCellText(table, row, colScore, "", fillFontSize);
                setCellText(table, row, colCredit, "", fillFontSize);
            }
        }
    }

    private void fillCourseFormula(XWPFTable table, List<CourseItemEntity> courses, int fillFontSize) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal creditSum = BigDecimal.ZERO;
        for (CourseItemEntity c : courses) {
            if (c == null) {
                continue;
            }
            BigDecimal score = safe(c.getReviewerScore() == null ? c.getScore() : c.getReviewerScore());
            BigDecimal credit = safe(c.getCredit());
            weightedSum = weightedSum.add(score.multiply(credit));
            creditSum = creditSum.add(credit);
        }

        BigDecimal avg = creditSum.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : weightedSum.divide(creditSum, 4, RoundingMode.HALF_UP);

        XWPFTableCell cell = requireCell(table, 10, 1);
        RunStyle style = snapshotFirstCellRunStyle(cell);
        while (cell.getParagraphs().size() > 0) {
            cell.removeParagraph(0);
        }

        int formulaFontSize = Math.max(8, Math.min(fillFontSize, 10));

        XWPFParagraph p1 = cell.addParagraph();
        initParagraph(p1, ParagraphAlignment.CENTER);
        XWPFRun p1Run = p1.createRun();
        applyStyle(p1Run, style, formulaFontSize);
        p1Run.setText("∑（所修课程成绩×该课程学分）");

        XWPFParagraph p2 = cell.addParagraph();
        initParagraph(p2, ParagraphAlignment.CENTER);
        XWPFRun p2Prefix = p2.createRun();
        applyStyle(p2Prefix, style, formulaFontSize);
        p2Prefix.setText("本期课程成绩 = ");

        XWPFRun lineRun = p2.createRun();
        applyStyle(lineRun, style, formulaFontSize);
        lineRun.setUnderline(UnderlinePatterns.SINGLE);
        lineRun.setText("                                        ");

        XWPFRun p2Suffix = p2.createRun();
        applyStyle(p2Suffix, style, formulaFontSize);
        p2Suffix.setUnderline(UnderlinePatterns.NONE);
        p2Suffix.setText(" = （" + formatScore2(avg) + "）分。");

        XWPFParagraph p3 = cell.addParagraph();
        initParagraph(p3, ParagraphAlignment.CENTER);
        XWPFRun p3Run = p3.createRun();
        applyStyle(p3Run, style, formulaFontSize);
        p3Run.setText("∑所修课程学分");
    }

    private void fillActivities(XWPFTable table, List<ActivityItemEntity> activities, Map<String, Object> score, int fillFontSize) {
        setCellText(table, 12, 2, joinActivities(activities, "INTEL_PRO_INNOV"), fillFontSize, ParagraphAlignment.LEFT);
        setRightmostCellText(table, 12, formatScore2(readWeightedScore(score, "intelScore", "intelRaw", WEIGHT_INTEL)), fillFontSize);

        setCellText(table, 13, 2, joinActivities(activities, "MORAL"), fillFontSize, ParagraphAlignment.LEFT);
        setRightmostCellText(table, 13, formatScore2(readWeightedScore(score, "moralScore", "moralRaw", WEIGHT_MORAL)), fillFontSize);

        setCellText(table, 14, 2, joinActivities(activities, "SPORT_ACTIVITY"), fillFontSize, ParagraphAlignment.LEFT);
        setRightmostCellText(table, 14, formatScore2(readWeightedScore(score, "sportScore", "sportRaw", WEIGHT_SPORT)), fillFontSize);

        setCellText(table, 15, 2, joinActivities(activities, "ART"), fillFontSize, ParagraphAlignment.LEFT);
        setRightmostCellText(table, 15, formatScore2(readWeightedScore(score, "artScore", "artRaw", WEIGHT_ART)), fillFontSize);

        setCellText(table, 16, 2, joinActivities(activities, "LABOR"), fillFontSize, ParagraphAlignment.LEFT);
        setRightmostCellText(table, 16, formatScore2(readWeightedScore(score, "laborScore", "laborRaw", WEIGHT_LABOR)), fillFontSize);
    }

    private void fillFinalScore(XWPFTable table, Map<String, Object> score, int fillFontSize) {
        String total = formatScore2(readDecimal(score, "totalScore"));
        setCellText(table, 17, 1, total, fillFontSize);
        setRightmostCellText(table, 17, total, fillFontSize);
    }

    private void removeNoteSection(XWPFDocument doc) {
        List<IBodyElement> elements = doc.getBodyElements();
        int firstTableIndex = -1;
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).getElementType() == BodyElementType.TABLE) {
                firstTableIndex = i;
                break;
            }
        }
        if (firstTableIndex < 0) {
            return;
        }

        // 保留申请表标题，删除说明段落
        for (int i = doc.getBodyElements().size() - 1; i > firstTableIndex; i--) {
            IBodyElement element = doc.getBodyElements().get(i);
            if (element.getElementType() != BodyElementType.PARAGRAPH) {
                doc.removeBodyElement(i);
                continue;
            }
            XWPFParagraph paragraph = (XWPFParagraph) element;
            String text = str(paragraph.getText()).trim();
            if (text.contains("综合奖学金申请表")) {
                continue;
            }
            doc.removeBodyElement(i);
        }
    }

    private boolean hasCell(XWPFTable table, int rowIndex, int colIndex) {
        XWPFTableRow row = table.getRow(rowIndex);
        return row != null && row.getCell(colIndex) != null;
    }

    private String joinActivities(List<ActivityItemEntity> activities, String moduleType) {
        List<String> parts = new ArrayList<>();
        int index = 1;
        for (ActivityItemEntity activity : activities) {
            if (activity == null) {
                continue;
            }
            if (!moduleType.equalsIgnoreCase(str(activity.getModuleType()))) {
                continue;
            }

            BigDecimal value = activity.getFinalScore() == null ? activity.getSelfScore() : activity.getFinalScore();
            StringBuilder item = new StringBuilder();
            item.append(index).append("、").append(str(activity.getTitle()).trim());

            String desc = str(activity.getDescription()).trim();
            if (!desc.isEmpty()) {
                item.append("：").append(desc);
            }
            item.append("（").append(formatScore2(value)).append("分）");
            parts.add(item.toString());
            index++;
        }
        if (parts.isEmpty()) {
            return "-";
        }
        return String.join("；", parts);
    }

    private BigDecimal readWeightedScore(Map<String, Object> score, String weightedKey, String rawKey, BigDecimal weight) {
        if (score != null && score.containsKey(weightedKey)) {
            return readDecimal(score, weightedKey);
        }
        BigDecimal raw = readDecimal(score, rawKey);
        return raw.multiply(weight);
    }

    private void setCellText(XWPFTable table, int rowIndex, int colIndex, String value, int fillFontSize) {
        setCellText(table, rowIndex, colIndex, value, fillFontSize, ParagraphAlignment.CENTER);
    }

    private void setCellText(XWPFTable table, int rowIndex, int colIndex, String value, int fillFontSize, ParagraphAlignment alignment) {
        XWPFTableCell cell = requireCell(table, rowIndex, colIndex);

        XWPFParagraph paragraph;
        if (cell.getParagraphs() == null || cell.getParagraphs().isEmpty()) {
            paragraph = cell.addParagraph();
        } else {
            paragraph = cell.getParagraphs().get(0);
            for (int i = cell.getParagraphs().size() - 1; i > 0; i--) {
                cell.removeParagraph(i);
            }
        }
        initParagraph(paragraph, alignment);

        RunStyle style = snapshotFirstRunStyle(paragraph);
        while (paragraph.getRuns().size() > 0) {
            paragraph.removeRun(0);
        }

        XWPFRun run = paragraph.createRun();
        applyStyle(run, style, fillFontSize);
        writeTextWithLineBreaks(run, str(value));
    }

    private void setRightmostCellText(XWPFTable table, int rowIndex, String value, int fillFontSize) {
        XWPFTableRow row = table.getRow(rowIndex);
        if (row == null || row.getTableCells() == null || row.getTableCells().isEmpty()) {
            throw new BizException(50001, "模板字段定位失败: row=" + rowIndex);
        }
        int lastColIndex = row.getTableCells().size() - 1;
        setCellText(table, rowIndex, lastColIndex, value, fillFontSize, ParagraphAlignment.CENTER);
    }

    private XWPFTableCell requireCell(XWPFTable table, int rowIndex, int colIndex) {
        XWPFTableRow row = table.getRow(rowIndex);
        if (row == null) {
            throw new BizException(50001, "模板字段定位失败: row=" + rowIndex);
        }
        XWPFTableCell cell = row.getCell(colIndex);
        if (cell == null) {
            throw new BizException(50001, "模板字段定位失败: row=" + rowIndex + ", col=" + colIndex);
        }
        return cell;
    }

    private void initParagraph(XWPFParagraph paragraph, ParagraphAlignment alignment) {
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);
        paragraph.setSpacingBetween(1.0);
        paragraph.setIndentationLeft(0);
        paragraph.setIndentationFirstLine(0);
        paragraph.setAlignment(alignment == null ? ParagraphAlignment.CENTER : alignment);
    }

    private RunStyle snapshotFirstCellRunStyle(XWPFTableCell cell) {
        if (cell == null || cell.getParagraphs() == null || cell.getParagraphs().isEmpty()) {
            return new RunStyle();
        }
        return snapshotFirstRunStyle(cell.getParagraphs().get(0));
    }

    private void writeTextWithLineBreaks(XWPFRun run, String value) {
        String[] lines = value.split("\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(lines[i]);
        }
    }

    private RunStyle snapshotFirstRunStyle(XWPFParagraph paragraph) {
        RunStyle style = new RunStyle();
        if (paragraph.getRuns() == null || paragraph.getRuns().isEmpty()) {
            return style;
        }

        XWPFRun first = paragraph.getRuns().get(0);
        style.fontSize = first.getFontSize();
        style.bold = first.isBold();
        style.italic = first.isItalic();
        style.color = first.getColor();
        style.underline = first.getUnderline();
        return style;
    }

    private void applyStyle(XWPFRun run, RunStyle style, int fillFontSize) {
        RunStyle resolvedStyle = style == null ? new RunStyle() : style;
        run.setBold(resolvedStyle.bold);
        run.setItalic(resolvedStyle.italic);
        if (resolvedStyle.color != null && !resolvedStyle.color.isEmpty()) {
            run.setColor(resolvedStyle.color);
        }
        if (resolvedStyle.underline != null) {
            run.setUnderline(resolvedStyle.underline);
        }

        int fontSize = fillFontSize > 0 ? fillFontSize : (resolvedStyle.fontSize > 0 ? resolvedStyle.fontSize : 10);
        run.setFontSize(fontSize);

        CTRPr rPr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        CTFonts rFonts = rPr.sizeOfRFontsArray() > 0 ? rPr.getRFontsArray(0) : rPr.addNewRFonts();
        rFonts.setEastAsia("宋体");
        rFonts.setAscii("Times New Roman");
        rFonts.setHAnsi("Times New Roman");
        rFonts.setCs("Times New Roman");
    }

    private String normalizeGender(String raw) {
        String value = str(raw).trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            return "";
        }
        if ("M".equals(value) || "MALE".equals(value) || "男".equals(raw)) {
            return "男";
        }
        if ("F".equals(value) || "FEMALE".equals(value) || "女".equals(raw)) {
            return "女";
        }
        return str(raw);
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private int safeLen(String value) {
        return value == null ? 0 : value.trim().length();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal readDecimal(Map<String, Object> score, String key) {
        if (score == null || key == null) {
            return BigDecimal.ZERO;
        }
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

    private String formatScore(BigDecimal value) {
        BigDecimal n = safe(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return n.toPlainString();
    }

    private String formatScore2(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
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
        if (obj == null) {
            return new ArrayList<>();
        }
        if (obj instanceof List) {
            return (List<T>) obj;
        }
        throw new BizException(50001, "模板填充数据类型异常: " + obj.getClass().getName());
    }

    private static class RunStyle {
        int fontSize;
        boolean bold;
        boolean italic;
        String color;
        UnderlinePatterns underline;
    }
}
