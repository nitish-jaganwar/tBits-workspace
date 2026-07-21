package com.cable.se;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.cable.se.CableOptimizer.CableRecord;
import com.cable.se.CableOptimizer.OptimizationResult;

public class Test {

	public static class ExportRecord {
		public String tagNo;
		public double core;
		public OptimizationResult optResult;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Starting Cable Master Batch Processor...");

		// INPUT DETAILS (File Paths)
		String inputFilePath = "C:\\Users\\NITISH JAGANWAR\\Desktop\\test\\SE\\temp\\cable_schedule.xlsm";
		String outputFilePath = "C:\\Users\\NITISH JAGANWAR\\Desktop\\10_rows_22-06-20265555.xlsx";

		String sheetName = "SARALA NAGAR";

		int startRowIndex = 12; // Excel Row 60 (0-based index)
		int numberOfRecordsToProcess = 22;

		processBulkExcelData(inputFilePath, outputFilePath, sheetName, startRowIndex, numberOfRecordsToProcess);
	}

	private static void processBulkExcelData(String inputFilePath, String outputFilePath, String sheetName,
			int startRowIndex, int numberOfRecordsToProcess) throws IOException {

		CalculationEngine engine = new CalculationEngine();
		// ExportRecord expRec = new ExportRecord();

		engine.cache.loadAllDataIntoRam(inputFilePath);

		// Master lists to collect data for EXCEL EXPORT
		List<ExportRecord> masterOptResults = new ArrayList<>();
		List<CutLengthRecord> masterCutList = new ArrayList<>();

		FileInputStream fis = new FileInputStream(inputFilePath);
		Workbook workbook = new XSSFWorkbook(fis);
		Sheet sheet = workbook.getSheet(sheetName);

		if (sheet == null) {
			System.out.println("❌ ERROR: Sheet '" + sheetName + "' not found!");
			return;
		}

		for (int i = startRowIndex; i < numberOfRecordsToProcess; i++) {

			ExportRecord expRec = new ExportRecord();
			Row row = sheet.getRow(i);
			if (row == null)
				continue;

			String layingMode = "AIR"; // AR
			String efficiencyClass = "IE3";

			String sNo = getSafeString(row.getCell(0)); // A
			String cableTagNo = getSafeString(row.getCell(1)); // B
			String feederNo = getSafeString(row.getCell(2)); // C
			String pannel = getSafeString(row.getCell(3)); // D
			String destinationTag = getSafeString(row.getCell(5)); // F
			String loadType = getSafeString(row.getCell(8)); // I
			String starterType = getSafeString(row.getCell(9)); // J
			double loadFactor = getSafeNumeric(row.getCell(11), 0); // L
			double voltage = getSafeNumeric(row.getCell(12), 0); // M
			int feederQty = (int) getSafeNumeric(row.getCell(13), 0); // N
			double singleUnitRatingKw = getSafeNumeric(row.getCell(14), 0); // O

			double effPercent = getSafeNumeric(row.getCell(22), 0); // W

			double cableSize = getSafeNumeric(row.getCell(25), 0); // Z

			double upstreamRunningVoltageDrop = getSafeNumeric(row.getCell(34), 0.0); // AI

			int initialRuns = (int) getSafeNumeric(row.getCell(39), 0); // AN
			double unitLength = getSafeNumeric(row.getCell(44), 0); // AS

			inputFilePath = "C:\\Users\\NITISH JAGANWAR\\Desktop\\test\\SE\\temp\\cable_schedule.xlsm";

			 //String cableTagNo = pannel + "/" + feederNo + "/" + destinationTag + sN

			if (cableTagNo == null || cableTagNo.trim().isEmpty()) {
				System.out.println("Skipping empty row at: " + row.getRowNum());
				continue; // Skip this row
			}
			if (loadType == null || loadType.trim().isEmpty()) {
				System.out.println("⚠️ Skipping separator/header row at: " + row.getRowNum());
				continue; // Skip this row
			}

			List<CutLengthRecord> rowCuts = processcable(engine, inputFilePath, sNo, feederNo, pannel, destinationTag,
					loadType, starterType, loadFactor, voltage, feederQty, singleUnitRatingKw, effPercent,
					efficiencyClass, cableSize, layingMode, initialRuns, unitLength, upstreamRunningVoltageDrop,
					cableTagNo, expRec);

			expRec.tagNo = cableTagNo;

			if (expRec.optResult != null) {
				masterOptResults.add(expRec);
			}
			if (rowCuts != null && !rowCuts.isEmpty()) {
				masterCutList.addAll(rowCuts);
			}

			System.out.println("✅ Processed Row " + (row.getRowNum() + 1) + ": " + cableTagNo);

		}
//		DrumScheduleGenerator drumGenerator = new DrumScheduleGenerator();
//
//		double maxDrumLimitSmall = 800.0; // Default fallback0
//		double maxDrumLimitLarge = 500.0; // Default fallback
//
//		List<DrumRecord> combinedDrumSchedule = drumGenerator.generateLinkedDrumSchedule(masterCutList,
			//	maxDrumLimitSmall, maxDrumLimitLarge);
		//CableExcelExporter.exportBatchResults(outputFilePath, masterOptResults, masterCutList, combinedDrumSchedule);
	}

