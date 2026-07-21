package com.cable.se;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DrumScheduleGenerator {

	/*
	 * public List<DrumRecord> generateLinkedDrumSchedule(List<CutLengthRecord>
	 * allCutLengths) { List<DrumRecord> drumSchedule = new ArrayList<>();
	 * 
	 * // 1. Group cuts by Cable Combination (Core + Size + Type) // LinkedHashMap
	 * keeps the order in which they were processed Map<String,
	 * List<CutLengthRecord>> groupedCuts = new LinkedHashMap<>();
	 * 
	 * for (CutLengthRecord cut : allCutLengths) { if (cut.cableLength <= 0 ||
	 * cut.diameterSize <= 0) continue;
	 * 
	 * // Format numbers to avoid "3.0C" instead of "3C" String formatCore =
	 * (cut.core == Math.floor(cut.core)) ? String.valueOf((int) cut.core) :
	 * String.valueOf(cut.core); String formatSize = (cut.diameterSize ==
	 * Math.floor(cut.diameterSize)) ? String.valueOf((int) cut.diameterSize) :
	 * String.valueOf(cut.diameterSize);
	 * 
	 * // e.g., "3C x 2.5 Sqmm XLPE,Cu" String cableDesc = formatCore + "C x " +
	 * formatSize + " Sqmm " + cut.cableType;
	 * 
	 * groupedCuts.computeIfAbsent(cableDesc, k -> new ArrayList<>()).add(cut); }
	 * 
	 * // 2. Process each group into exactly ONE custom ordered drum int
	 * globalDrumCounter = 1;
	 * 
	 * for (Map.Entry<String, List<CutLengthRecord>> entry : groupedCuts.entrySet())
	 * { String cableDesc = entry.getKey(); List<CutLengthRecord> cutsForCombo =
	 * entry.getValue();
	 * 
	 * DrumRecord drum = new DrumRecord(); drum.cableCombination = cableDesc;
	 * drum.drumNo = "Drum - " + globalDrumCounter++; drum.totalPieces =
	 * cutsForCombo.size();
	 * 
	 * double totalLen = 0.0;
	 * 
	 * // Dictionary to count identical cuts for the summary string // Key format:
	 * "OrgTag|RunLength" Map<String, Integer> tagSummary = new LinkedHashMap<>();
	 * 
	 * for (CutLengthRecord cut : cutsForCombo) { totalLen += cut.cableLength;
	 * 
	 * // BACK-LINKING: Assign this drum number back to the cut record!
	 * cut.orderingDrumNumber = drum.drumNo;
	 * 
	 * String tagKey = cut.orgTagNumber + "|" + cut.cableLength;
	 * tagSummary.put(tagKey, tagSummary.getOrDefault(tagKey, 0) + 1); }
	 * 
	 * // Round total length to 2 decimal places drum.exactOrderedLength =
	 * Math.round(totalLen * 100.0) / 100.0;
	 * 
	 * // 3. Build the VBA-style Cut Details String StringBuilder detailsStr = new
	 * StringBuilder(); for (Map.Entry<String, Integer> tagEntry :
	 * tagSummary.entrySet()) { String[] parts = tagEntry.getKey().split("\\|");
	 * String tag = parts[0]; String len = parts[1]; int count =
	 * tagEntry.getValue();
	 * 
	 * detailsStr.append(tag).append(": ").append(count).append(" cuts x ").append(
	 * len).append("m each\n"); }
	 * 
	 * // Remove the trailing newline character if (detailsStr.length() > 0) {
	 * detailsStr.setLength(detailsStr.length() - 1); } drum.cutDetails =
	 * detailsStr.toString();
	 * 
	 * drumSchedule.add(drum); }
	 * 
	 * return drumSchedule; }
	 */

	// Update this method signature to accept limits
	public List<DrumRecord> generateLinkedDrumSchedule(List<CutLengthRecord> masterCutList, double limitSmall,
			double limitLarge) {
		List<DrumRecord> schedule = new ArrayList<>();

		// Grouping cuts by combination
		Map<String, List<CutLengthRecord>> groupedCuts = new LinkedHashMap<>();
		for (CutLengthRecord cut : masterCutList) {
			String combo = cut.core + "C x " + cut.diameterSize + " Sqmm " + cut.cableType;
			groupedCuts.computeIfAbsent(combo, k -> new ArrayList<>()).add(cut);
		}

		for (Map.Entry<String, List<CutLengthRecord>> entry : groupedCuts.entrySet()) {
			String combination = entry.getKey();
			List<CutLengthRecord> cuts = entry.getValue();

			int drumCounter = 1;
			double currentDrumLength = 0.0;
			List<CutLengthRecord> currentDrumCuts = new ArrayList<>();

			for (CutLengthRecord cut : cuts) {
				// 🔥 MAIN LOGIC: Size check karke limit decide karna
				double maxDrumCapacity = (cut.diameterSize <= 70.0) ? limitSmall : limitLarge;

				if (currentDrumLength + cut.cableLength > maxDrumCapacity && !currentDrumCuts.isEmpty()) {
					// Puraana drum save karte time limit pass kar rahe hain
					schedule.add(createDrumRecord(combination, drumCounter, currentDrumCuts, maxDrumCapacity));
					drumCounter++;
					currentDrumLength = 0.0;
					currentDrumCuts.clear();
				}

				cut.orderingDrumNumber = "Drum - " + drumCounter;
				currentDrumCuts.add(cut);
				currentDrumLength += cut.cableLength;
			}

			if (!currentDrumCuts.isEmpty()) {
				// Aakhiri drum me bhi limit pass karni hogi.
				// Chuki list me sabhi cable same size ki hain, to pehli cable se limit nikal
				// lenge
				double maxDrumCapacity = (currentDrumCuts.get(0).diameterSize <= 70.0) ? limitSmall : limitLarge;
				schedule.add(createDrumRecord(combination, drumCounter, currentDrumCuts, maxDrumCapacity));
			}
		}
		return schedule;
	}

	// 🛠️ HELPER METHOD UPDATE: Ab ye maxLimit bhi accept karega
	private DrumRecord createDrumRecord(String combo, int drumNo, List<CutLengthRecord> cuts, double maxLimit) {
		DrumRecord drum = new DrumRecord();
		drum.cableCombination = combo;
		drum.drumNo = "Drum - " + drumNo;
		drum.maxDrumLimit = maxLimit; // 🔥 NEW FIELD SET KIYI
		drum.totalPieces = cuts.size();

		double totalLen = 0;
		Map<String, Integer> cutCounts = new LinkedHashMap<>();
		Map<String, Double> cutLengths = new LinkedHashMap<>();

		for (CutLengthRecord c : cuts) {
			totalLen += c.cableLength;
			String tag = c.orgTagNumber;
			cutCounts.put(tag, cutCounts.getOrDefault(tag, 0) + 1);
			cutLengths.put(tag, c.cableLength);
		}

		drum.exactOrderedLength = Math.round(totalLen * 100.0) / 100.0;

		StringBuilder details = new StringBuilder();
		for (String tag : cutCounts.keySet()) {
			details.append(tag).append(": ").append(cutCounts.get(tag)).append(" cuts x ").append(cutLengths.get(tag))
					.append("m each\n");
		}
		drum.cutDetails = details.toString().trim();
		return drum;
	}
}