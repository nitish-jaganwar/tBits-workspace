package com.cable.se;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

public class SiteExecutionEngine {

	public static final String STATUS_YET_TO_BE_CUT = "yetToBeCut";
	public static final String STATUS_ALREADY_CUT = "alreadyCut";
	public static final String STATUS_ASSIGNED = "drumAssigned";
	public static final String STATUS_UNASSIGNED = "unassigned";
	public static final String RECORD_STATUS_ACTIVE = "Active";
	
	//Solver Timeout (in seconds)
	public static final double SOLVER_MAX_TIME_SECONDS = 300.0;
	// --- JOINT CONFIGURATION ---
	public static double JOINT_ALLOWED_THRESHOLD_SQMM = 120.0;
	public static double MIN_JOINT_SEGMENT_METRES = 5.0;

	static {
		Loader.loadNativeLibraries();
	}

	public static PackingResult executeSiteBinPacking(List<CutLengthRecord> allCuts, List<DrumRecord> allDrums) {
		List<CutLengthRecord> activeCuts = new ArrayList<>();
		List<CutLengthRecord> ignoredCuts = new ArrayList<>();

		for (CutLengthRecord cut : allCuts) {
			if (STATUS_YET_TO_BE_CUT.equalsIgnoreCase(cut.cuttingStatus)
					&& RECORD_STATUS_ACTIVE.equalsIgnoreCase(cut.status)) {
				activeCuts.add(cut);
			} else {
				ignoredCuts.add(cut);
			}
		}

		// Recombine theoretical pre-splits into full lengths before solving
		recombinePreSplitCuts(activeCuts);

		// drum remaining capacity calculate (alreadyCut minus )
		Map<String, Double> remainingDrumCapacity = new HashMap<>();
		for (DrumRecord drum : allDrums) {
			double baseCapacity = drum.manufacturerActualDrumLength > 0 ? drum.manufacturerActualDrumLength
					: drum.exactOrderedLength;

			double alreadyCutLength = 0.0;
			for (CutLengthRecord cut : allCuts) {
				if (STATUS_ALREADY_CUT.equalsIgnoreCase(cut.cuttingStatus)
						&& drum.drumNo.equalsIgnoreCase(cut.orderingDrumNumber)) {
					alreadyCutLength += (cut.actualLength > 0 ? cut.actualLength : cut.cableLength);
				}
			}

			double availableForSolver = Math.max(0.0, baseCapacity - alreadyCutLength);
			remainingDrumCapacity.put(drum.drumNo, availableForSolver);
		}

		Map<String, List<CutLengthRecord>> cutsBySpec = new HashMap<>();
		for (CutLengthRecord cut : activeCuts) {
			String spec = getSpecKey(cut.cableType, cut.core, cut.diameterSize);
			cutsBySpec.computeIfAbsent(spec, k -> new ArrayList<>()).add(cut);
		}

		Map<String, List<DrumRecord>> drumsBySpec = new HashMap<>();
		for (DrumRecord drum : allDrums) {
			String spec = getSpecKey(drum.cableType, drum.core, drum.diameterSize);
			drumsBySpec.computeIfAbsent(spec, k -> new ArrayList<>()).add(drum);
		}

		List<CutLengthRecord> finalProcessedActiveCuts = new ArrayList<>();

		for (Map.Entry<String, List<CutLengthRecord>> entry : cutsBySpec.entrySet()) {
			String spec = entry.getKey();
			List<CutLengthRecord> specCuts = entry.getValue();
			List<DrumRecord> specDrums = drumsBySpec.getOrDefault(spec, new ArrayList<>());

			if (specDrums.isEmpty()) {
				for (CutLengthRecord cut : specCuts) {
					markUnassigned(cut);
					finalProcessedActiveCuts.add(cut);
				}
				continue;
			}

			// Pass remainingDrumCapacity into the solver partition
			List<CutLengthRecord> solvedCuts = solvePartition(specCuts, specDrums, remainingDrumCapacity);
			finalProcessedActiveCuts.addAll(solvedCuts);
		}

		// 3. Combine active solved cuts and ignored/alreadyCut cuts
		List<CutLengthRecord> finalCutList = new ArrayList<>();
		finalCutList.addAll(finalProcessedActiveCuts);
		finalCutList.addAll(ignoredCuts);
		finalCutList.sort(Comparator.comparing(c -> c.orderingTagNumber));

		// 4. Final calculation using ALL cuts (pending + finished)
		updateDrumLeftovers(allDrums, finalCutList);

		return new PackingResult(finalCutList, allDrums);
	}

