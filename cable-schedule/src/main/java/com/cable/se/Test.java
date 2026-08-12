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
import com.cable.se.CableOptimizer2.CableOption;
import com.cable.se.CableOptimizer2.CableRecord2;
import com.cable.se.CableOptimizer2.OptimizationResult2;

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

		String sheetName = "Cable Schedule";

		int startRowIndex = 7; // Excel Row 60 (0-based index)
		int numberOfRecordsToProcess = 10;

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

		FileInputStream fis = new FileInputStream(
				"C:\\Users\\NITISH JAGANWAR\\Downloads\\Schneider Electric -Cable\\POWER CABLE SCEHDULE_Format.xlsx");
		Workbook workbook = new XSSFWorkbook(fis);
		Sheet sheet = workbook.getSheet(sheetName);

		if (sheet == null) {
			System.out.println("❌ ERROR: Sheet '" + sheetName + "' not found!");
			return;
		}

		// for (int i = startRowIndex; i < numberOfRecordsToProcess; i++) {
//
		ExportRecord expRec = new ExportRecord();
//			Row row = sheet.getRow(i);
//			if (row == null)
//				continue;

		// double cableSize =2.5;
		// int initialRuns=1;
//			String sNo = getSafeString(row.getCell(0)); // A
//			String cableTagNo = getSafeString(row.getCell(1)); // B
//			String feederNo = getSafeString(row.getCell(2)); // C
//			String pannel = getSafeString(row.getCell(3)); // D
//			String destinationTag = getSafeString(row.getCell(5)); // F
//			String loadType = getSafeString(row.getCell(8)); // I
//			String starterType = getSafeString(row.getCell(9)); // J
//			double loadFactor = getSafeNumeric(row.getCell(11), 0); // L
//			double voltage = getSafeNumeric(row.getCell(12), 0); // M
//			int feederQty = (int) getSafeNumeric(row.getCell(13), 0); // N
//			double singleUnitRatingKw = getSafeNumeric(row.getCell(14), 0); // O
//
//			double effPercent = getSafeNumeric(row.getCell(22), 0); // W
//
//			double cableSize = getSafeNumeric(row.getCell(25), 0); // Z
//
//			double upstreamRunningVoltageDrop = getSafeNumeric(row.getCell(34), 0.0); // AI
//
//			int initialRuns = (int) getSafeNumeric(row.getCell(39), 0); // AN
//			double unitLength = getSafeNumeric(row.getCell(44), 0); // AS

//			String sNo = getSafeString(row.getCell(1)); // B
//			String cableTagNo = getSafeString(row.getCell(2)); // C
//			String feederNo = getSafeString(row.getCell(3)); // D
//			String pannel = getSafeString(row.getCell(4)); // E
//			String destinationTag = getSafeString(row.getCell(6)); // G
//			String loadType = getSafeString(row.getCell(9)); // J
//			String starterType = getSafeString(row.getCell(10)); // K
//			double loadFactor = getSafeNumeric(row.getCell(12), 0); // M
//			double voltage = getSafeNumeric(row.getCell(13), 0); // N
//			int feederQty = (int) getSafeNumeric(row.getCell(14), 0); // O
//			double singleUnitRatingKw = getSafeNumeric(row.getCell(15), 0); // P
//
//			double effPercent = getSafeNumeric(row.getCell(23), 0); // X
//
//			double cableSize = getSafeNumeric(row.getCell(26), 0); // AA
//
//			double upstreamRunningVoltageDrop = getSafeNumeric(row.getCell(35), 0.0); // AJ
//
//			int initialRuns = (int) getSafeNumeric(row.getCell(24), 0); // AO
//			double unitLength = getSafeNumeric(row.getCell(45), 0); // AT

		String layingMode = "AIR";
		String efficiencyClass = "IE3";
		String sNo = "1";
		// String cableTagNo = "PCC/2F2/1";
		String feederNo = "2F2";
		String pannel = "PCC";
		String destinationTag = "";
		String loadType = "Panel";
		String starterType = "ACB";
		double loadFactor = 0.7;
		double voltage = 415.0;
		int feederQty = 1;
		double singleUnitRatingKw = 1678.32;
		double effPercent = 90;
		double cableSize = 400;
		double upstreamRunningVoltageDrop = 0.10;
		int initialRuns = 1;
		double unitLength = 62;
		String cableTagNo = pannel + "/" + feederNo + "/" + destinationTag + sNo;

		System.out.println("\n--- INPUT  PARAMETERS ---");
		System.out.println("[B] S No : " + sNo);
		System.out.println("[C] Cable Tag No : " + cableTagNo);
		System.out.println("[D] Feeder No : " + feederNo);
		System.out.println("[E] Panel : " + pannel);
		System.out.println("[G] Destination Tag : " + destinationTag);
		System.out.println("[J] Load Type : " + loadType);
		System.out.println("[K] Starter Type : " + starterType);
		System.out.println("[M] Load Factor : " + loadFactor);
		System.out.println("[N] Voltage : " + voltage);
		System.out.println("[O] Feeder Qty : " + feederQty);
		System.out.println("[P] Single Unit Rating (kW) : " + singleUnitRatingKw);
		System.out.println("[X] Efficiency (%) : " + effPercent);
		System.out.println("[AA] Cable Size : " + cableSize);
		System.out.println("[AJ] Upstream Running Voltage Drop : " + upstreamRunningVoltageDrop);
		System.out.println("[AO] Initial Runs : " + initialRuns);
		System.out.println("[AT] Unit Length : " + unitLength);

		inputFilePath = "C:\\Users\\NITISH JAGANWAR\\Desktop\\test\\SE\\temp\\cable_schedule.xlsm";

