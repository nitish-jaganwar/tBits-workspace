package com.cable.se;

import java.util.ArrayList;
import java.util.List;

import com.cable.se.CableOptimizer.CableRecord;

public class CutLengthGenerator {

	/**
	 * Generates Cut Lengths with a 2% wastage buffer. Automatically splits cuts
	 * exceeding drum limits (800m for <=70sqmm, 500m for >70sqmm) and appends
	 * suffix labels (A, B, C...) for joints.
	 */
	public List<CutLengthRecord> generateOrderingCutLengths(CableRecord record,
			com.cable.se.CableOptimizer.OptimizationResult result) {

		System.out.println("Generating Cut Lengths for Tag: " + record.tagNo + ", Best Runs: " + result.bestRuns
				+ ", Best Size: " + result.bestSize + ", Best Length: " + result.bestLen);
		
		List<CutLengthRecord> cutLengths = new ArrayList<>();

//		if (result == null || result.bestRuns <= 0) {
//			return cutLengths;
//		}
		if (result == null || (result.bestRuns <= 0 && (result.finalSelectionRuns == null || result.finalSelectionRuns <= 0))) {
	        return cutLengths;
	    }
		int runsToUse = 0;
		double sizeToUse = 0.0;

		// 1. Check Final Selection First
		if (result.finalSelectionRuns != null && result.finalSelectionSize != null) {
			runsToUse = result.finalSelectionRuns;
			sizeToUse = result.finalSelectionSize;
		}
		// 2. Fallback to Optimized Result
		else if (result.bestRuns > 0) {
			runsToUse = result.bestRuns;
			sizeToUse = result.bestSize;
		}
		System.out.println("runs to use " + runsToUse);
		// 3. Generate Cut Lengths with Splitting Logic
		if (runsToUse > 0 && result.bestLen > 0) {

			// Calculate Unit Length with 2% Wastage Buffer
			double unitLenWithBuffer = (result.bestLen / runsToUse) * 1.02;
			double roundedUnitLen = Math.round(unitLenWithBuffer * 100.0) / 100.0;

			System.out.println("Unit Length with 2% Buffer: " + unitLenWithBuffer + ", Rounded: " + roundedUnitLen);
			// Capacity threshold determination
			double maxDrumLimit = (sizeToUse <= 70.0) ? 800.0 : 500.0;
			
			for (int run = 1; run <= runsToUse; run++) {

				if (roundedUnitLen > maxDrumLimit) {
					// --- CASE A: Length > Limit (Split into Pieces with Suffix A, B, C) ---
					double remainingLength = roundedUnitLen;
					int pieceIndex = 0;

					while (remainingLength > 0.001) {
						double currentPieceLen = Math.min(remainingLength, maxDrumLimit);
						currentPieceLen = Math.round(currentPieceLen * 100.0) / 100.0;

						CutLengthRecord cutRow = new CutLengthRecord();
						// Suffix Tagging: -1A, -1B, -1C...
						char suffix = (char) ('A' + pieceIndex);
						cutRow.orderingTagNumber = record.tagNo + " -" + run + suffix;
						cutRow.orgTagNumber = record.tagNo;
						cutRow.cableLength = currentPieceLen;
						cutRow.diameterSize = sizeToUse;
						cutRow.core = result.bestCores;
						cutRow.cableType = result.bestCond;
						cutRow.slitStatus = "No";
						cutRow.actualLength = 0.0;

						cutLengths.add(cutRow);

						remainingLength -= currentPieceLen;
						pieceIndex++;
					}
				} else {
					// --- CASE B: Normal Length (No Splitting Required) ---
					CutLengthRecord cutRow = new CutLengthRecord();
					cutRow.orderingTagNumber = record.tagNo + " -" + run;
					cutRow.orgTagNumber = record.tagNo;
					cutRow.cableLength = roundedUnitLen;
					cutRow.diameterSize = sizeToUse;
					cutRow.core = result.bestCores;
					cutRow.cableType = result.bestCond;
					cutRow.slitStatus = "No";
					cutRow.actualLength = 0.0;

					cutLengths.add(cutRow);
				}
			}
		}

		return cutLengths;
	}
}