	private static List<CutLengthRecord> solvePartition(List<CutLengthRecord> cuts, List<DrumRecord> drums,
			Map<String, Double> remainingDrumCapacity) {

		int numCuts = cuts.size();
		int numDrums = drums.size();

		long[] cutLengths = new long[numCuts];
		boolean[] jointAllowed = new boolean[numCuts];
		for (int i = 0; i < numCuts; i++) {
			// Use cableLength as primary demand; if actualLength was manually entered (>0),
			// honor it
			double effectiveLength = cuts.get(i).actualLength > 0 ? cuts.get(i).actualLength : cuts.get(i).cableLength;
			cutLengths[i] = Math.round(effectiveLength * 100.0);
			jointAllowed[i] = cuts.get(i).diameterSize >= JOINT_ALLOWED_THRESHOLD_SQMM;
		}

		long[] drumCapacities = new long[numDrums];
		for (int j = 0; j < numDrums; j++) {
			DrumRecord drum = drums.get(j);
			double fallbackCapacity = drum.manufacturerActualDrumLength > 0 ? drum.manufacturerActualDrumLength
					: drum.exactOrderedLength;

			// Read the remaining available capacity after subtracting any 'alreadyCut' cuts
			double capacity = remainingDrumCapacity != null
					? remainingDrumCapacity.getOrDefault(drum.drumNo, fallbackCapacity)
					: fallbackCapacity;

			drumCapacities[j] = Math.round(capacity * 100.0);
		}

		CpModel model = new CpModel();
		BoolVar[] served = new BoolVar[numCuts];
		BoolVar[] joint = new BoolVar[numCuts];
		BoolVar[][] use = new BoolVar[numCuts][numDrums];
		IntVar[][] amt = new IntVar[numCuts][numDrums];

		long minJointSegmentUnits = Math.round(MIN_JOINT_SEGMENT_METRES * 100.0);

		for (int i = 0; i < numCuts; i++) {
			served[i] = model.newBoolVar("served_" + i);
			joint[i] = model.newBoolVar("joint_" + i);
			model.addLessOrEqual(joint[i], served[i]);

			if (!jointAllowed[i]) {
				model.addEquality(joint[i], 0);
			}

			LinearExprBuilder sumAmt = LinearExpr.newBuilder();
			LinearExprBuilder sumUse = LinearExpr.newBuilder();

			for (int j = 0; j < numDrums; j++) {
				use[i][j] = model.newBoolVar("use_" + i + "_" + j);
				long ub = Math.min(cutLengths[i], drumCapacities[j]);
				amt[i][j] = model.newIntVar(0, Math.max(ub, 0), "amt_" + i + "_" + j);

				model.addLessOrEqual(amt[i][j], LinearExpr.term(use[i][j], ub));

				// Minimum Joint Segment logic preventing useless short fractions
				LinearExprBuilder minBound = LinearExpr.newBuilder();
				minBound.addTerm(use[i][j], minJointSegmentUnits);
				minBound.addTerm(joint[i], minJointSegmentUnits);
				minBound.add(-minJointSegmentUnits);
				model.addGreaterOrEqual(amt[i][j], minBound);

				sumAmt.addTerm(amt[i][j], 1);
				sumUse.addTerm(use[i][j], 1);
			}

			model.addEquality(sumAmt, LinearExpr.term(served[i], cutLengths[i]));

			// If fully served (1), uses 1 drum. If jointed (1), uses 1 additional drum
			// (total 2).
			LinearExprBuilder rhs = LinearExpr.newBuilder();
			rhs.addTerm(served[i], 1);
			rhs.addTerm(joint[i], 1);
			model.addEquality(sumUse, rhs);
		}

		// Drum Capacities
		for (int j = 0; j < numDrums; j++) {
			LinearExprBuilder weightInDrum = LinearExpr.newBuilder();
			for (int i = 0; i < numCuts; i++) {
				weightInDrum.addTerm(amt[i][j], 1);
			}
			model.addLessOrEqual(weightInDrum, drumCapacities[j]);
		}

		// Objective: Heavily prioritize maximizing served length; apply a tiny penalty
		// (-1) per joint to minimize them.
		LinearExprBuilder objective = LinearExpr.newBuilder();
		for (int i = 0; i < numCuts; i++) {
			objective.addTerm(served[i], cutLengths[i] * 10L);
			objective.addTerm(joint[i], -1L);
		}
		model.maximize(objective);

		CpSolver solver = new CpSolver();

	
		//solver.getParameters().setMaxTimeInSeconds(25.0);
		solver.getParameters().setMaxTimeInSeconds(SOLVER_MAX_TIME_SECONDS);

		// Use all available cores minus 1 to avoid freezing the system
		int availableCores = Runtime.getRuntime().availableProcessors();
		solver.getParameters().setNumWorkers(Math.max(1, availableCores - 1));
		
		CpSolverStatus status = solver.solve(model);

		List<CutLengthRecord> resultingCuts = new ArrayList<>();

		if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
			for (int i = 0; i < numCuts; i++) {
				CutLengthRecord originalCut = cuts.get(i);

				if (!solver.booleanValue(served[i])) {
					markUnassigned(originalCut);
					resultingCuts.add(originalCut);
					continue;
				}

				// Gather drum allocations for this cut
				List<DrumAllocation> allocations = new ArrayList<>();
				for (int j = 0; j < numDrums; j++) {
					long allocatedAmt = solver.value(amt[i][j]);
					if (allocatedAmt > 0) {
						allocations.add(new DrumAllocation(drums.get(j), allocatedAmt / 100.0));
					}
				}

				if (allocations.size() == 1) {
					// Standard 1-to-1 Assignment
					originalCut.orderingDrumNumber = allocations.get(0).drum.drumNo;
					originalCut.manufacturerDrumNumber = allocations.get(0).drum.manufacturerDrumNo;
					originalCut.DrumAssignmentStatus = STATUS_ASSIGNED;
					resultingCuts.add(originalCut);
				} else if (allocations.size() == 2) {
					// Joint Split: Create two new explicit objects and discard the original unsplit
					// one
					CutLengthRecord part1 = duplicateCut(originalCut, "-part-1", allocations.get(0).length,
							allocations.get(0).drum);
					CutLengthRecord part2 = duplicateCut(originalCut, "-part-2", allocations.get(1).length,
							allocations.get(1).drum);
					resultingCuts.add(part1);
					resultingCuts.add(part2);
				}
			}
		} else {
			// Fail-safe: if solver completely fails, mark all as unassigned.
			for (CutLengthRecord c : cuts) {
				markUnassigned(c);
				resultingCuts.add(c);
			}
		}