//			if (cableTagNo == null || cableTagNo.trim().isEmpty()) {
//				System.out.println("Skipping empty row at: " + row.getRowNum());
//				continue; // Skip this row
//			}
//			if (loadType == null || loadType.trim().isEmpty()) {
//				System.out.println("⚠️ Skipping separator/header row at: " + row.getRowNum());
//				continue; // Skip this row
//			}

		List<CutLengthRecord> rowCuts = processcable(engine, inputFilePath, sNo, feederNo, pannel, destinationTag,
				loadType, starterType, loadFactor, voltage, feederQty, singleUnitRatingKw, effPercent, efficiencyClass,
				cableSize, layingMode, initialRuns, unitLength, upstreamRunningVoltageDrop, cableTagNo, expRec);

		for (CutLengthRecord record : rowCuts) {
			System.out.println(record);
		}

//			expRec.tagNo = cableTagNo;
//
//			if (expRec.optResult != null) {
//				masterOptResults.add(expRec);
//			}
		if (rowCuts != null && !rowCuts.isEmpty()) {
			masterCutList.addAll(rowCuts);
		}
//
//			System.out.println("✅ Processed Row " + (row.getRowNum() + 1) + ": " + cableTagNo);

//	}

		DrumScheduleGenerator drumGenerator = new DrumScheduleGenerator();

		double maxDrumLimitSmall = 800.0; // Default fallback0
		double maxDrumLimitLarge = 500.0; // Default fallback

		List<DrumRecord> combinedDrumSchedule = drumGenerator.generateLinkedDrumSchedule(masterCutList,
				maxDrumLimitSmall, maxDrumLimitLarge);

		for (DrumRecord record : combinedDrumSchedule) {
			System.out.println(record);
		}
		// CableExcelExporter.exportBatchResults(outputFilePath, masterOptResults,
		// masterCutList, combinedDrumSchedule);

		for (CutLengthRecord record : rowCuts) {
			System.out.println(record);
		}
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

		System.out.printf("[AB] Base Ampacity " + baseAmpacity + "\n");
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
		System.out.printf("Optimized No of Cores           :%d%n", (long) result.bestCores);
		System.out.printf("Optimized Lowest Cost COMBINATION: %s%n", result.combinationString);
		System.out.printf("Optimized Lowest Cost            : Rs. %d%n", (long) result.minCost);
		System.out.printf("Solution with min no of runs    : %d%n", result.minRun_Runs);
		System.out.printf("Min cable size (Sq. mm)         : %.1f Sqmm%n", result.minRun_Size);
		System.out.printf("Total length                    : %.1f m%n", result.bestLen);
		System.out.printf("Optimized Lugs Qty              : %d Nos%n", result.outLugs);
		System.out.printf("Optimized Glands Qty            : %d Nos%n", result.outGlands);
		System.out.printf("Final Selection Runs            : %s%n",
				(result.finalSelectionRuns != null ? result.finalSelectionRuns : "-BLANK-"));
		System.out.printf("Final Cable Selection Size      : %s Sqmm%n",
				(result.finalSelectionSize != null ? String.format("%.1f", result.finalSelectionSize) : "-BLANK-"));
		System.out.println("Derated Current :" + result.deratedCurrent);
		System.out.println("\n  [All OK Cases Matrix]");
		System.out.println("  -------------------------------------");
		System.out.println(result.matrix);

		// OptimizationResult result = optimizer.optimize(record);

		System.out.println("===== Optimization Result =====");
		System.out.println("status              : " + result.status);
		System.out.println("bestRuns            : " + result.bestRuns);
		System.out.println("bestSize            : " + result.bestSize);
		System.out.println("bestCores           : " + result.bestCores);
		System.out.println("bestCond            : " + result.bestCond);
		System.out.println("minRun_Runs         : " + result.minRun_Runs);
		System.out.println("minRun_Size         : " + result.minRun_Size);
		System.out.println("combinationString   : " + result.combinationString);
		System.out.println("minCost             : " + result.minCost);
		System.out.println("bestLen             : " + result.bestLen);
		System.out.println("outLugs             : " + result.outLugs);
		System.out.println("outGlands           : " + result.outGlands);
		System.out.println("finalSelectionRuns  : " + result.finalSelectionRuns);
		System.out.println("finalSelectionSize  : " + result.finalSelectionSize);
		System.out.println("deratedCurrent      : " + result.deratedCurrent);// deratedCurrent
		System.out.println("totalCurrentRating  : " + result.totalCurrentRating);// baseAmpacity
		System.out.println("cableResistance     : " + result.cableResistance);// cableResistance
		System.out.println("cableReactance      : " + result.cableReactance);// cableReactance
		System.out.println("startingVd          : " + result.startingVd);// startingVoltageDrop
		System.out.println("totalStartingVd     : " + result.totalStartingVd);// totalStartingVoltageDrop
		System.out.println("runningVd           : " + result.runningVd);// runningVoltageDrop
		System.out.println("totalRunningVd      : " + result.totalRunningVd);// totalRunningVoltageDrop
		System.out.println("Validation");
		System.out.println("capacityValidationStatus : " + result.capacityStatus);
		System.out.println("vdValidationStatus       : " + result.vdStatus);
		System.out.println("finalValidation          : " + result.status);
		System.out.println("No Of Joints : " + result.totalJoints);
		System.out.println("--------------------------------");
		System.out.println("matrix:\n" + result.matrix);
		System.out.println("================================");
