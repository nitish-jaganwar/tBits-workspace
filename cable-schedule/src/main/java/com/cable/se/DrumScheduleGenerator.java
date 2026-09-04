package com.cable.se;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

public class DrumScheduleGenerator {

    // Load the native C++ libraries for OR-Tools
    static {
        Loader.loadNativeLibraries();
    }

    public List<DrumRecord> generateLinkedDrumSchedule(List<CutLengthRecord> masterCutList, 
            double limitSmall, double limitLarge) {
        
        List<DrumRecord> schedule = new ArrayList<>();

        // Group cuts by combination
        Map<String, List<CutLengthRecord>> groupedCuts = new LinkedHashMap<>();
        for (CutLengthRecord cut : masterCutList) {
            String combo = cut.core + "C x " + cut.diameterSize + " Sqmm " + cut.cableType;
            groupedCuts.computeIfAbsent(combo, k -> new ArrayList<>()).add(cut);
        }

        for (Map.Entry<String, List<CutLengthRecord>> entry : groupedCuts.entrySet()) {
            String combination = entry.getKey();
            List<CutLengthRecord> cuts = entry.getValue();

            int numItems = cuts.size();
            int numBins = cuts.size();
            
            // CP-SAT requires integers. Multiply by 100 to handle 2 decimal places.
            long[] itemLengths = new long[numItems];
            for (int i = 0; i < numItems; i++) {
                itemLengths[i] = (long) Math.round(cuts.get(i).cableLength * 100.0);
            }
            
            double drumCapDouble = (cuts.get(0).diameterSize <= 70.0) ? limitSmall : limitLarge;
            long binCapacity = (long) Math.round(drumCapDouble * 100.0);

            CpModel model = new CpModel();

            // x[i][j] = 1 if item i is placed in bin j
            BoolVar[][] x = new BoolVar[numItems][numBins];
            for (int i = 0; i < numItems; i++) {
                for (int j = 0; j < numBins; j++) {
                    x[i][j] = model.newBoolVar("x_" + i + "_" + j);
                }
            }

            // y[j] = 1 if bin j is used
            BoolVar[] y = new BoolVar[numBins];
            for (int j = 0; j < numBins; j++) {
                y[j] = model.newBoolVar("y_" + j);
            }

            // Constraint A: Each item must be placed in exactly one bin.
            for (int i = 0; i < numItems; i++) {
                model.addExactlyOne(x[i]);
            }

            // Constraint B: The sum of items in a bin cannot exceed the bin's capacity.
            for (int j = 0; j < numBins; j++) {
                LinearExprBuilder weightExpr = LinearExpr.newBuilder();
                for (int i = 0; i < numItems; i++) {
                    weightExpr.addTerm(x[i][j], itemLengths[i]);
                }
                model.addLessOrEqual(weightExpr, LinearExpr.term(y[j], binCapacity));
            }

            // Objective: Minimize the number of bins used
            model.minimize(LinearExpr.sum(y));

            CpSolver solver = new CpSolver();
            CpSolverStatus status = solver.solve(model);

            if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
                int drumCounter = 1;
                for (int j = 0; j < numBins; j++) {
                    if (solver.booleanValue(y[j])) { 
                        List<CutLengthRecord> cutsInThisDrum = new ArrayList<>();
                        for (int i = 0; i < numItems; i++) {
                            if (solver.booleanValue(x[i][j])) {
                                CutLengthRecord cut = cuts.get(i);
                                // Apply the new naming format to the individual cut
                                cut.orderingDrumNumber = generateDrumName(cut, drumCounter);
                                cutsInThisDrum.add(cut);
                            }
                        }
                        //  new naming format to the final drum record = 3.5C-120Sqmm-Drum-01
                        String formattedDrumNo = generateDrumName(cutsInThisDrum.get(0), drumCounter);
                        schedule.add(createDrumRecord(combination, formattedDrumNo, cutsInThisDrum, drumCapDouble));
                        drumCounter++;
                    }
                }
            } else {
                System.err.println("The OR-Tools solver could not find a solution for combination: " + combination);
            }
        }
        return schedule;
    }

    // Helper method to format the drum name cleanly
    private String generateDrumName(CutLengthRecord cut, int drumNo) {
        return String.format("%sC-%sSqmm-Drum-%02d", 
                formatNumber(cut.core), 
                formatNumber(cut.diameterSize), 
                drumNo);
    }

    //remove the ".0" from round numbers (e.g., 120.0 -> 120)
    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private DrumRecord createDrumRecord(String combo, String formattedDrumNo, List<CutLengthRecord> cuts, double maxLimit) {
        DrumRecord drum = new DrumRecord();
        drum.cableCombination = combo;
        drum.drumNo = formattedDrumNo;
        drum.maxDrumLimit = maxLimit;
        drum.totalPieces = cuts.size();

        CutLengthRecord referenceCut = cuts.get(0);
        drum.cableType = referenceCut.cableType;
        drum.core = referenceCut.core;
        drum.diameterSize = referenceCut.diameterSize;
        
        double totalLen = 0.0;
        Map<String, Integer> cutCounts = new LinkedHashMap<>();
        Map<String, Double> cutLengths = new LinkedHashMap<>();

        for (CutLengthRecord c : cuts) {
            totalLen += c.cableLength;
            String tagDetails = c.orderingTagNumber; 
            cutCounts.put(tagDetails, cutCounts.getOrDefault(tagDetails, 0) + 1);
            cutLengths.put(tagDetails, c.cableLength);
        }

        drum.exactOrderedLength = Math.round(totalLen * 100.0) / 100.0;

        StringBuilder details = new StringBuilder();
        for (String tag : cutCounts.keySet()) {
            details.append(tag).append(": ")
                   .append(cutCounts.get(tag)).append(" cut x ")
                   .append(cutLengths.get(tag)).append("m\n");
        }
        drum.cutDetails = details.toString().trim();
        return drum;
    }
}