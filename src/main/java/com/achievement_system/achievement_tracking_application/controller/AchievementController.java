package com.achievement_system.achievement_tracking_application.controller;

import com.achievement_system.achievement_tracking_application.model.Achievement;
import com.achievement_system.achievement_tracking_application.repository.AchievementRepository;

// ===== SPRING =====
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// ===== FILE HANDLING =====
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// ===== JAVA UTIL =====
import java.util.*;
import java.util.stream.*;

// ===== APACHE POI (EXCEL ONLY) =====
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Cell;

// ===== ITEXT PDF (IMPORTANT) =====
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;

import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;

@RestController
@RequestMapping("/api/achievements")
@CrossOrigin
public class AchievementController {

    @Autowired
    private AchievementRepository repo;

    // ===== ADD =====
    @PostMapping("/add")
    public Achievement addAchievement(@RequestBody Achievement achievement) {
        return repo.save(achievement);
    }

    // ===== GET USER =====
    @GetMapping("/{username}")
    public List<Achievement> getUserAchievements(@PathVariable String username) {
        return repo.findByUsername(username);
    }

    // ===== GET ALL =====
    @GetMapping("/all")
    public List<Achievement> getAllAchievements() {
        return repo.findAll();
    }

    // ===== GET BY ID (for edit pre-fill) =====
    @GetMapping("/id/{id}")
    public ResponseEntity<?> getById(@PathVariable int id) {
        Optional<Achievement> found = repo.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(found.get());
    }

    // ===== DELETE =====
    @DeleteMapping("/delete/{id}")
    public void deleteAchievement(@PathVariable int id) {
        repo.deleteById(id);
    }

    // ===== FILE UPLOAD (NEW) =====
    @PostMapping("/addWithFile")
    public ResponseEntity<?> addWithFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("year") String year,
            @RequestParam("semester") String semester,
            @RequestParam(value = "eventLevel", required = false) String eventLevel,
            @RequestParam(value = "skills", required = false) String skills,
            @RequestParam("status") String status
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get("uploads/" + fileName);

            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            Achievement a = new Achievement();
            a.setUsername(username);
            a.setTitle(title);
            a.setCategory(category);
            a.setYear(year);
            a.setSemester(semester);
            a.setStatus(status);
            a.setFileName(fileName);
            a.setEventLevel(eventLevel);
            a.setSkills(skills);
            a.setApproved("Pending");

            repo.save(a);

