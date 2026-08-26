package com.cable.se;

public class CableOptimizer {

    private static final double[] SIZES = { 2.5, 4, 6, 10, 16, 25, 35, 50, 70, 95, 120, 150, 185, 240, 300, 400};
    private static final String[] COND_TYPES = { "XLPE,Al", "XLPE,Cu" };

    private CalculationEngine engine;
    private String excelFilePath;

    public CableOptimizer(CalculationEngine engine, String excelFilePath) {
        this.engine = engine;
        this.excelFilePath = excelFilePath;
    }

    public OptimizationResult optimize(CableRecord record) {
        OptimizationResult result = new OptimizationResult();

        double minCost = Double.MAX_VALUE;
        int bestRuns = -1;
        double bestSize = -1;
        String bestCond = "";
        double bestCores = -1;
        
        int minRun_Runs = -1;
        double minRun_Size = -1;
        boolean isFirstValidCase = true;
        
        StringBuilder matrixBuilder = new StringBuilder();
        double lastValidSize = -1.0;

        // 1. ITERATIVE SEARCH: Build Matrix & Find Baseline Targets
        outerLoop: for (int runTest = 1; runTest <= 15; runTest++) {
            for (double size : SIZES) {
                double testCores = getRequiredCores(record.loadType, size);
                
                for (String cond : COND_TYPES) {
                    Double baseAmpacity = engine.getBaseCableAmpacity(record.layingMode, size, testCores, excelFilePath);
                    if (baseAmpacity == null) continue;

                    Double totalCurrentRating = engine.calculateCurrentRatingOfCable(baseAmpacity, runTest);
                    Double deratedCurrent = engine.calculateDeratedCurrentOfCable(record.deratingFactor, totalCurrentRating);
                    Double cableResistance = engine.getCableResistance(size, excelFilePath);
                    Double cableReactance = engine.getCableReactance(size, testCores, excelFilePath);
                    
                    if (cableResistance == null || cableReactance == null) continue;

                    Double startingVd = engine.calculateStartingVoltageDrop(record.startingCurrent, record.unitLength,
                            cableResistance, record.startingPf, cableReactance, runTest, record.voltage);
                    Double totalStartingVd = engine.calculateTotalStartingVoltageDrop(startingVd);

                    Double runningVd = engine.calculateRunningVoltageDrop(record.fullLoadCurrent, record.unitLength,
                            cableResistance, record.fullLoadPf, cableReactance, runTest, record.voltage);
                    Double totalRunningVd = engine.calculateTotalRunningVoltageDrop(runningVd, record.upstreamRunningVoltageDrop);

                    String capacityStatus = engine.validateCableCapacity(deratedCurrent, record.fullLoadCurrent);
                    String vdStatus = engine.validateVoltageDrop(totalStartingVd, runningVd, totalRunningVd,
                            record.limitE12, record.limitE13, record.limitE14, record.limitE15, record.pannel);
                    String finalValidation = engine.validateFinalCableSize(capacityStatus, vdStatus);

                    if ("OK".equalsIgnoreCase(finalValidation)) {
                        Double unitPrice = engine.fetchCablePrice(testCores, size, cond, excelFilePath);

                        if (unitPrice != null && unitPrice > 0) {
                            if (size == lastValidSize) continue outerLoop;
                            lastValidSize = size;

                            int actualRuns = getActualRuns(record.starterType, runTest);
                            double totalLength = record.unitLength * actualRuns;
                            double currentCost = unitPrice * totalLength;

                            if (isFirstValidCase) {
                                minRun_Runs = runTest;
                                minRun_Size = size;
                                isFirstValidCase = false;
                            }

                            String coreStr = formatWholeNumber(testCores);
                            String sizeStr = formatWholeNumber(size);
                            matrixBuilder.append(runTest).append("R x ").append(coreStr).append("C x ").append(sizeStr)
                                    .append(" Sqmm(Rs.").append((long) currentCost).append(cond).append(")\n");

                            if (currentCost < minCost) {
                                minCost = currentCost;
                                bestRuns = runTest;
                                bestSize = size;
                                bestCores = testCores;
                                bestCond = cond;
                            }
                            continue outerLoop;
                        }
                    }
                }
            }
        }

        // Generate Matrix String
        String matrixStr = matrixBuilder.toString();
        if (matrixStr.endsWith("\n")) matrixStr = matrixStr.substring(0, matrixStr.length() - 1);
        result.matrix = "Tag: " + record.tagNo + "\n-----------------\n" + matrixStr;

        // 2. RESOLVE ABSOLUTE FINAL SELECTION
        int targetRuns;
        double targetSize;
        String targetCond = (bestCond.isEmpty()) ? "XLPE,Al" : bestCond; // Default fallback

        if (record.isUserOverride && record.customRuns != null && record.customSize != null) {
            // A. User Override takes highest priority
            result.status = "User Override";
            targetRuns = record.customRuns;
            targetSize = record.customSize;
        } else if (bestRuns != -1) {
            if (bestRuns < 4) {
                // B. Optimal
                result.status = "Optimal";
                targetRuns = bestRuns;
                targetSize = bestSize;
            } else {
                // C. Sub Optimal (Force minimum runs and its associated size)
                result.status = "Sub Optimal";
                targetRuns = minRun_Runs;
                targetSize = minRun_Size;
            }
            // Preserve engine's historical lowest cost data for reference
            result.bestRuns = bestRuns;
            result.bestSize = bestSize;
            result.minCost = minCost;
            result.minRun_Runs = minRun_Runs;
            result.minRun_Size = minRun_Size;
            result.combinationString = bestRuns + " Runs of " + formatWholeNumber(bestCores) + " C - " + formatWholeNumber(bestSize) + " Sqmm - " + targetCond;
        } else {
            return result; // Fail state, no valid combinations and no override
        }

        // 3. LOCK FINAL VARIABLES & RECALCULATE 
        double targetCores = getRequiredCores(record.loadType, targetSize);
        int targetActualRuns = getActualRuns(record.starterType, targetRuns);
        
        result.finalSelectionRuns = targetRuns;
        result.finalSelectionSize = targetSize;
        result.bestCores = targetCores;
        result.bestCond = targetCond;
        result.bestLen = record.unitLength * targetActualRuns;

        result.outLugs = targetRuns * ((int) Math.ceil(targetCores)) * 2;
        result.outGlands = targetRuns * 2;

        double limit = (targetSize <= 70.0) ? 800.0 : 500.0;
        double unitLenWithBuffer = (result.bestLen / targetRuns) * 1.02; // Using targetRuns for joint grouping
        if (unitLenWithBuffer > limit) {
            int piecesPerRun = (int) Math.ceil(unitLenWithBuffer / limit);
            result.totalJoints = (piecesPerRun - 1) * targetRuns;
        } else {
            result.totalJoints = 0;
        }

        // 4. ONE-PASS RECALCULATION FOR THE LOCKED SELECTION
        Double baseAmpacity = engine.getBaseCableAmpacity(record.layingMode, targetSize, targetCores, excelFilePath);
        Double totalCurrentRating = (baseAmpacity != null) ? engine.calculateCurrentRatingOfCable(baseAmpacity, targetRuns) : null;
        Double deratedCurrent = engine.calculateDeratedCurrentOfCable(record.deratingFactor, totalCurrentRating);
        Double cableResistance = engine.getCableResistance(targetSize, excelFilePath);
        Double cableReactance = engine.getCableReactance(targetSize, targetCores, excelFilePath);
        
        Double startingVd = null, totalStartingVd = null, runningVd = null, totalRunningVd = null;
        String capacityStatus = "N/A", vdStatus = "N/A", finalValidation = "N/A";

        if (cableResistance != null && cableReactance != null) {
            startingVd = engine.calculateStartingVoltageDrop(record.startingCurrent, record.unitLength,
                    cableResistance, record.startingPf, cableReactance, targetRuns, record.voltage);
            totalStartingVd = engine.calculateTotalStartingVoltageDrop(startingVd);

            runningVd = engine.calculateRunningVoltageDrop(record.fullLoadCurrent, record.unitLength,
                    cableResistance, record.fullLoadPf, cableReactance, targetRuns, record.voltage);
            totalRunningVd = engine.calculateTotalRunningVoltageDrop(runningVd, record.upstreamRunningVoltageDrop);

            capacityStatus = engine.validateCableCapacity(deratedCurrent, record.fullLoadCurrent);
            vdStatus = engine.validateVoltageDrop(totalStartingVd, runningVd, totalRunningVd,
                    record.limitE12, record.limitE13, record.limitE14, record.limitE15, record.pannel);
            finalValidation = engine.validateFinalCableSize(capacityStatus, vdStatus);
        }

        // Map final values
        result.totalCurrentRating = formatAsNA(totalCurrentRating);
        result.cableResistance = formatAsNA(cableResistance);
        result.cableReactance = formatAsNA(cableReactance);
        result.startingVd = formatAsNA(startingVd);
        result.totalStartingVd = formatAsNA(totalStartingVd);
        result.runningVd = formatAsNA(runningVd);
        result.totalRunningVd = formatAsNA(totalRunningVd);
        result.deratedCurrent = (deratedCurrent != null) ? deratedCurrent : 0;
        result.capacityStatus = capacityStatus;
        result.vdStatus = vdStatus;
        result.finalValidation = finalValidation;

        result.combinationString = result.finalSelectionRuns + " Runs of " + formatWholeNumber(result.bestCores) + " C - " + formatWholeNumber(result.finalSelectionSize) + " Sqmm - " + targetCond;
        return result;
    }

