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
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTInd;
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
import java.util.LinkedHashMap;
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

    private static final String TITLE_KEYWORD = "\u7efc\u5408\u5956\u5b66\u91d1\u7533\u8bf7\u8868";
    private static final String FORMULA_ANCHOR = "\u672c\u671f\u8bfe\u7a0b\u6210\u7ee9";
    private static final String HEADER_TOTAL = "\u603b\u5206";
    private static final String ROW_INTEL = "\u4e13\u4e1a\u6280\u80fd\u4e0e\u521b\u65b0\u521b\u4e1a";
    private static final String ROW_MORAL = "\u5fb7\u80b2\u5f97\u5206";
    private static final String ROW_SPORT = "\u4f53\u80b2\u5f97\u5206";
    private static final String ROW_ART = "\u7f8e\u80b2\u5f97\u5206";
    private static final String ROW_LABOR = "\u52b3\u80b2\u5f97\u5206";
    private static final String ROW_TOTAL = "\u7efc\u5408\u6d4b\u8bc4\u5206\u6570";
    private static final String COLOR_BLACK = "000000";
    private static final int ROW_INDEX_INTEL = 12;
    private static final int ROW_INDEX_MORAL = 13;
    private static final int ROW_INDEX_SPORT = 14;
    private static final int ROW_INDEX_ART = 15;
    private static final int ROW_INDEX_LABOR = 16;
    private static final int ROW_INDEX_TOTAL = 17;
    private static final int DEFAULT_TOTAL_COLUMN_INDEX = 15;

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
                throw new BizException(50001, "\u6a21\u677f\u683c\u5f0f\u4e0d\u6b63\u786e\uff1a\u672a\u627e\u5230\u8868\u683c\u7ed3\u6784");
            }
            XWPFTable table = doc.getTables().get(0);
            if (table.getNumberOfRows() < 18) {
                throw new BizException(50001, "\u6a21\u677f\u683c\u5f0f\u4e0d\u6b63\u786e\uff1a\u5173\u952e\u884c\u6570\u91cf\u4e0d\u8db3");
            }

            int fillFontSize = resolveFillFontSize(courses, activities);

            fillBaseInfo(table, student, fillFontSize);
            fillCourseSummary(table, courses, fillFontSize);
            fillCourses(table, courses, fillFontSize);
            fillCourseFormula(table, courses);
            fillActivities(table, activities, fillFontSize);
            fillRightScoreColumn(table, score, fillFontSize);
            fillFinalScore(table, score, fillFontSize);
            removeNoteSection(doc);

            doc.write(out);
            return out.toByteArray();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(50001, "\u6a21\u677f\u586b\u5145\u5931\u8d25: " + ex.getMessage());
        }
    }

    private InputStream loadTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("report-templates/" + templateName);
            if (!resource.exists()) {
                throw new BizException(50001, "\u6a21\u677f\u4e0d\u5b58\u5728: " + templateName);
            }
            return resource.getInputStream();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(50001, "\u8bfb\u53d6\u6a21\u677f\u5931\u8d25: " + ex.getMessage());
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
        setCellText(table, 0, 5, str(student == null ? null : student.getAccountNo()), fillFontSize);
        setCellText(table, 1, 1, "\u672c\u79d1", fillFontSize);
        setCellText(table, 1, 3, str(student == null ? null : student.getClassName()), fillFontSize);

        String phone = str(student == null ? null : student.getPhone()).trim();
        setCellText(table, 1, 5, phone.isEmpty() ? "-" : phone, fillFontSize);

        // 闂佸搫鎳樼紓姘跺磻濞戞﹩鍤楁俊銈傚亾闁搞劌閰ｅ畷锝夘敂閸愵亞顔旈梺浼欑稻閻熝囨偟椤愶箑绠抽柟鐑樻尵閸ㄨ偐绱掑☉娆愬珪缂佽鲸绻冪粙澶婎吋閸涱喛鍚┑顔界缚閸婃洟藝閳哄懏鈷旈柛鏇ㄥ亜椤綁鏌涢幒鎴烆棡闁哄棛鍠栨俊?        setCellText(table, 2, 0, "\u667a\n\u80b2\n\u5f97\n\u5206", fillFontSize);
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
                "\u672c\u5b66\u671f\u5171\u4fee %s \u95e8\u8bfe\uff0c\u5176\u4e2d\u5fc5\u4fee\u8bfe %s \u95e8\uff0c\u9009\u4fee\u8bfe %s \u95e8\uff0c\u91cd\u4fee %s \u95e8\uff0c\u518d\u4fee %s \u95e8\uff1b\u4e0d\u53ca\u683c %s \u95e8\u3002",
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

    private void fillCourseFormula(XWPFTable table, List<CourseItemEntity> courses) {
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
        XWPFParagraph formulaParagraph = null;
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            if (normalizeForMatch(paragraph.getText()).contains(normalizeForMatch(FORMULA_ANCHOR))) {
                formulaParagraph = paragraph;
                break;
            }
        }
        if (formulaParagraph == null) {
            throw new BizException(50001, "\u6a21\u677f\u516c\u5f0f\u533a\u5360\u4f4d\u672a\u5339\u914d");
        }

        if (!replaceFormulaScorePlaceholder(formulaParagraph, formatScore2(avg))) {
            throw new BizException(50001, "\u6a21\u677f\u516c\u5f0f\u533a\u5360\u4f4d\u672a\u5339\u914d");
        }
    }

    private boolean replaceFormulaScorePlaceholder(XWPFParagraph paragraph, String score) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) {
            return false;
        }

        int leftParenRun = -1;
        int rightParenRun = -1;
        for (int i = 0; i < runs.size(); i++) {
            String text = getRunText(runs.get(i));
            if (leftParenRun < 0 && text.contains("(")) {
                leftParenRun = i;
            }
            if (leftParenRun >= 0 && text.contains(")")) {
                rightParenRun = i;
                break;
            }
        }
        if (leftParenRun < 0 || rightParenRun < 0 || rightParenRun - leftParenRun < 1) {
            return false;
        }

        int placeholderRun = -1;
        for (int i = leftParenRun + 1; i < rightParenRun; i++) {
            String text = getRunText(runs.get(i));
            if (text.trim().isEmpty()) {
                placeholderRun = i;
                break;
            }
        }
        if (placeholderRun < 0) {
            placeholderRun = leftParenRun + 1;
        }

        String normalized = str(score).trim();
        // Keep the bracket group compact and adaptive: ( score )
        XWPFRun leftRun = runs.get(leftParenRun);
        overwriteRunText(leftRun, "(");
        leftRun.setColor(COLOR_BLACK);

        XWPFRun rightRun = runs.get(rightParenRun);
        overwriteRunText(rightRun, ")");
        rightRun.setColor(COLOR_BLACK);

        for (int i = leftParenRun + 1; i < rightParenRun; i++) {
            XWPFRun run = runs.get(i);
            if (i == placeholderRun) {
                overwriteRunText(run, " " + normalized + " ");
                run.setColor(COLOR_BLACK);
            } else {
                overwriteRunText(run, "");
            }
        }
        return true;
    }

    private void fillActivities(XWPFTable table, List<ActivityItemEntity> activities, int fillFontSize) {
        setCellText(table, 12, 2, joinActivities(activities, "INTEL_PRO_INNOV"), fillFontSize, ParagraphAlignment.LEFT);
        setCellText(table, 13, 2, joinActivities(activities, "MORAL"), fillFontSize, ParagraphAlignment.LEFT);
        setCellText(table, 14, 2, joinActivities(activities, "SPORT_ACTIVITY"), fillFontSize, ParagraphAlignment.LEFT);
        setCellText(table, 15, 2, joinActivities(activities, "ART"), fillFontSize, ParagraphAlignment.LEFT);
        setCellText(table, 16, 2, joinActivities(activities, "LABOR"), fillFontSize, ParagraphAlignment.LEFT);
    }

    private void fillRightScoreColumn(XWPFTable table, Map<String, Object> score, int fillFontSize) {
        int totalCol = findGridColumnByCellText(table, HEADER_TOTAL);
        if (totalCol < 0) {
            totalCol = DEFAULT_TOTAL_COLUMN_INDEX;
        }

        Map<String, String> valueByRow = new LinkedHashMap<>();
        valueByRow.put(ROW_INTEL, formatScore2(readWeightedScore(score, "intelScore", "intelRaw", WEIGHT_INTEL)));
        valueByRow.put(ROW_MORAL, formatScore2(readWeightedScore(score, "moralScore", "moralRaw", WEIGHT_MORAL)));
        valueByRow.put(ROW_SPORT, formatScore2(readWeightedScore(score, "sportScore", "sportRaw", WEIGHT_SPORT)));
        valueByRow.put(ROW_ART, formatScore2(readWeightedScore(score, "artScore", "artRaw", WEIGHT_ART)));
        valueByRow.put(ROW_LABOR, formatScore2(readWeightedScore(score, "laborScore", "laborRaw", WEIGHT_LABOR)));
        valueByRow.put(ROW_TOTAL, formatScore2(readDecimal(score, "totalScore")));

        Map<String, Integer> fallbackRowIndexes = new LinkedHashMap<>();
        fallbackRowIndexes.put(ROW_INTEL, ROW_INDEX_INTEL);
        fallbackRowIndexes.put(ROW_MORAL, ROW_INDEX_MORAL);
        fallbackRowIndexes.put(ROW_SPORT, ROW_INDEX_SPORT);
        fallbackRowIndexes.put(ROW_ART, ROW_INDEX_ART);
        fallbackRowIndexes.put(ROW_LABOR, ROW_INDEX_LABOR);
        fallbackRowIndexes.put(ROW_TOTAL, ROW_INDEX_TOTAL);

        for (Map.Entry<String, String> entry : valueByRow.entrySet()) {
            int rowIndex = resolveRowIndex(table, entry.getKey(), fallbackRowIndexes.getOrDefault(entry.getKey(), -1));
            if (rowIndex < 0) {
                throw new BizException(50001, "\u6a21\u677f\u603b\u5206\u5217\u5b9a\u4f4d\u5931\u8d25");
            }
            setCellTextByGridColumn(table, rowIndex, totalCol, entry.getValue(), fillFontSize, ParagraphAlignment.CENTER);
        }
    }

    private void fillFinalScore(XWPFTable table, Map<String, Object> score, int fillFontSize) {
        String total = formatScore2(readDecimal(score, "totalScore"));
        setCellText(table, 17, 1, total, fillFontSize);
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

        // Keep title paragraph, remove note paragraphs.
        for (int i = doc.getBodyElements().size() - 1; i > firstTableIndex; i--) {
            IBodyElement element = doc.getBodyElements().get(i);
            if (element.getElementType() != BodyElementType.PARAGRAPH) {
                doc.removeBodyElement(i);
                continue;
            }
            XWPFParagraph paragraph = (XWPFParagraph) element;
            String text = str(paragraph.getText()).trim();
            if (text.contains(TITLE_KEYWORD)) {
                continue;
            }
            doc.removeBodyElement(i);
        }
    }

    private int findGridColumnByCellText(XWPFTable table, String text) {
        String target = normalizeForMatch(text);
        for (int rowIndex = 0; rowIndex < table.getNumberOfRows(); rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            int gridStart = 0;
            for (XWPFTableCell cell : row.getTableCells()) {
                int span = getGridSpan(cell);
                String normalized = normalizeForMatch(extractCellText(cell));
                if (normalized.contains(target)) {
                    return gridStart;
                }
                gridStart += span;
            }
        }
        return -1;
    }

    private int findRowByLabel(XWPFTable table, String rowLabel) {
        String target = normalizeForMatch(rowLabel);
        for (int rowIndex = 0; rowIndex < table.getNumberOfRows(); rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (XWPFTableCell cell : row.getTableCells()) {
                sb.append(extractCellText(cell));
            }
            if (normalizeForMatch(sb.toString()).contains(target)) {
                return rowIndex;
            }
        }
        return -1;
    }

    private int resolveRowIndex(XWPFTable table, String rowLabel, int fallbackRowIndex) {
        int rowIndex = findRowByLabel(table, rowLabel);
        if (rowIndex >= 0) {
            return rowIndex;
        }
        if (fallbackRowIndex >= 0 && fallbackRowIndex < table.getNumberOfRows()) {
            return fallbackRowIndex;
        }
        return -1;
    }

    private int getGridSpan(XWPFTableCell cell) {
        if (cell == null || cell.getCTTc() == null || cell.getCTTc().getTcPr() == null || !cell.getCTTc().getTcPr().isSetGridSpan()) {
            return 1;
        }
        return cell.getCTTc().getTcPr().getGridSpan().getVal().intValue();
    }

    private String extractCellText(XWPFTableCell cell) {
        if (cell == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : cell.getParagraphs()) {
            sb.append(str(p.getText()));
        }
        return sb.toString();
    }

    private String normalizeForMatch(String raw) {
        String value = str(raw);
        return value
                .replaceAll("[\\s\\u3000\\r\\n\\t]+", "")
                .replace(":", "")
                .replace("(", "")
                .replace(")", "")
                .replace(",", "")
                .replace("\uFF1A", "")
                .replace("\uFF08", "")
                .replace("\uFF09", "")
                .replace("\u3010", "")
                .replace("\u3011", "")
                .replace("\uFF0C", "");
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
            item.append(index).append("\u3001").append(str(activity.getTitle()).trim());

            String desc = str(activity.getDescription()).trim();
            if (!desc.isEmpty()) {
                item.append("\uff1a").append(desc);
            }
            item.append("\uff08").append(formatScore2(value)).append("\u5206\uff09");
            parts.add(item.toString());
            index++;
        }
        if (parts.isEmpty()) {
            return "-";
        }
        return String.join("\uff1b", parts);
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
        setCellText(cell, value, fillFontSize, alignment);
    }

    private void setCellTextByGridColumn(XWPFTable table, int rowIndex, int gridColIndex, String value, int fillFontSize, ParagraphAlignment alignment) {
        XWPFTableRow row = table.getRow(rowIndex);
        if (row == null) {
            throw new BizException(50001, "\u6a21\u677f\u603b\u5206\u5217\u5b9a\u4f4d\u5931\u8d25");
        }
        XWPFTableCell cell = findCellByGridColumn(row, gridColIndex);
        if (cell == null) {
            throw new BizException(50001, "\u6a21\u677f\u603b\u5206\u5217\u5b9a\u4f4d\u5931\u8d25");
        }
        setCellText(cell, value, fillFontSize, alignment);
    }

    private XWPFTableCell findCellByGridColumn(XWPFTableRow row, int gridColIndex) {
        int start = 0;
        for (XWPFTableCell cell : row.getTableCells()) {
            int span = getGridSpan(cell);
            int end = start + span - 1;
            if (gridColIndex >= start && gridColIndex <= end) {
                return cell;
            }
            start += span;
        }
        return null;
    }

    private void setCellText(XWPFTableCell cell, String value, int fillFontSize, ParagraphAlignment alignment) {
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

    private XWPFTableCell requireCell(XWPFTable table, int rowIndex, int colIndex) {
        XWPFTableRow row = table.getRow(rowIndex);
        if (row == null) {
            throw new BizException(50001, "\u6a21\u677f\u5b57\u6bb5\u5b9a\u4f4d\u5931\u8d25: row=" + rowIndex);
        }
        XWPFTableCell cell = row.getCell(colIndex);
        if (cell == null) {
            throw new BizException(50001, "\u6a21\u677f\u5b57\u6bb5\u5b9a\u4f4d\u5931\u8d25: row=" + rowIndex + ", col=" + colIndex);
        }
        return cell;
    }

    private void initParagraph(XWPFParagraph paragraph, ParagraphAlignment alignment) {
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);
        paragraph.setSpacingBetween(1.0);
        paragraph.setIndentationLeft(0);
        paragraph.setIndentationFirstLine(0);
        if (paragraph.getCTP() != null && paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetInd()) {
            CTInd ind = paragraph.getCTP().getPPr().getInd();
            if (ind.isSetLeftChars()) {
                ind.unsetLeftChars();
            }
            if (ind.isSetFirstLineChars()) {
                ind.unsetFirstLineChars();
            }
            if (ind.isSetRightChars()) {
                ind.unsetRightChars();
            }
            if (ind.isSetHangingChars()) {
                ind.unsetHangingChars();
            }
        }
        paragraph.setAlignment(alignment == null ? ParagraphAlignment.CENTER : alignment);
    }

    private void writeTextWithLineBreaks(XWPFRun run, String text) {
        String[] lines = text.split("\\n", -1);
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
        run.setColor(COLOR_BLACK);
        if (resolvedStyle.underline != null) {
            run.setUnderline(resolvedStyle.underline);
        }

        int fontSize = fillFontSize > 0 ? fillFontSize : (resolvedStyle.fontSize > 0 ? resolvedStyle.fontSize : 10);
        run.setFontSize(fontSize);

        CTRPr rPr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        CTFonts rFonts = rPr.sizeOfRFontsArray() > 0 ? rPr.getRFontsArray(0) : rPr.addNewRFonts();
        rFonts.setEastAsia("\u5b8b\u4f53");
        rFonts.setAscii("Times New Roman");
        rFonts.setHAnsi("Times New Roman");
        rFonts.setCs("Times New Roman");
    }

    private String getRunText(XWPFRun run) {
        if (run == null) {
            return "";
        }
        String text = run.getText(0);
        return text == null ? "" : text;
    }

    private void overwriteRunText(XWPFRun run, String text) {
        if (run == null) {
            return;
        }
        if (run.getCTR().sizeOfTArray() == 0) {
            run.setText(text == null ? "" : text);
            return;
        }
        run.setText(text == null ? "" : text, 0);
        for (int i = run.getCTR().sizeOfTArray() - 1; i > 0; i--) {
            run.getCTR().removeT(i);
        }
    }

    private String normalizeGender(String raw) {
        String value = str(raw).trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) {
            return "";
        }
        if ("M".equals(value) || "MALE".equals(value) || "\u7537".equals(raw)) {
            return "\u7537";
        }
        if ("F".equals(value) || "FEMALE".equals(value) || "\u5973".equals(raw)) {
            return "\u5973";
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
        BigDecimal normalized = safe(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return normalized.toPlainString();
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
        throw new BizException(50001, "\u5b66\u751f\u4fe1\u606f\u7c7b\u578b\u5f02\u5e38: " + obj.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object obj) {
        if (obj == null) {
            return new ArrayList<>();
        }
        if (obj instanceof List) {
            return (List<T>) obj;
        }
        throw new BizException(50001, "\u6a21\u677f\u586b\u5145\u6570\u636e\u7c7b\u578b\u5f02\u5e38: " + obj.getClass().getName());
    }

    private static class RunStyle {
        int fontSize;
        boolean bold;
        boolean italic;
        String color;
        UnderlinePatterns underline;
    }
}

