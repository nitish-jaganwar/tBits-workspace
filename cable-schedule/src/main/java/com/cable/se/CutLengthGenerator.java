package com.cable.se;

import java.util.ArrayList;
import java.util.List;

import com.cable.se.CableOptimizer.CableRecord;

public class CutLengthGenerator {

	/**
	 * Generates Cut Lengths with a 2% wastage buffer, matching the VBA logic
	 * exactly.
	 */
	public List<CutLengthRecord> generateOrderingCutLengths(CableRecord record,
			com.cable.se.CableOptimizer.OptimizationResult result) {
		List<CutLengthRecord> cutLengths = new ArrayList<>();

		// If optimization failed or was skipped, return an empty list
		if (result == null || result.bestRuns <= 0) {
			return cutLengths;
		}

		int runsToUse = 0;
		double sizeToUse = 0.0;

		// =========================================================================
		// 1. Check Final Selection First (VBA Lines 42-44)
		// =========================================================================
		if (result.finalSelectionRuns != null && result.finalSelectionSize != null) {
			runsToUse = result.finalSelectionRuns;
			sizeToUse = result.finalSelectionSize;
		}
		// =========================================================================
		// 2. Fallback to Optimized Result (VBA Lines 46-48)
		// =========================================================================
		else if (result.bestRuns > 0) {
			runsToUse = result.bestRuns;
			sizeToUse = result.bestSize;
		}

		// =========================================================================
		// 3. Generate the Rows (VBA Lines 56-65)
		// =========================================================================
		if (runsToUse > 0 && result.bestLen > 0) {

			// Calculate Unit Length AND ADD 2% WASTAGE BUFFER (* 1.02)
			double unitLenWithBuffer = (result.bestLen / runsToUse) * 1.02;

			// Round to 2 decimal places to match Excel's Round(unitLen, 2)
			double roundedUnitLen = Math.round(unitLenWithBuffer * 100.0) / 100.0;

			for (int i = 1; i <= runsToUse; i++) {
				CutLengthRecord cutRow = new CutLengthRecord();

				cutRow.orderingTagNumber = record.tagNo + " -" + i;
				cutRow.orgTagNumber = record.tagNo;
				cutRow.cableLength = roundedUnitLen;
				cutRow.diameterSize = sizeToUse;
				cutRow.core = result.bestCores;
				cutRow.cableType = result.bestCond;

				cutLengths.add(cutRow);
			}
		}

		return cutLengths;
	}
}
