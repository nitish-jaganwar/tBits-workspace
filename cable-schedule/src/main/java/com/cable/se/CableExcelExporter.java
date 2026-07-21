package com.cable.se;

import java.io.FileOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CableExcelExporter {

	public static void exportBatchResults(String outputPath, List<Test.ExportRecord> optResults,
			List<CutLengthRecord> cuts, List<DrumRecord> drums) {

		try (Workbook workbook = new XSSFWorkbook()) {

			// ==========================================
			// SHEET 1: Optimization Results
			// ==========================================
			Sheet sheet1 = workbook.createSheet("Optimization Results");
			createOptimizationSheet(sheet1, optResults, workbook);

			// ==========================================
			// SHEET 2: Ordering Cut Lengths
			// ==========================================
			Sheet sheet2 = workbook.createSheet("Ordering Cut Lengths");
			createCutLengthSheet(sheet2, cuts, workbook);

			// ==========================================
			// SHEET 3: Drum Schedule
			// ==========================================
			Sheet sheet3 = workbook.createSheet("Drum Schedule");
			createDrumScheduleSheet(sheet3, drums, workbook);

			// Write to Output File
			try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
				workbook.write(fileOut);
			}

			System.out.println("✅ Data Successfully Exported to: " + outputPath);

		} catch (Exception e) {
			System.out.println("❌ ERROR: Failed to create Excel Output File!");
			e.printStackTrace();
		}
	}

	private static void createOptimizationSheet(Sheet sheet, List<Test.ExportRecord> records, Workbook wb) {
		String[] headers = { "Cable Tag No", "Optimized Core", "Total Length (m)", "Optimized Number of Runs",
				"Optimized Cable Size (Sq. mm)", "Solution with Min Runs", "Min Associated Cable Size (Sq. mm)",
				"Status (Optimal/Sub-Optimal)", "Final Selected Runs", "Final Selected Cable Size (Sq. mm)",
				"Optimized Lowest Cost (Rs.)", "All Valid Cases (Matrix)", "Optimized Lug Quantity",
				"Optimized Gland Quantity" };

		Row headerRow = sheet.createRow(0);
		CellStyle headerStyle = createHeaderStyle(wb);
		for (int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		int rowIdx = 1;
		for (Test.ExportRecord record : records) {
			Row row = sheet.createRow(rowIdx++);
			CableOptimizer.OptimizationResult res = record.optResult;

			row.createCell(0).setCellValue(record.tagNo);
			row.createCell(1).setCellValue(record.core);
			row.createCell(2).setCellValue(res.bestLen);
			row.createCell(3).setCellValue(res.bestRuns > 0 ? String.valueOf(res.bestRuns) : "-");
			row.createCell(4).setCellValue(res.bestSize > 0 ? String.valueOf(res.bestSize) : "-");
			row.createCell(5).setCellValue(res.minRun_Runs);
			row.createCell(6).setCellValue(res.minRun_Size);
			row.createCell(7).setCellValue(res.status);
			row.createCell(8)
					.setCellValue(res.finalSelectionRuns != null ? String.valueOf(res.finalSelectionRuns) : "-BLANK-");
			row.createCell(9)
					.setCellValue(res.finalSelectionSize != null ? String.valueOf(res.finalSelectionSize) : "-BLANK-");
			row.createCell(10).setCellValue(res.minCost);
			row.createCell(11).setCellValue(res.matrix);
			row.createCell(12).setCellValue(res.outLugs);
			row.createCell(13).setCellValue(res.outGlands);
		}
	}

	private static void createCutLengthSheet(Sheet sheet, List<CutLengthRecord> cuts, Workbook wb) {
		String[] headers = { "Ordering Tag No", "Original Tag No", "Length (m)", "Size (Sq. mm)", "Core", "Cable Type",
				"Assigned Drum" };
		Row headerRow = sheet.createRow(0);
		CellStyle headerStyle = createHeaderStyle(wb);
		for (int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		int rowIdx = 1;
		for (CutLengthRecord cut : cuts) {
			Row row = sheet.createRow(rowIdx++);
			row.createCell(0).setCellValue(cut.orderingTagNumber);
			row.createCell(1).setCellValue(cut.orgTagNumber);
			row.createCell(2).setCellValue(cut.cableLength);
			row.createCell(3).setCellValue(cut.diameterSize);
			row.createCell(4).setCellValue(cut.core);
			row.createCell(5).setCellValue(cut.cableType);
			row.createCell(6).setCellValue(cut.orderingDrumNumber != null ? cut.orderingDrumNumber : "");
		}
	}

	private static void createDrumScheduleSheet(Sheet sheet, List<DrumRecord> drums, Workbook wb) {
		
		String[] headers = { "Cable Combination", "Drum No.", "Max Drum Limit (m)", "Exact Ordered Length (m)", "Total Pieces", "Cut Details (Count x Length)" };
		Row headerRow = sheet.createRow(0);
		CellStyle headerStyle = createHeaderStyle(wb);
		for (int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		int rowIdx = 1;
		for (DrumRecord drum : drums) {
			Row row = sheet.createRow(rowIdx++);
			row.createCell(0).setCellValue(drum.cableCombination);
			row.createCell(1).setCellValue(drum.drumNo);
			row.createCell(2).setCellValue(drum.maxDrumLimit); // 🔥 Nayi value set ki
			row.createCell(3).setCellValue(drum.exactOrderedLength);
			row.createCell(4).setCellValue(drum.totalPieces);
			
			Cell detailsCell = row.createCell(5);
			detailsCell.setCellValue(drum.cutDetails);
		}
	}
	private static CellStyle createHeaderStyle(Workbook wb) {
		CellStyle style = wb.createCellStyle();
		Font font = wb.createFont();
		font.setBold(true);
		style.setFont(font);
		return style;
	}
}