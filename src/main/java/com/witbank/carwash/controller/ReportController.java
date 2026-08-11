package com.witbank.carwash.controller;

// ── OpenPDF (lowagie) — explicit imports to avoid clash with Apache POI ──────
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

// ── Apache POI — explicit imports to avoid clash with OpenPDF ─────────────────
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// ── Application ───────────────────────────────────────────────────────────────
import com.witbank.carwash.model.Booking;
import com.witbank.carwash.model.NotificationLog;
import com.witbank.carwash.model.Staff;
import com.witbank.carwash.repository.NotificationLogRepository;
import com.witbank.carwash.service.BookingService;
import com.witbank.carwash.service.CustomerService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/report")
public class ReportController {

    @Autowired private BookingService           bookingService;
    @Autowired private CustomerService          customerService;
    @Autowired private NotificationLogRepository notifRepo;

    private boolean notAdmin(HttpSession s) {
        Staff st = (Staff) s.getAttribute("staffUser");
        return st == null || !"ADMIN".equalsIgnoreCase(st.getRole());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PDF REPORT
    // ═══════════════════════════════════════════════════════════════════════════
    @GetMapping("/pdf")
    public void exportPdf(HttpServletResponse response, HttpSession session) throws Exception {
        if (notAdmin(session)) { response.sendRedirect("/staff/login"); return; }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"WitbankElite_Report_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf\"");

        List<Booking>      bookings     = bookingService.getAllBookings();
        double             totalRev     = bookingService.getTotalRevenue();
        Map<String,Double> revByService = bookingService.getRevenueByService();
        Map<String,Long>   cntByService = bookingService.getBookingCountByService();
        double             avgRating    = customerService.getAverageRating();

        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        PdfWriter.getInstance(doc, response.getOutputStream());
        doc.open();

        // ── Colours ───────────────────────────────────────────────────────────
        Color bgDark  = new Color(11,  17,  32);
        Color bgCard  = new Color(20,  28,  50);
        Color cyan    = new Color(0,  210, 255);
        Color dimText = new Color(140, 160, 190);
        Color white   = Color.WHITE;
        Color light   = new Color(180, 180, 180);

        // ── Fonts (all fully-qualified to avoid conflict with POI Font) ────────
        com.lowagie.text.Font fTitle  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, cyan);
        com.lowagie.text.Font fHead   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, white);
        com.lowagie.text.Font fSub    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, white);
        com.lowagie.text.Font fNormal = FontFactory.getFont(FontFactory.HELVETICA,       9, white);
        com.lowagie.text.Font fSmall  = FontFactory.getFont(FontFactory.HELVETICA,       8, light);
        com.lowagie.text.Font fDim    = FontFactory.getFont(FontFactory.HELVETICA,       9, dimText);

        // ── Title block ───────────────────────────────────────────────────────
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);

        PdfPCell tc = new PdfPCell(new Phrase("WITBANK ELITE CAR WASH", fTitle));
        tc.setBackgroundColor(bgDark); tc.setBorder(0); tc.setPadding(10);
        titleTable.addCell(tc);

        PdfPCell sc = new PdfPCell(new Phrase(
                "Revenue & Booking Report  ·  Generated: "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), fDim));
        sc.setBackgroundColor(bgDark); sc.setBorder(0);
        sc.setPaddingLeft(10); sc.setPaddingBottom(12);
        titleTable.addCell(sc);
        doc.add(titleTable);
        doc.add(Chunk.NEWLINE);

        // ── KPI cards ─────────────────────────────────────────────────────────
        PdfPTable kpi = new PdfPTable(3);
        kpi.setWidthPercentage(100);
        kpi.setSpacingAfter(18);
        addPdfKpi(kpi, "Total Revenue",   String.format("R %.2f", totalRev),         bgCard, cyan, fDim);
        addPdfKpi(kpi, "Total Bookings",  String.valueOf(bookings.size()),            bgCard, cyan, fDim);
        addPdfKpi(kpi, "Avg Rating",      String.format("%.1f / 5 ★", avgRating),    bgCard, cyan, fDim);
        doc.add(kpi);

        // ── Revenue by service ────────────────────────────────────────────────
        doc.add(new Paragraph("Revenue by Service", fSub));
        doc.add(Chunk.NEWLINE);
        PdfPTable svcTbl = new PdfPTable(3);
        svcTbl.setWidthPercentage(100);
        svcTbl.setWidths(new float[]{4f, 2f, 2f});
        svcTbl.setSpacingAfter(18);
        addPdfHeader(svcTbl, bgCard, fHead, "Service", "Bookings", "Revenue");
        for (Map.Entry<String,Double> e : revByService.entrySet()) {
            addPdfRow(svcTbl, bgDark, fSmall,
                    e.getKey(),
                    String.valueOf(cntByService.getOrDefault(e.getKey(), 0L)),
                    String.format("R %.2f", e.getValue()));
        }
        doc.add(svcTbl);

        // ── Booking details ───────────────────────────────────────────────────
        doc.add(new Paragraph("Booking Details", fSub));
        doc.add(Chunk.NEWLINE);
        PdfPTable bTbl = new PdfPTable(6);
        bTbl.setWidthPercentage(100);
        bTbl.setWidths(new float[]{1.2f, 2.5f, 2f, 2f, 1.5f, 1.5f});
        addPdfHeader(bTbl, bgCard, fHead, "Ref", "Customer", "Service", "Date", "Amount", "Status");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        for (Booking b : bookings) {
            addPdfRow(bTbl, bgDark, fSmall,
                    "#WE-" + b.getId(),
                    nvl(b.getCustomerName()),
                    nvl(b.getServiceType()),
                    b.getBookingTime() != null ? b.getBookingTime().format(dtf) : "—",
                    String.format("R %.2f", b.getPrice()),
                    nvl(b.getStatus()));
        }
        doc.add(bTbl);
        doc.close();
    }

    private void addPdfKpi(PdfPTable t, String label, String value,
                            Color bg, Color valColor, com.lowagie.text.Font labelFont) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorderColor(new Color(40, 60, 90));
        c.setPadding(14);
        c.addElement(new Paragraph(label, labelFont));
        c.addElement(new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, valColor)));
        t.addCell(c);
    }

    private void addPdfHeader(PdfPTable t, Color bg, com.lowagie.text.Font f, String... cols) {
        for (String col : cols) {
            PdfPCell c = new PdfPCell(new Phrase(col, f));
            c.setBackgroundColor(bg);
            c.setBorderColor(new Color(40, 60, 90));
            c.setPadding(8);
            t.addCell(c);
        }
    }

    private void addPdfRow(PdfPTable t, Color bg, com.lowagie.text.Font f, String... vals) {
        for (String val : vals) {
            PdfPCell c = new PdfPCell(new Phrase(val != null ? val : "—", f));
            c.setBackgroundColor(bg);
            c.setBorderColor(new Color(30, 45, 70));
            c.setPadding(7);
            t.addCell(c);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXCEL REPORT
    // ═══════════════════════════════════════════════════════════════════════════
    @GetMapping("/excel")
    public void exportExcel(HttpServletResponse response, HttpSession session) throws Exception {
        if (notAdmin(session)) { response.sendRedirect("/staff/login"); return; }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"WitbankElite_Report_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx\"");

        List<Booking>      bookings     = bookingService.getAllBookings();
        Map<String,Double> revByService = bookingService.getRevenueByService();
        Map<String,Long>   cntByService = bookingService.getBookingCountByService();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ── Styles ────────────────────────────────────────────────────────
            CellStyle hdrStyle = wb.createCellStyle();
            hdrStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            hdrStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hdrStyle.setBorderBottom(BorderStyle.THIN);
            XSSFFont hdrFont = wb.createFont(); // XSSFFont — no ambiguity
            hdrFont.setBold(true);
            hdrFont.setColor(IndexedColors.WHITE.getIndex());
            hdrStyle.setFont(hdrFont);

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle moneyStyle = wb.createCellStyle();
            DataFormat dfmt = wb.createDataFormat();
            moneyStyle.setDataFormat(dfmt.getFormat("\"R\"#,##0.00"));

            CellStyle boldStyle = wb.createCellStyle();
            XSSFFont bFont = wb.createFont();
            bFont.setBold(true);
            boldStyle.setFont(bFont);

            // ── Sheet 1 — Summary ──────────────────────────────────────────────
            Sheet sum = wb.createSheet("Summary");
            Row r0 = sum.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("WITBANK ELITE CAR WASH — Revenue Report");
            c0.setCellStyle(boldStyle);

            sum.createRow(1).createCell(0).setCellValue(
                    "Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

            Row kpiHdr = sum.createRow(3);
            addXlCell(kpiHdr, 0, "Metric",  hdrStyle);
            addXlCell(kpiHdr, 1, "Value",   hdrStyle);

            Row r4 = sum.createRow(4);
            r4.createCell(0).setCellValue("Total Revenue");
            Cell rv = r4.createCell(1); rv.setCellValue(bookingService.getTotalRevenue()); rv.setCellStyle(moneyStyle);

            Row r5 = sum.createRow(5);
            r5.createCell(0).setCellValue("Total Bookings");
            r5.createCell(1).setCellValue(bookings.size());

            Row r6 = sum.createRow(6);
            r6.createCell(0).setCellValue("Average Rating");
            r6.createCell(1).setCellValue(String.format("%.1f / 5", customerService.getAverageRating()));

            sum.setColumnWidth(0, 6000);
            sum.setColumnWidth(1, 5000);

            // Revenue by service table
            Row svcHdr = sum.createRow(8);
            addXlCell(svcHdr, 0, "Service",  hdrStyle);
            addXlCell(svcHdr, 1, "Bookings", hdrStyle);
            addXlCell(svcHdr, 2, "Revenue",  hdrStyle);
            int ri = 9;
            for (Map.Entry<String,Double> e : revByService.entrySet()) {
                Row row = sum.createRow(ri++);
                row.createCell(0).setCellValue(e.getKey());
                row.createCell(1).setCellValue(cntByService.getOrDefault(e.getKey(), 0L));
                Cell mc = row.createCell(2); mc.setCellValue(e.getValue()); mc.setCellStyle(moneyStyle);
            }
            sum.setColumnWidth(2, 4000);

            // ── Sheet 2 — All Bookings ──────────────────────────────────────────
            Sheet bks = wb.createSheet("All Bookings");
            String[] bCols = {"Ref","Customer","Email","Cellphone","Service",
                              "Date & Time","Amount","Status","Payment","Verified"};
            Row bHdr = bks.createRow(0);
            for (int i = 0; i < bCols.length; i++) addXlCell(bHdr, i, bCols[i], hdrStyle);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            int bri = 1;
            for (Booking b : bookings) {
                Row row = bks.createRow(bri);
                CellStyle rowStyle = bri % 2 == 0 ? altStyle : null;
                setCell(row, 0, "#WE-" + b.getId(),     rowStyle);
                setCell(row, 1, nvl(b.getCustomerName()), rowStyle);
                setCell(row, 2, nvl(b.getEmail()),       rowStyle);
                setCell(row, 3, nvl(b.getCellphone()),   rowStyle);
                setCell(row, 4, nvl(b.getServiceType()), rowStyle);
                setCell(row, 5, b.getBookingTime() != null ? b.getBookingTime().format(dtf) : "—", rowStyle);
                Cell mc = row.createCell(6); mc.setCellValue(b.getPrice()); mc.setCellStyle(moneyStyle);
                setCell(row, 7, nvl(b.getStatus()),       rowStyle);
                setCell(row, 8, nvl(b.getPaymentStatus()), rowStyle);
                setCell(row, 9, b.isVerified() ? "Yes" : "No", rowStyle);
                bri++;
            }
            for (int i = 0; i < bCols.length; i++) bks.autoSizeColumn(i);

            // ── Sheet 3 — Notifications ──────────────────────────────────────────
            Sheet notifSheet = wb.createSheet("Notifications");
            String[] nCols = {"ID","Recipient","Type","Message","Timestamp"};
            Row nHdr = notifSheet.createRow(0);
            for (int i = 0; i < nCols.length; i++) addXlCell(nHdr, i, nCols[i], hdrStyle);
            int nri = 1;
            for (NotificationLog log : notifRepo.findAll()) {
                Row row = notifSheet.createRow(nri++);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(nvl(log.getRecipient()));
                row.createCell(2).setCellValue(nvl(log.getType()));
                row.createCell(3).setCellValue(nvl(log.getMessage()));
                row.createCell(4).setCellValue(log.getTimestamp() != null ? log.getTimestamp().toString() : "—");
            }
            for (int i = 0; i < nCols.length; i++) notifSheet.autoSizeColumn(i);

            wb.write(response.getOutputStream());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void addXlCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        if (style != null) c.setCellStyle(style);
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "—");
        if (style != null) c.setCellStyle(style);
    }

    private String nvl(String s) { return s != null ? s : "—"; }
}