    // --- HELPER METHODS --- //

    private double getRequiredCores(String loadType, double size) {
        String lType = (loadType != null) ? loadType.trim().toUpperCase() : "";
        if (lType.equals("MOTOR") || lType.equals("HEATER")) return 3.0;
        return (size <= 16.0) ? 4.0 : 3.5;
    }

    private int getActualRuns(String starterType, int baseRuns) {
        if ("STAR-DELTA".equalsIgnoreCase(starterType) || "STAR DELTA".equalsIgnoreCase(starterType)) {
            return baseRuns * 2;
        }
        return baseRuns;
    }

    private static String formatAsNA(Double value) {
        return (value != null) ? String.valueOf(value) : "N/A";
    }

    private static String formatWholeNumber(double value) {
        return (value == Math.floor(value)) ? String.valueOf((int) value) : String.valueOf(value);
    }

    // --- DATA MODELS --- //

    public static class CableRecord {
        public String tagNo;
        public String layingMode;
        public String starterType;
        public double noOfCores;
        public double unitLength;
        public double cableLen;
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
        
        // NEW FIELDS FOR MANUAL OVERRIDE
        public boolean isUserOverride = false;
        public Integer customRuns = null;
        public Double customSize = null;
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

        public String cableResistance = "N/A";
        public String totalRunningVd = "N/A";
        public String totalStartingVd = "N/A";
        public String totalCurrentRating = "N/A";
        public String cableReactance = "N/A";
        public String startingVd = "N/A";
        public String runningVd = "N/A";
        public double deratedCurrent = 0;
        public String capacityStatus = "N/A";
        public String vdStatus = "N/A";
        public String finalValidation = "N/A";
        public int totalJoints = 0;
    }
}