		System.out.println("Spec: " + cuts.get(0).diameterSize + " | Solver Status: " + status);
		return resultingCuts;
	}

	private static void updateDrumLeftovers(List<DrumRecord> drums, List<CutLengthRecord> allCuts) {
		Map<String, List<CutLengthRecord>> drumAssignments = new HashMap<>();
		for (DrumRecord d : drums)
			drumAssignments.put(d.drumNo, new ArrayList<>());

		// Group ALL cuts (finished and pending) by their assigned drum
		for (CutLengthRecord cut : allCuts) {
			if (cut.orderingDrumNumber != null && !cut.orderingDrumNumber.isEmpty()) {
				List<CutLengthRecord> list = drumAssignments.get(cut.orderingDrumNumber);
				if (list != null)
					list.add(cut);
			}
		}

		for (DrumRecord drum : drums) {
			List<CutLengthRecord> assigned = drumAssignments.get(drum.drumNo);
			drum.totalPieces = assigned.size();

			double totalPlannedUsed = 0.0;
			double totalPhysicallyCut = 0.0;
			StringBuilder detailsBuilder = new StringBuilder();

			for (int i = 0; i < assigned.size(); i++) {
				CutLengthRecord c = assigned.get(i);
				double effectiveLength = c.actualLength > 0 ? c.actualLength : c.cableLength;

				// 1. PLANNED: Add every assigned cut to the total plan
				totalPlannedUsed += effectiveLength;

				// 2. ACTUAL: Only add to the physical cut total if it has actually been cut
				if (STATUS_ALREADY_CUT.equalsIgnoreCase(c.cuttingStatus)) {
					totalPhysicallyCut += effectiveLength;
				}

				detailsBuilder.append(c.orderingTagNumber).append(": 1 cut x ").append(effectiveLength).append("m [")
						.append(c.cuttingStatus).append("]");
				if (i < assigned.size() - 1)
					detailsBuilder.append("\n");
			}

			drum.cutDetails = detailsBuilder.toString();

			// Baseline physical capacity (e.g., 26m)
			double physicalBaseCapacity = drum.manufacturerActualDrumLength > 0 ? drum.manufacturerActualDrumLength
					: drum.exactOrderedLength;

			// --- CALCULATE ACTUAL AVAILABLE Inventory)
			drum.actualAvailableLength = Math.round((physicalBaseCapacity - totalPhysicallyCut) * 100.0) / 100.0;
			if (drum.actualAvailableLength < 0)
				drum.actualAvailableLength = 0.0;

			// CALCULATE PLANNED LEFTOVER (expected at the end of the job)
			drum.plannedLeftOverLength = Math.round((physicalBaseCapacity - totalPlannedUsed) * 100.0) / 100.0;
		}
	}

	private static void recombinePreSplitCuts(List<CutLengthRecord> activeCuts) {
		Map<String, CutLengthRecord> mergedCuts = new HashMap<>();
		List<CutLengthRecord> toRemove = new ArrayList<>();

		for (CutLengthRecord cut : activeCuts) {
			String tag = cut.orderingTagNumber != null ? cut.orderingTagNumber.trim() : "";

			if (tag.matches(".*-\\d+[A-Z]$")) {
				String baseTag = tag.substring(0, tag.length() - 1);

				System.out.println("Recombining split cut: " + tag + " into base tag: " + baseTag);
				if (mergedCuts.containsKey(baseTag)) {
					CutLengthRecord existingBase = mergedCuts.get(baseTag);

					existingBase.cableLength += cut.cableLength;

					toRemove.add(cut);
				} else {
					cut.orderingTagNumber = baseTag;
					mergedCuts.put(baseTag, cut);
				}
			}
		}
		activeCuts.removeAll(toRemove);
	}

	private static String getSpecKey(String type, double core, double dia) {
		String cleanType = type != null ? type.trim().toUpperCase() : "";
		return cleanType + "|" + core + "|" + dia;
	}

	private static void markUnassigned(CutLengthRecord cut) {
		cut.DrumAssignmentStatus = STATUS_UNASSIGNED;
		cut.orderingDrumNumber = "";
		cut.manufacturerDrumNumber = "";
	}

	// Helper Factory to clone cut records for splits
	private static CutLengthRecord duplicateCut(CutLengthRecord source, String suffix, double specificLength,
			DrumRecord assignedDrum) {
		CutLengthRecord clone = new CutLengthRecord();
		clone.orderingTagNumber = source.orderingTagNumber + suffix;
		clone.orgTagNumber = source.orgTagNumber;

		clone.diameterSize = source.diameterSize;
		clone.core = source.core;
		clone.cableType = source.cableType;
		clone.cuttingStatus = source.cuttingStatus;
		clone.status = source.status;
		clone.wastage = source.wastage;
		clone.cableLength = specificLength;
		clone.actualLength = STATUS_ALREADY_CUT.equalsIgnoreCase(source.cuttingStatus) ? specificLength : 0.0;
		clone.orderingDrumNumber = assignedDrum.drumNo;
		clone.manufacturerDrumNumber = assignedDrum.manufacturerDrumNo;
		clone.DrumAssignmentStatus = STATUS_ASSIGNED;

		return clone;
	}

	// Internal class for handling solver loop outputs cleanly
	private static class DrumAllocation {
		DrumRecord drum;
		double length;

		DrumAllocation(DrumRecord drum, double length) {
			this.drum = drum;
			this.length = length;
		}
	}

	public static class PackingResult {
		public List<CutLengthRecord> processedCuts;
		public List<DrumRecord> processedDrums;

		public PackingResult(List<CutLengthRecord> processedCuts, List<DrumRecord> processedDrums) {
			this.processedCuts = processedCuts;
			this.processedDrums = processedDrums;
		}
	}

}
