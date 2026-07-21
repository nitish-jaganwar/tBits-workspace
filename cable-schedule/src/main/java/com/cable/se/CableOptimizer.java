package com.cable.se;

public class CableOptimizer {

	private static final double[] SIZES = { 2.5, 4, 6, 10, 16, 25, 35, 50, 70, 95, 120, 150, 185, 240, 300, 400 };
	private static final String[] COND_TYPES = { "XLPE,Al", "XLPE,Cu" };

	private CalculationEngine engine;
	private String excelFilePath;

	public CableOptimizer(CalculationEngine engine, String excelFilePath) {
		this.engine = engine;
		this.excelFilePath = excelFilePath;
	}

	public OptimizationResult optimize(CableRecord record) {
		OptimizationResult result = new OptimizationResult();

		// System.out.println(" Running Master Optimizer Loop (Matching Excel VBA Bug
		// Logic)...");

		double minCost = Double.MAX_VALUE;
		int bestRuns = -1;
		double bestSize = -1;
		String bestCond = "";
		double bestCores = -1;
		double bestLen = 0;
		int minRun_Runs = 0;
		double minRun_Size = 0;
		boolean isFirstValidCase = true;

		StringBuilder matrixBuilder = new StringBuilder();
		// Set<String> seenSizes = new LinkedHashSet<>();
		double lastValidSize = -1.0;

		outerLoop: for (int runTest = 1; runTest <= 15; runTest++) {
			for (double size : SIZES) {
				// EXCEL FORMULA FOR CORES
				double testCores = 3.5; // Default
				String lType = (record.loadType != null) ? record.loadType.trim().toUpperCase() : "";

				if (lType.equals("MOTOR") || lType.equals("HEATER")) {
					testCores = 3.0;
				} else {
					if (size <= 16.0) {
						testCores = 4.0;
					} else {
						testCores = 3.5;
					}
				}
				for (String cond : COND_TYPES) {

					Double baseAmpacity = engine.getBaseCableAmpacity(record.layingMode, size, testCores,
							excelFilePath);
					if (baseAmpacity == null)
						continue;

					Double totalCurrentRating = engine.calculateCurrentRatingOfCable(baseAmpacity, runTest);
					Double deratedCurrent = engine.calculateDeratedCurrentOfCable(record.deratingFactor,
							totalCurrentRating);

					Double cableResistance = engine.getCableResistance(size, excelFilePath);
					Double cableReactance = engine.getCableReactance(size, testCores, excelFilePath);
					if (cableResistance == null || cableReactance == null)
						continue;

					Double startingVd = engine.calculateStartingVoltageDrop(record.startingCurrent, record.unitLength,
							cableResistance, record.startingPf, cableReactance, runTest, record.voltage);
					Double totalStartingVd = engine.calculateTotalStartingVoltageDrop(startingVd);

					Double runningVd = engine.calculateRunningVoltageDrop(record.fullLoadCurrent, record.unitLength,
							cableResistance, record.fullLoadPf, cableReactance, runTest, record.voltage);

					Double totalRunningVd = engine.calculateTotalRunningVoltageDrop(runningVd,
							record.upstreamRunningVoltageDrop);

					String capacityStatus = engine.validateCableCapacity(deratedCurrent, record.fullLoadCurrent);

					String vdStatus = engine.validateVoltageDrop(totalStartingVd, runningVd, totalRunningVd,
							record.limitE12, record.limitE13, record.limitE14, record.limitE15, record.pannel);
					String finalValidation = engine.validateFinalCableSize(capacityStatus, vdStatus);

					if ("OK".equalsIgnoreCase(finalValidation)) {
						Double unitPrice = engine.fetchCablePrice(testCores, size, cond, excelFilePath);

						if (unitPrice != null && unitPrice > 0) {

							if (size == lastValidSize) {
								continue outerLoop;
							}

							// get new valid size, so update lastValidSize
							lastValidSize = size;

							int actualRuns = runTest;
							if ("STAR-DELTA".equalsIgnoreCase(record.starterType)
									|| "STAR DELTA".equalsIgnoreCase(record.starterType)) {
								actualRuns = runTest * 2;
							}
							double totalLength = record.unitLength * actualRuns;
							double currentCost = unitPrice * totalLength;

							if (isFirstValidCase) {
								minRun_Runs = runTest;
								minRun_Size = size;
								isFirstValidCase = false;
							}

							String coreStr = (testCores == Math.floor(testCores)) ? String.valueOf((int) testCores)
									: String.valueOf(testCores);
							String sizeStr = (size == Math.floor(size)) ? String.valueOf((int) size)
									: String.valueOf(size);

							matrixBuilder.append(runTest).append("R x ").append(coreStr).append("C x ").append(sizeStr)
									.append(" Sqmm(Rs.").append((long) currentCost).append(")\n");

							if (currentCost < minCost) {
								minCost = currentCost;
								bestRuns = runTest;
								bestSize = size;
								bestCores = testCores;
								bestCond = cond;
								bestLen = totalLength;
							}
							continue outerLoop;
						}
					}
				}
			}
		}

		if (bestRuns != -1) {
			String coreStr = (bestCores == Math.floor(bestCores)) ? String.valueOf((int) bestCores)
					: String.valueOf(bestCores);
			String sizeStr = (bestSize == Math.floor(bestSize)) ? String.valueOf((int) bestSize)
					: String.valueOf(bestSize);
			result.status = (bestRuns < 4) ? "Optimal" : "Sub Optimal";
			result.bestRuns = bestRuns;
			result.bestSize = bestSize;
			result.minRun_Runs = minRun_Runs;
			result.minRun_Size = minRun_Size;

			result.bestCores = bestCores;
			result.bestCond = bestCond;
			result.combinationString = bestRuns + " Runs of " + coreStr + " C - " + sizeStr + " Sqmm - " + bestCond;
			result.minCost = minCost;
			result.bestLen = bestLen;

			int coreCountForLugs = (int) Math.ceil(bestCores);
			result.outLugs = bestRuns * coreCountForLugs * 2;
			result.outGlands = bestRuns * 2;

			// THE "FINAL SELECTION" LOGIC (BASED ON STATUS)

			if ("Optimal".equalsIgnoreCase(result.status)) {
				// if (bestRuns < 4) optimal ,so add to final selection, otherwise sub-optimal,
				// so leave final selection blank
				result.finalSelectionRuns = bestRuns;
				result.finalSelectionSize = bestSize;
			} else {
				// if sub-optimal, then final selection should be blank
				result.finalSelectionRuns = null;
				result.finalSelectionSize = null;
			}

		}

		String matrixStr = matrixBuilder.toString();
		if (matrixStr.endsWith("\n"))
			matrixStr = matrixStr.substring(0, matrixStr.length() - 1);
		result.matrix = "Tag: " + record.tagNo + "\n-----------------\n" + matrixStr;

		return result;
	}

	public static class CableRecord {
		public String tagNo;
		public String layingMode;
		public String starterType;
		public double noOfCores;
		public double unitLength;
		public double cableLen; // Added to hold original 372 length
		public double voltage;
		public String loadType;
		public double fullLoadCurrent;
		public Double startingCurrent;
		public double fullLoadPf;
		public Double startingPf;
		public Double deratingFactor;
		public double upstreamRunningVoltageDrop;
		public double totalRunningVoltageDrop;

		public Double limitE12;
		public Double limitE13;
		public Double limitE14;
		public Double limitE15;
		public String pannel;

	}

	public static class OptimizationResult {
		public String status = "Fail";
		public int bestRuns = 0;
		public double bestSize = 0;
		public int minRun_Runs = 0;
		public double minRun_Size = 0;
		public String combinationString = "No Valid Option found";
		public double minCost = 0;
		public double bestLen = 0;
		public int outLugs = 0;
		public int outGlands = 0;
		public Integer finalSelectionRuns = null;
		public Double finalSelectionSize = null;
		public String matrix = "";
		public double bestCores = 0;
		public String bestCond = "";

	}
}