	private static List<CutLengthRecord> processcable(CalculationEngine engine, String filePath, String sNo,
			String feederNo, String pannel, String destinationTag, String loadType, String starterType,
			double loadFactor, double voltage, int feederQty, double singleUnitRatingKw, double effPercent,
			String efficiencyClass, double cableSize, String layingMode, int initialRuns, double unitLength,
			double upstreamRunningVoltageDrop, String cableTagNo, ExportRecord expRec) {

		double totalKw = engine.calculateTotalActivePowerKw(singleUnitRatingKw, feederQty);

		double fullLoadPf = engine.calculateFullLoadPf(loadType, singleUnitRatingKw);
		Double startingPf = engine.calculateStartingPowerFactor(loadType, singleUnitRatingKw);

		double ratingKva = engine.calculateTotalApparentPowerKva(totalKw, fullLoadPf, loadFactor);

		double effPercent2 = engine.calculateEfficiencyPercent(loadType, singleUnitRatingKw, efficiencyClass);

		double effDecimal = engine.convertEfficiencyToDecimal(effPercent);

		double fullLoadCurrent = engine.calculateFullLoadCurrent(totalKw, ratingKva, voltage, loadFactor, effDecimal);

		Double startingCurrent = engine.calculateStartingCurrent(loadType, starterType, efficiencyClass,
				singleUnitRatingKw, fullLoadCurrent);

		double noOfCores = engine.calculateNumberOfCores(loadType, cableSize);

		Double deratingFactor = engine.getDeratingFactor(filePath);

		Double baseAmpacity = engine.getBaseCableAmpacity(layingMode, cableSize, noOfCores, filePath);

		Double totalCurrentRating = (baseAmpacity != null)
				? engine.calculateCurrentRatingOfCable(baseAmpacity, initialRuns)
				: null;

		Double deratedCurrent = engine.calculateDeratedCurrentOfCable(deratingFactor, totalCurrentRating);

		Double cableResistance = engine.getCableResistance(cableSize, filePath);

		Double cableReactance = engine.getCableReactance(cableSize, noOfCores, filePath);

		Double startingVoltageDrop = engine.calculateStartingVoltageDrop(startingCurrent, unitLength, cableResistance,
				startingPf, cableReactance, initialRuns, voltage);

		Double totalStartingVoltageDrop = engine.calculateTotalStartingVoltageDrop(startingVoltageDrop);

		Double runningVoltageDrop = engine.calculateRunningVoltageDrop(fullLoadCurrent, unitLength, cableResistance,
				fullLoadPf, cableReactance, initialRuns, voltage);

		Double totalRunningVoltageDrop = engine.calculateTotalRunningVoltageDrop(runningVoltageDrop,
				upstreamRunningVoltageDrop);

		Double limitE12 = engine.getVdLimitE12(filePath);

		Double limitE13 = engine.getVdLimitE13(filePath);

		Double limitE15 = engine.getVdLimitE15(filePath);

		Double limitE14 = engine.getVdLimitE14(filePath);

		String capacityValidationStatus = engine.validateCableCapacity(deratedCurrent, fullLoadCurrent);

		String vdValidationStatus = engine.validateVoltageDrop(totalStartingVoltageDrop, runningVoltageDrop,
				totalRunningVoltageDrop, limitE12, limitE13, limitE14, limitE15, pannel);

		String finalValidation = engine.validateFinalCableSize(capacityValidationStatus, vdValidationStatus);

		String formattedSize = engine.formatCableSizeString(noOfCores, cableSize);

		String cableType = engine.getCableType(cableSize);

		String cableCode = engine.getCableCode(cableSize);

		double totalLength = engine.calculateTotalLength(noOfCores, unitLength, initialRuns);

		Double cableDiameter = engine.getCableDiameter(cableSize, noOfCores, filePath);

		Double totalCableDiameter = engine.calculateTotalCableDiameter(cableDiameter, initialRuns);

		int lugsQty = engine.calculateLugsQty(noOfCores, initialRuns);

		int glandsQty = engine.calculateGlandsQty(initialRuns);

		String cableSelected = engine.formatCableSelected(noOfCores, cableSize, cableType);

		CableRecord record = new CableRecord();
		record.tagNo = cableTagNo;
		record.pannel = pannel;
		record.layingMode = layingMode;
		record.starterType = starterType;
		record.loadType = loadType;
		record.noOfCores = noOfCores;
		record.unitLength = unitLength;
		record.voltage = voltage;
		record.fullLoadCurrent = fullLoadCurrent;
		record.startingCurrent = startingCurrent;
		record.fullLoadPf = fullLoadPf;
		record.startingPf = startingPf;
		record.deratingFactor = deratingFactor;
		record.upstreamRunningVoltageDrop = upstreamRunningVoltageDrop;
		record.totalRunningVoltageDrop = totalRunningVoltageDrop;
		record.limitE12 = limitE12;
		record.limitE13 = limitE13;
		record.limitE14 = limitE14;
		record.limitE15 = limitE15;

		// (Original Length = 6 * 62 = 372.0)
		record.cableLen = unitLength * initialRuns;

		// Ensure Derating and Limits were fetched successfully
		if (deratingFactor == null || limitE12 == null) {
			System.out.println("❌ Error: Excel  Limits Not checked check sheet name  .");
			return new ArrayList<>();
		}
		System.out.println("\n--- CALCULATED ELECTRICAL PARAMETERS ---");

		System.out.printf("[P] Total kW            : %.2f kW\n", totalKw);
		System.out.printf("[Q] Rating (kVA)        : %.2f kVA\n", ratingKva);
		System.out.printf("[R] Full Load Current   : %.2f A\n", fullLoadCurrent);
		System.out.printf("[S] Starting Current    : %s\n",
				(startingCurrent == null ? "-NA-" : String.format("%.2f A", startingCurrent)));
		System.out.printf("[T] Full Load PF        : %.2f\n", fullLoadPf);
		System.out.printf("[U] Starting PF         : %s\n",
				(startingPf == null) ? "-NA-" : String.format("%.2f", startingPf));
		System.out.printf("[V] Efficiency INPUT  : %.4f\n", effPercent);
		System.out.printf("[W] CALCULATED Efficiency          : %.2f%%\n", effPercent2);

		System.out.printf("[Y] Number of Cores     : %.1f\n", noOfCores);
		System.out.printf("[AA] Derating Factor    : %s\n",
				(deratingFactor != null ? String.format("%.4f", deratingFactor) : "Fetch Failed"));
		System.out.printf("[AB] Base Ampacity      : %s\n",
				(totalCurrentRating != null ? String.format("%.2f A", totalCurrentRating) : "Fetch Failed"));
		
		System.out.printf("[AB] Base Ampacity "+baseAmpacity+"\n");
		System.out.printf("[AC] Derated Current    : %s\n",
				(deratedCurrent != null ? String.format("%.2f A", deratedCurrent) : "Failed"));

		System.out.printf("[AD] Resistance (R)     : %s Ohms/km\n",
				(cableResistance != null ? String.format("%.4f", cableResistance) : "Fetch Failed"));
		System.out.printf("[AE] Reactance (X)      : %s Ohms/km\n",
				(cableReactance != null ? String.format("%.4f", cableReactance) : "Fetch Failed"));
		System.out.printf("[AF] Starting Volt Drop (%%): %s\n",
				(startingVoltageDrop != null ? String.format("%.2f%%", startingVoltageDrop) : "-NA-"));
		System.out.printf("[AG] Total Start VD (%%) : %s\n",
				(totalStartingVoltageDrop != null ? String.format("%.2f%%", totalStartingVoltageDrop) : "-NA-"));

		System.out.printf("[AH] Running VD (%%)     : %s\n",
				(runningVoltageDrop != null ? String.format("%.2f%%", runningVoltageDrop) : "Failed"));

		System.out.println("[AI] Upstream Running Voltage Drop (%) :" + upstreamRunningVoltageDrop);
		System.out.println("[AJ] Total Running Voltage Drop (%) :"
				+ (totalRunningVoltageDrop != null ? String.format("%.2f%%", totalRunningVoltageDrop) : "Failed"));

		System.out.println("\n--- VALIDATIONS ---");
		System.out.printf("[AK] Capacity Status    : %s\n", capacityValidationStatus);
		System.out.printf("[AL] Volt Drop Status   : %s\n", vdValidationStatus);
		System.out.printf("[AM] FINAL VALIDATION   : %s\n", finalValidation);
		System.out.println("\n--- VALIDATIONS ---");
		System.out.printf("[AN] Final No. of Runs  : %d\n", initialRuns);
		System.out.printf("[AO] Formatted Size     : %s\n", formattedSize);
		System.out.printf("[AP] Cable Type         : %s\n", (cableType != null ? cableType : "Not Found"));
		System.out.printf("[AQ] Cable Code         : %s\n", cableCode);
		System.out.printf("[AR] Cable Laying Mode  : %s\n", layingMode); // Defaulted
		System.out.printf("[AT] Total Length (m)   : %.1f\n", totalLength);
		System.out.printf("[AU] Cable Diameter     : %.1f mm\n", (cableDiameter != null ? cableDiameter : 0.0));
		System.out.printf("[AV] Total Diameter     : %.1f mm\n",
				(totalCableDiameter != null ? totalCableDiameter : 0.0));
		System.out.printf("[AW] Lugs Qty (Nos)     : %d\n", lugsQty);
		System.out.printf("[AX] Glands Qty (Nos)   : %d\n", glandsQty);
		System.out.printf("[AY] Cable Selected     : %s\n", cableSelected);

		CableOptimizer optimizer = new CableOptimizer(engine, filePath);
		OptimizationResult result = optimizer.optimize(record);
		/*
		 * System.out.println("║      CABLE MASTER OPTIMIZER RESULT   ║"); //
		 * System.out.println("══════════════════════════════════════");
		 * 
		 * System.out.printf("Status                          : %s%n", result.status);
		 * System.out.printf("Optimized No. of Runs           : %s%n", (result.bestRuns
		 * > 0 ? result.bestRuns : "-"));
		 * System.out.printf("Optimized Cable Size (Sq. mm)   : %s Sqmm%n",
		 * (result.bestSize > 0 ? result.bestSize : "-"));
		 * 
		 * System.out.printf("Optimized Lowest Cost COMBINATION: %s%n",
		 * result.combinationString);
		 * System.out.printf("Optimized Lowest Cost                      : Rs. %d%n",
		 * (long) result.minCost);
		 * System.out.printf("Solution with min no of runs    : %d%n",
		 * result.minRun_Runs);
		 * System.out.printf("Min cable size (Sq. mm)         : %.1f Sqmm%n",
		 * result.minRun_Size);
		 * System.out.printf("Total length                    : %.1f m%n",
		 * result.bestLen);
		 * System.out.printf("Optimized Lugs Qty              : %d Nos%n",
		 * result.outLugs);
		 * System.out.printf("Optimized Glands Qty            : %d Nos%n",
		 * result.outGlands);
		 * System.out.printf("Final Selection Runs            : %s%n",
		 * (result.finalSelectionRuns != null ? result.finalSelectionRuns : "-BLANK-"));
		 * System.out.printf("Final Cable Selection Size      : %s Sqmm%n",
		 * (result.finalSelectionSize != null ? String.format("%.1f",
		 * result.finalSelectionSize) : "-BLANK-"));
		 * System.out.println("\n  [All OK Cases Matrix]");
		 * System.out.println("  -------------------------------------");
		 * System.out.println(result.matrix);
		 */
		System.out.println("║      CABLE MASTER OPTIMIZER RESULT   ║");
		//
		System.out.println("══════════════════════════════════════");

		System.out.printf("Status                          : %s%n", result.status);
		System.out.printf("Optimized No. of Runs           : %s%n", (result.bestRuns > 0 ? result.bestRuns : "-"));
		System.out.printf("Optimized Cable Size (Sq. mm)   : %s Sqmm%n", (result.bestSize > 0 ? result.bestSize : "-"));

		System.out.printf("Optimized Lowest Cost COMBINATION: %s%n", result.combinationString);
		System.out.printf("Optimized Lowest Cost                      : Rs. %d%n", (long) result.minCost);
		System.out.printf("Solution with min no of runs    : %d%n", result.minRun_Runs);
		System.out.printf("Min cable size (Sq. mm)         : %.1f Sqmm%n", result.minRun_Size);
		System.out.printf("Total length                    : %.1f m%n", result.bestLen);
		System.out.printf("Optimized Lugs Qty              : %d Nos%n", result.outLugs);
		System.out.printf("Optimized Glands Qty            : %d Nos%n", result.outGlands);
		System.out.printf("Final Selection Runs            : %s%n",
				(result.finalSelectionRuns != null ? result.finalSelectionRuns : "-BLANK-"));
		System.out.printf("Final Cable Selection Size      : %s Sqmm%n",
				(result.finalSelectionSize != null ? String.format("%.1f", result.finalSelectionSize) : "-BLANK-"));
		System.out.println("\n  [All OK Cases Matrix]");
		System.out.println("  -------------------------------------");
		System.out.println(result.matrix);

		expRec.optResult = result; // Save Result for Export
		expRec.core = noOfCores;
		CutLengthGenerator generator = new CutLengthGenerator();
		return generator.generateOrderingCutLengths(record, result);
	}

	private static String getSafeString(Cell cell) {
		if (cell == null)
			return "";
		try {
			CellType type = cell.getCellType();
			if (type == CellType.FORMULA)
				type = cell.getCachedFormulaResultType();
			if (type == CellType.STRING)
				return cell.getStringCellValue().trim();
			if (type == CellType.NUMERIC) {
				double val = cell.getNumericCellValue();
				if (val == Math.floor(val))
					return String.valueOf((long) val);
				return String.valueOf(val);
			}
		} catch (Exception e) {
		}
		return "";
	}

	private static double getSafeNumeric(Cell cell, double defaultValue) {
		if (cell == null)
			return defaultValue;
		try {
			CellType type = cell.getCellType();
			if (type == CellType.FORMULA)
				type = cell.getCachedFormulaResultType();
			if (type == CellType.NUMERIC)
				return cell.getNumericCellValue();
			if (type == CellType.STRING)
				return Double.parseDouble(cell.getStringCellValue().trim());
		} catch (Exception e) {
		}
		return defaultValue;
	}

}