            return ResponseEntity.ok("Saved");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Upload failed");
        }
    }

    // ===== UPDATE ACHIEVEMENT (STUDENT) =====
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAchievement(
            @PathVariable int id,
            @RequestParam("title") String title,
            @RequestParam("category") String category,
            @RequestParam("year") String year,
            @RequestParam("semester") String semester,
            @RequestParam(value = "eventLevel", required = false) String eventLevel,
            @RequestParam(value = "skills", required = false) String skills,
            @RequestParam("status") String status,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {
            Optional<Achievement> optional = repo.findById(id);

            if (optional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Achievement a = optional.get();

            // Update fields
            a.setTitle(title);
            a.setCategory(category);
            a.setYear(year);
            a.setSemester(semester);
            a.setStatus(status);   // score auto-updates via setStatus()
            a.setEventLevel(eventLevel);
            a.setSkills(skills);

            // Only replace file if a new one was uploaded
            if (file != null && !file.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path path = Paths.get("uploads/" + fileName);
                Files.createDirectories(path.getParent());
                Files.write(path, file.getBytes());
                a.setFileName(fileName);
            }

            repo.save(a);

            return ResponseEntity.ok("Updated");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Update failed");
        }
    }

    // ===== PERFORMANCE SCORE =====
    @GetMapping("/performance/{username}")
    public ResponseEntity<?> getPerformance(@PathVariable String username) {

        List<Achievement> list = repo.findByUsername(username);

        int score = 0;

        for (Achievement a : list) {

            String status = (a.getStatus() == null)
                    ? ""
                    : a.getStatus().toLowerCase();

            if (status.contains("winner")) score += 3;
            else if (status.contains("runner")) score += 2;
            else score += 1;
        }

        return ResponseEntity.ok(Map.of(
                "score", score,
                "totalEvents", list.size()
        ));
    }

    // ===== RANKING SYSTEM =====
    @GetMapping("/ranking/{username}")
    public ResponseEntity<?> getRanking(@PathVariable String username) {

        List<Achievement> all = repo.findAll();

        Map<String, Integer> scores = new HashMap<>();

        for (Achievement a : all) {

            String user = a.getUsername();
            String status = (a.getStatus() == null)
                    ? ""
                    : a.getStatus().toLowerCase();

            int points = 1;
            if (status.contains("winner")) points = 3;
            else if (status.contains("runner")) points = 2;

            scores.put(user, scores.getOrDefault(user, 0) + points);
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int rank = 1;
        for (Map.Entry<String, Integer> entry : sorted) {
            if (entry.getKey().equals(username)) break;
            rank++;
        }

        return ResponseEntity.ok(Map.of(
                "rank", rank,
                "totalUsers", scores.size()
        ));
    }

    // ===== RECOMMENDATION =====
    @GetMapping("/recommendation/{username}")
    public ResponseEntity<?> getRecommendation(@PathVariable String username) {

        List<Achievement> list = repo.findByUsername(username);

        Map<String, Integer> categoryCount = new HashMap<>();
        categoryCount.put("technical", 0);
        categoryCount.put("sports", 0);
        categoryCount.put("cultural", 0);

        for (Achievement a : list) {
            String cat = a.getCategory().toLowerCase();
            categoryCount.put(cat, categoryCount.get(cat) + 1);
        }

        int min = Collections.min(categoryCount.values());

        List<String> weakAreas = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            if (entry.getValue() == min) {
                weakAreas.add(entry.getKey());
            }
        }

        return ResponseEntity.ok(Map.of(
                "suggestion", "Improve in " + String.join(", ", weakAreas) + " activities"
        ));
    }

    // ===== AI REPORT =====
    @GetMapping("/ai-report/{username}")
    public ResponseEntity<?> getAiReport(@PathVariable String username) {

        List<Achievement> list = repo.findByUsername(username);

        if (list.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "report", "No achievements found for analysis."
            ));
        }

        int technical = 0, sports = 0, cultural = 0;
        int winner = 0, runner = 0, participation = 0;

        for (Achievement a : list) {

            String cat = (a.getCategory() == null) ? "" : a.getCategory().toLowerCase();
            if (cat.contains("technical")) technical++;
            else if (cat.contains("sports")) sports++;
            else if (cat.contains("cultural")) cultural++;

            String status = (a.getStatus() == null) ? "" : a.getStatus().toLowerCase();
            if (status.contains("winner")) winner++;
            else if (status.contains("runner")) runner++;
            else participation++;
        }

        String strongest = "Technical";
        int max = technical;
        if (sports > max) { strongest = "Sports"; max = sports; }
        if (cultural > max) { strongest = "Cultural"; }

        String weakest = "Technical";
        int min = technical;
        if (sports < min) { weakest = "Sports"; min = sports; }
        if (cultural < min) { weakest = "Cultural"; }

        String report =
                username + " has participated in " + list.size() + " events. " +
                        "Strongest area is " + strongest + ". " +
                        "Needs improvement in " + weakest + " activities. " +
                        "Achievements include " + winner + " wins, " +
                        runner + " runner positions and " +
                        participation + " participations.";

        return ResponseEntity.ok(Map.of("report", report));
    }

    // ===== EXPORT EXCEL (with optional filters: year, semester, category) =====
    @GetMapping("/export")
    public void exportExcel(
            HttpServletResponse response,
            @RequestParam(value = "year",     required = false) String year,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "category", required = false) String category
    ) throws Exception {

        // Start with all records then apply whichever filters were provided
        List<Achievement> all = repo.findAll();

        List<Achievement> list = new ArrayList<>();
        for (Achievement a : all) {

            if (year != null && !year.isEmpty()
                    && !year.equalsIgnoreCase(a.getYear())) continue;

            if (semester != null && !semester.isEmpty()
                    && !semester.equalsIgnoreCase(a.getSemester())) continue;

            if (category != null && !category.isEmpty()
                    && !category.equalsIgnoreCase(a.getCategory())) continue;

            list.add(a);
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Achievements");

        // ── Style: bold header ──
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header = sheet.createRow(0);
        String[] cols = {"Username", "Title", "Category", "Year", "Semester", "Position", "Score"};

        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }

        int rowNum = 1;
        for (Achievement a : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(a.getUsername());
            row.createCell(1).setCellValue(a.getTitle());
            row.createCell(2).setCellValue(a.getCategory());
            row.createCell(3).setCellValue(a.getYear());
            row.createCell(4).setCellValue(a.getSemester());
            row.createCell(5).setCellValue(a.getStatus());
            row.createCell(6).setCellValue(a.getScore());
        }

        // Auto-size all columns after data is written
        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Build a descriptive filename based on active filters
        StringBuilder fileName = new StringBuilder("achievements");
        if (year     != null && !year.isEmpty())     fileName.append("_").append(year);
        if (semester != null && !semester.isEmpty()) fileName.append("_").append(semester);
        if (category != null && !category.isEmpty()) fileName.append("_").append(category);
        fileName.append(".xlsx");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ===== PDF REPORT (with semester-wise breakdown) =====
    @GetMapping("/report/{username}")
    public void generatePdf(@PathVariable String username, HttpServletResponse response) throws Exception {

        List<Achievement> list = repo.findByUsername(username);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + username + "_report.pdf");

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        // ── Fonts ──
        Font titleFont  = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
        Font headFont   = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD);
        Font semFont    = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 11);
        Font totalFont  = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD);
        Font subtotalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD | Font.ITALIC);

        // ── Title ──
        Paragraph title = new Paragraph("STUDENT ACHIEVEMENT REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(6);
        document.add(title);

        // ── Student info ──
        Paragraph namePara = new Paragraph("Student: " + username, normalFont);
        namePara.setAlignment(Element.ALIGN_CENTER);
        namePara.setSpacingAfter(4);
        document.add(namePara);

        Paragraph totalEvents = new Paragraph("Total Achievements: " + list.size(), normalFont);
        totalEvents.setAlignment(Element.ALIGN_CENTER);
        totalEvents.setSpacingAfter(16);
        document.add(totalEvents);

        // ══════════════════════════════════════
        // SECTION 1 — All Achievements Table
        // ══════════════════════════════════════
        Paragraph sec1 = new Paragraph("All Achievements", headFont);
        sec1.setSpacingAfter(8);
        document.add(sec1);

        PdfPTable allTable = new PdfPTable(4);
        allTable.setWidthPercentage(100);
        allTable.setSpacingBefore(4f);
        allTable.setSpacingAfter(18f);
        allTable.setWidths(new float[]{3.5f, 2f, 1.5f, 1f});

        // header row
        for (String h : new String[]{"Title", "Category", "Status", "Score"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(new com.itextpdf.text.BaseColor(41, 98, 255));
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            com.itextpdf.text.Font whiteFont = new com.itextpdf.text.Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,
                    com.itextpdf.text.BaseColor.WHITE);
            cell.setPhrase(new Phrase(h, whiteFont));
            allTable.addCell(cell);
        }

        int grandTotal = 0;
        for (Achievement a : list) {
            allTable.addCell(new Phrase(a.getTitle(),    normalFont));
            allTable.addCell(new Phrase(a.getCategory(), normalFont));
            allTable.addCell(new Phrase(a.getStatus(),   normalFont));
            allTable.addCell(new Phrase(String.valueOf(a.getScore()), normalFont));
            grandTotal += a.getScore();
        }

        document.add(allTable);

        Paragraph gt = new Paragraph("Grand Total Score: " + grandTotal, totalFont);
        gt.setAlignment(Element.ALIGN_RIGHT);
        gt.setSpacingAfter(20);
        document.add(gt);

        // ══════════════════════════════════════
        // SECTION 2 — Semester-wise Breakdown
        // ══════════════════════════════════════
        Paragraph sec2 = new Paragraph("Semester-wise Performance", headFont);
        sec2.setSpacingAfter(8);
        document.add(sec2);

        // Group achievements by semester
        Map<String, List<Achievement>> bySemester = new LinkedHashMap<>();
        for (Achievement a : list) {
            String sem = (a.getSemester() == null || a.getSemester().isEmpty())
                    ? "Unknown" : a.getSemester().toUpperCase();
            bySemester.computeIfAbsent(sem, k -> new ArrayList<>()).add(a);
        }

        // Sort semesters naturally
        List<String> semKeys = new ArrayList<>(bySemester.keySet());
        Collections.sort(semKeys);

        for (String sem : semKeys) {
            List<Achievement> semList = bySemester.get(sem);

            // Semester heading
            Paragraph semHeading = new Paragraph("  " + sem, semFont);
            semHeading.setSpacingBefore(10f);
            semHeading.setSpacingAfter(4f);
            document.add(semHeading);

            // Semester table
            PdfPTable semTable = new PdfPTable(4);
            semTable.setWidthPercentage(100);
            semTable.setSpacingBefore(2f);
            semTable.setSpacingAfter(4f);
            semTable.setWidths(new float[]{3.5f, 2f, 1.5f, 1f});

            // column headers
            com.itextpdf.text.BaseColor headerBg = new com.itextpdf.text.BaseColor(230, 236, 255);
            for (String h : new String[]{"Title", "Category", "Status", "Score"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, subtotalFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(5);
                semTable.addCell(cell);
            }

            int semScore = 0;
            int winners = 0, runners = 0, participations = 0;

            for (Achievement a : semList) {
                semTable.addCell(new Phrase(a.getTitle(),    normalFont));
                semTable.addCell(new Phrase(a.getCategory(), normalFont));
                semTable.addCell(new Phrase(a.getStatus(),   normalFont));
                semTable.addCell(new Phrase(String.valueOf(a.getScore()), normalFont));
                semScore += a.getScore();

                String s = (a.getStatus() == null) ? "" : a.getStatus().toLowerCase();
                if (s.contains("winner"))      winners++;
                else if (s.contains("runner")) runners++;
                else                           participations++;
            }

            document.add(semTable);

            // Semester summary line
            String summary = "  " + sem + " Summary — Events: " + semList.size()
                    + "  |  Winners: " + winners
                    + "  |  Runners: " + runners
                    + "  |  Participations: " + participations
                    + "  |  Semester Score: " + semScore;

            Paragraph semSummary = new Paragraph(summary, subtotalFont);
            semSummary.setSpacingAfter(10f);
            document.add(semSummary);
        }

        // ══════════════════════════════════════
        // SECTION 3 — Score Summary per Semester (bar-style text chart)
        // ══════════════════════════════════════
        Paragraph spacer = new Paragraph(" ", normalFont);
        spacer.setSpacingAfter(12f);
        document.add(spacer);
        Paragraph sec3 = new Paragraph("Score Summary", headFont);
        sec3.setSpacingAfter(8);
        document.add(sec3);

        PdfPTable summaryTable = new PdfPTable(3);
        summaryTable.setWidthPercentage(70);
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        summaryTable.setWidths(new float[]{2f, 1f, 1f});
        summaryTable.setSpacingAfter(10f);

        com.itextpdf.text.BaseColor summaryHeaderBg = new com.itextpdf.text.BaseColor(41, 98, 255);
        com.itextpdf.text.Font whiteFont = new com.itextpdf.text.Font(Font.FontFamily.HELVETICA, 11,
                Font.BOLD, com.itextpdf.text.BaseColor.WHITE);

        for (String h : new String[]{"Semester", "Events", "Score"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, whiteFont));
            cell.setBackgroundColor(summaryHeaderBg);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            summaryTable.addCell(cell);
        }

        for (String sem : semKeys) {
            List<Achievement> semList = bySemester.get(sem);
            int semScore = semList.stream().mapToInt(Achievement::getScore).sum();

            summaryTable.addCell(new Phrase(sem, normalFont));
            PdfPCell evCell = new PdfPCell(new Phrase(String.valueOf(semList.size()), normalFont));
            evCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            summaryTable.addCell(evCell);
            PdfPCell scCell = new PdfPCell(new Phrase(String.valueOf(semScore), normalFont));
            scCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            summaryTable.addCell(scCell);
        }

        document.add(summaryTable);

        // Final grand total
        Paragraph finalTotal = new Paragraph("Overall Total Score: " + grandTotal, totalFont);
        finalTotal.setAlignment(Element.ALIGN_RIGHT);
        finalTotal.setSpacingBefore(8);
        document.add(finalTotal);

        document.close();
    }
}