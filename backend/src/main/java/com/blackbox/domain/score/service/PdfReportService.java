package com.blackbox.domain.score.service;

import com.blackbox.domain.score.dto.ScoreDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private static final Color C_DARK  = new Color(0x0F, 0x1F, 0x2E);
    private static final Color C_TEAL  = new Color(0x00, 0x96, 0x88);
    private static final Color C_LIGHT = new Color(0xF0, 0xF4, 0xF8);
    private static final Color C_WHITE = Color.WHITE;

    public byte[] generate(Long projectId, ScoreDto.ProjectScoreReport report) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            Font titleFont  = new Font(bf, 20, Font.BOLD, C_DARK);
            Font headerFont = new Font(bf, 11, Font.BOLD, C_WHITE);
            Font bodyFont   = new Font(bf, 10, Font.NORMAL, C_DARK);
            Font subFont    = new Font(bf, 9,  Font.NORMAL, new Color(0x78, 0x90, 0x9C));

            // 제목
            Paragraph title = new Paragraph("Team Blackbox - Contribution Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            doc.add(title);

            Paragraph sub = new Paragraph(
                    "Project ID: " + projectId + "   |   Generated: " + FMT.format(report.getCalculatedAt()),
                    subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            doc.add(sub);

            // 요약
            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(100);
            summary.setSpacingAfter(20);
            addSummaryCell(summary, "Team Average Score", String.format("%.1f", report.getTeamAverage()), headerFont, bodyFont);
            addSummaryCell(summary, "Member Count", String.valueOf(report.getMembers().size()), headerFont, bodyFont);
            doc.add(summary);

            // 멤버별 점수 테이블
            PdfPTable table = new PdfPTable(new float[]{3, 2, 2, 2, 2, 2, 1.5f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(30);

            for (String h : new String[]{"Name", "Task", "Meeting", "File", "Total", "Normalized", "Grade"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(C_DARK);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                cell.setBorderColor(C_TEAL);
                table.addCell(cell);
            }

            boolean alt = false;
            for (ScoreDto.MemberScore m : report.getMembers()) {
                Color bg = alt ? C_LIGHT : C_WHITE;
                addCell(table, m.getUserName(),                                   bodyFont, bg, Element.ALIGN_LEFT);
                addCell(table, String.format("%.1f", m.getTaskScore()),           bodyFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.format("%.1f", m.getMeetingScore()),        bodyFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.format("%.1f", m.getFileScore()),           bodyFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.format("%.1f", m.getTotalScore()),          bodyFont, bg, Element.ALIGN_CENTER);
                addCell(table, String.format("%.1f%%", m.getNormalizedScore()),   bodyFont, bg, Element.ALIGN_CENTER);
                addCell(table, m.getGrade(),                                      bodyFont, bg, Element.ALIGN_CENTER);
                alt = !alt;
            }
            doc.add(table);

            Paragraph legend = new Paragraph(
                    "Grade: A>=120%  B>=100%  C>=80%  D>=60%  F<60%", subFont);
            legend.setAlignment(Element.ALIGN_CENTER);
            doc.add(legend);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addSummaryCell(PdfPTable t, String label, String value, Font lf, Font vf) {
        PdfPCell lc = new PdfPCell(new Phrase(label, lf));
        lc.setBackgroundColor(C_TEAL);
        lc.setPadding(8);
        t.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value, vf));
        vc.setPadding(8);
        t.addCell(vc);
    }

    private void addCell(PdfPTable t, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5);
        cell.setBorderColor(new Color(0xCF, 0xD8, 0xDC));
        t.addCell(cell);
    }
}
