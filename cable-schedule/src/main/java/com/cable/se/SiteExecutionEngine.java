package com.cable.se;

import java.util.List;

public class SiteExecutionEngine {

	/**
	 * Site execution stage algorithm: Calculates live dynamic balance using
	 * Manufacturer Drum Length (or Ordered Length fallback) and actual site
	 * execution (Slit = Yes).
	 */
	public void processSiteExecution(List<CutLengthRecord> cutList, List<DrumRecord> drumSchedule) {

		for (DrumRecord drum : drumSchedule) {

			// 1. Set base physical capacity
			// Fallback Priority: Manufacturer Actual Length -> Exact Ordered Length -> Max
			// Drum Limit
			double baseCapacity;
			if (drum.manufacturerActualDrumLength > 0.0) {
				baseCapacity = drum.manufacturerActualDrumLength;
			} else if (drum.exactOrderedLength > 0.0) {
				baseCapacity = drum.exactOrderedLength;
			} else {
				baseCapacity = drum.maxDrumLimit;
			}

			double totalSlittedLength = 0.0;
			int executedSlitCount = 0;
			int totalPlannedCutsInDrum = 0;

			// 2. Process cuts linked to this drum
			for (CutLengthRecord cut : cutList) {
				if (isCutLinkedToDrum(cut, drum)) {
					totalPlannedCutsInDrum++;

					if ("Yes".equalsIgnoreCase(cut.slitStatus)) {
						// Priority: Site measured Actual Length -> Initial Planned Length
						double consumedLength = (cut.actualLength > 0.0) ? cut.actualLength : cut.cableLength;

						totalSlittedLength += consumedLength;
						executedSlitCount++;
					}
				}
			}

			// 3.Dynamic Balance Calculation
			double rawBalance = baseCapacity - totalSlittedLength;
			drum.drumBalancedLength = Math.max(0.0, Math.round(rawBalance * 100.0) / 100.0);

			// 4. PRESERVE Total Planned Pieces (DO NOT overwrite totalPieces with executed
			// count)
			// If you want to track executed count, add a new field like drum.executedPieces
			// = executedSlitCount;
			if (totalPlannedCutsInDrum > 0) {
				drum.totalPieces = totalPlannedCutsInDrum;
			}
		}
	}

	/**
	 * Checks matching between CutLengthRecord and DrumRecord
	 */
	private boolean isCutLinkedToDrum(CutLengthRecord cut, DrumRecord drum) {
		if (cut.orderingDrumNumber == null || drum.drumNo == null) {
			return false;
		}

		// 1. Primary Check: Match by Manufacturer Drum No (Serial No) if present
		if (cut.manufacturerDrumNumber != null && !cut.manufacturerDrumNumber.trim().isEmpty()
				&& drum.manufacturerDrumNo != null && !drum.manufacturerDrumNo.trim().isEmpty()) {

			return cut.manufacturerDrumNumber.trim().equalsIgnoreCase(drum.manufacturerDrumNo.trim());
		}

		// 2. Secondary Check: Match by Ordering Drum Number
		boolean matchOrderingNo = cut.orderingDrumNumber.trim().equalsIgnoreCase(drum.drumNo.trim());

		// 3. Validation: Verify Combination match only if needed
		if (matchOrderingNo && drum.cableCombination != null) {
			String cutCombo = cut.core + "C x " + cut.diameterSize + " Sqmm " + cut.cableType;
			return cutCombo.equalsIgnoreCase(drum.cableCombination.trim());
		}

		return false;
	}
}
//public class SiteExecutionEngine {
//
//    public static class ExecutionResult {
//        public double totalInstalledLength = 0.0;
//        public double totalPlannedLengthUsed = 0.0;
//        public double totalWastageLength = 0.0;
//        public List<CutLengthRecord> updatedMasterCutList = new ArrayList<>();
//        public List<DrumRecord> updatedDrumSchedule = new ArrayList<>();
//    }
//
//    public ExecutionResult processSiteExecution(
//            List<CutLengthRecord> masterCutList,
//            List<DrumRecord> currentDrumSchedule) {
//
//        ExecutionResult result = new ExecutionResult();
//
//        // 1. Drum reset (Executed values counter)
//        for (DrumRecord drum : currentDrumSchedule) {
//           // drum.exactOrderedLength = 0.0; // Actual Executed Length on Drum
//            drum.totalPieces = 0;          // Total Slit Pieces Count
//            drum.drumBalancedLength = 0.0;
//        }
//
//        // 2. Master Cut List Process
//        for (CutLengthRecord cut : masterCutList) {
//            
//            DrumRecord targetDrum = findDrumForCut(currentDrumSchedule, cut);
//
//            if ("Yes".equalsIgnoreCase(cut.slitStatus)) {
//                cut.executionStatus = "Completed";
//                
//                // Actual Slit Length
//                double effectiveLength = (cut.actualLength > 0.0) ? cut.actualLength : cut.cableLength;
//                
//                result.totalInstalledLength += effectiveLength;
//                result.totalPlannedLengthUsed += cut.cableLength;
//
//                // SIRF SLIT STATUS = "YES" PAR DRUM SE CUT DEDUCT HOGA
//                if (targetDrum != null) {
//                    targetDrum.exactOrderedLength += effectiveLength;
//                    targetDrum.totalPieces += 1;
//                }
//            } else {
//                cut.executionStatus = "Pending";
//                // Pending cuts physical drum length ko kam nahi karenge
//            }
//
//            result.updatedMasterCutList.add(cut);
//        }
//
//        // 3. Drum Balance Calculation (Based ONLY on Slitted Cable)
//        for (DrumRecord drum : currentDrumSchedule) {
//            double capacity = (drum.manufacturerActualDrumLength > 0.0) 
//                    ? drum.manufacturerActualDrumLength 
//                    : drum.maxDrumLimit;
//
//            // Physical Remaining Balance = Actual Capacity - Total Slitted Length
//            drum.drumBalancedLength = round(capacity - drum.exactOrderedLength);
//            drum.exactOrderedLength = round(drum.exactOrderedLength);
//        }
//
//        result.totalWastageLength = round(result.totalInstalledLength - result.totalPlannedLengthUsed);
//        result.updatedDrumSchedule = new ArrayList<>(currentDrumSchedule);
//        return result;
//    }
//
//    private DrumRecord findDrumForCut(List<DrumRecord> drumList, CutLengthRecord cut) {
//        String mfgNo = cut.manufacturerDrumNumber != null ? cut.manufacturerDrumNumber.trim() : "";
//        String ordNo = cut.orderingDrumNumber != null ? cut.orderingDrumNumber.trim() : "";
//
//        for (DrumRecord drum : drumList) {
//            String dNo = drum.drumNo != null ? drum.drumNo.trim() : "";
//            String dMfgNo = drum.manufacturerDrumNo != null ? drum.manufacturerDrumNo.trim() : "";
//
//            if (!mfgNo.isEmpty() && (mfgNo.equalsIgnoreCase(dMfgNo) || mfgNo.equalsIgnoreCase(dNo))) {
//                return drum;
//            }
//            if (!ordNo.isEmpty() && (ordNo.equalsIgnoreCase(dNo) || ordNo.equalsIgnoreCase(dMfgNo))) {
//                return drum;
//            }
//        }
//        return null;
//    }
//
//    private double round(double val) {
//        return Math.round(val * 100.0) / 100.0;
//    }
//}