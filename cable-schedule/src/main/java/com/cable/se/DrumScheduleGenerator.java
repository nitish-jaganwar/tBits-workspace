package com.cable.se;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DrumScheduleGenerator {
	
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
				
				double maxDrumCapacity = (cut.diameterSize <= 70.0) ? limitSmall : limitLarge;

				if (currentDrumLength + cut.cableLength > maxDrumCapacity && !currentDrumCuts.isEmpty()) {
					
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
				double maxDrumCapacity = (currentDrumCuts.get(0).diameterSize <= 70.0) ? limitSmall : limitLarge;
				schedule.add(createDrumRecord(combination, drumCounter, currentDrumCuts, maxDrumCapacity));
			}
		}
		return schedule;
	}


	private DrumRecord createDrumRecord(String combo, int drumNo, List<CutLengthRecord> cuts, double maxLimit) {
		DrumRecord drum = new DrumRecord();
		drum.cableCombination = combo;
		drum.drumNo = "Drum - " + drumNo;
		drum.maxDrumLimit = maxLimit; 
		drum.totalPieces = cuts.size();

		double totalLen = 0;
		Map<String, Integer> cutCounts = new LinkedHashMap<>();
		Map<String, Double> cutLengths = new LinkedHashMap<>();

		for (CutLengthRecord c : cuts) {
			totalLen += c.cableLength;
			String tag = c.orgTagNumber;
			cutCounts.put(tag, cutCounts.getOrDefault(tag, 0) + 1);
			cutLengths.put(tag, c.cableLength);
			
			c.actualLength = c.cableLength; 
	        c.slitStatus = "Yes";             
		}

		drum.exactOrderedLength = Math.round(totalLen * 100.0) / 100.0;

		//Calculate remaining/balance cable left on drum
	    drum.drumBalancedLength = Math.round((drum.maxDrumLimit - drum.exactOrderedLength) * 100.0) / 100.0;
	    
		StringBuilder details = new StringBuilder();
		for (String tag : cutCounts.keySet()) {
			details.append(tag).append(": ").append(cutCounts.get(tag)).append(" cuts x ").append(cutLengths.get(tag))
					.append("m each\n");
		}
		drum.cutDetails = details.toString().trim();
		return drum;
	}
}