//		expRec.optResult = result; // Save Result for Export
//		expRec.core = noOfCores;

//		CableRecord2 record2 = new CableRecord2();
//		record2.tagNo = cableTagNo;
//		record2.pannel = pannel;
//		record2.layingMode = layingMode;
//		record2.starterType = starterType;
//		record2.loadType = loadType;
//		record2.noOfCores = noOfCores;
//		record2.unitLength = unitLength;
//		record2.voltage = voltage;
//		record2.fullLoadCurrent = fullLoadCurrent;
//		record2.startingCurrent = startingCurrent;
//		record2.fullLoadPf = fullLoadPf;
//		record2.startingPf = startingPf;
//		record2.deratingFactor = deratingFactor;
//		record2.upstreamRunningVoltageDrop = upstreamRunningVoltageDrop;
//		record2.totalRunningVoltageDrop = totalRunningVoltageDrop;
//		record2.limitE12 = limitE12;
//		record2.limitE13 = limitE13;
//		record2.limitE14 = limitE14;
//		record2.limitE15 = limitE15;
//
//		// (Original Length = 6 * 62 = 372.0)
//		record2.cableLen = unitLength * initialRuns;
//		CableOptimizer2 optimizer2 = new CableOptimizer2(engine, filePath);
//		OptimizationResult2 result2 = optimizer2.optimize2(record2);
//		System.out.printf("Status                          : %s%n", result2.status);
//		System.out.printf("Optimized No. of Runs           : %s%n", (result2.bestRuns > 0 ? result2.bestRuns : "-"));
//		System.out.printf("Optimized Cable Size (Sq. mm)   : %s Sqmm%n", (result2.bestSize > 0 ? result2.bestSize : "-"));
//		System.out.printf("Optimized No of Cores           :%d%n", (long) result2.bestCores);
//		System.out.printf("Optimized Lowest Cost COMBINATION: %s%n", result.combinationString);
//		System.out.printf("Optimized Lowest Cost            : Rs. %d%n", (long) result2.minCost);
//		System.out.printf("Solution with min no of runs    : %d%n", result2.minRun_Runs);
//		System.out.printf("Min cable size (Sq. mm)         : %.1f Sqmm%n", result2.minRun_Size);
//		System.out.printf("Total length                    : %.1f m%n", result2.bestLen);
//		System.out.printf("Optimized Lugs Qty              : %d Nos%n", result2.outLugs);
//		System.out.printf("Optimized Glands Qty            : %d Nos%n", result2.outGlands);
//		System.out.printf("Final Selection Runs            : %s%n",
//				(result2.finalSelectionRuns != null ? result2.finalSelectionRuns : "-BLANK-"));
//		System.out.printf("Final Cable Selection Size      : %s Sqmm%n",
//				(result2.finalSelectionSize != null ? String.format("%.1f", result2.finalSelectionSize) : "-BLANK-"));
//		System.out.println("Derated Current :"+result2.deratedCurrent);
//		System.out.println(result2.matrix);
//	
//		List<CableOption> availableOptions = result2.availableOptions;
//		for (CableOption option : result2.availableOptions) {
//
//		    System.out.println("--------------------------------");
//
//		    System.out.println("Combination               : " + option.combination);
//		    System.out.println("Runs                      : " + option.runs);
//		    System.out.println("Size                      : " + option.size);
//		    System.out.println("Cores                     : " + option.cores);
//		    System.out.println("Conductor                 : " + option.conductor);
//
//		    System.out.println("Derated Current           : " + option.deratedCurrent);
//
//		    System.out.println("Starting VD               : " + option.startingVoltageDrop);
//		    System.out.println("Total Starting VD         : " + option.totalStartingVoltageDrop);
//
//		    System.out.println("Running VD                : " + option.runningVoltageDrop);
//		    System.out.println("Total Running VD          : " + option.totalRunningVoltageDrop);
//
//		    System.out.println("Unit Price                : " + option.unitPrice);
//		    System.out.println("Total Length              : " + option.totalLength);
//		    System.out.println("Total Cost                : " + option.totalCost);
//
//		    System.out.println("Output Lugs               : " + option.outLugs);
//		    System.out.println("Output Glands             : " + option.outGlands);
//		}
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
