package com.cable.se;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DrumScheduleGenerator {

    // Helper class to track active/open drums during processing
    private static class ActiveDrum {
        int drumNumber;
        double currentLength;
        double maxCapacity;
        List<CutLengthRecord> cuts;

        public ActiveDrum(int drumNumber, double maxCapacity) {
            this.drumNumber = drumNumber;
            this.maxCapacity = maxCapacity;
            this.currentLength = 0.0;
            this.cuts = new ArrayList<>();
        }
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

            List<ActiveDrum> activeDrums = new ArrayList<>();
            int drumCounter = 1;

            for (CutLengthRecord cut : cuts) {
                double maxDrumCapacity = (cut.diameterSize <= 70.0) ? limitSmall : limitLarge;
                boolean placedInExistingDrum = false;

                // 1. First-Fit Algorithm: Try to fit the cut in any existing open drum
                for (ActiveDrum drum : activeDrums) {
                    // Safe floating point addition rounding to avoid precision issues
                    double projectedLength = Math.round((drum.currentLength + cut.cableLength) * 100.0) / 100.0;
                    
                    if (projectedLength <= drum.maxCapacity) {
                        drum.cuts.add(cut);
                        drum.currentLength = projectedLength;
                        cut.orderingDrumNumber = "Drum - " + drum.drumNumber;
                        placedInExistingDrum = true;
                        break; // Stop searching once we find a suitable drum
                    }
                }

                // 2. If it couldn't fit anywhere, create a brand new drum
                if (!placedInExistingDrum) {
                    ActiveDrum newDrum = new ActiveDrum(drumCounter, maxDrumCapacity);
                    newDrum.cuts.add(cut);
                    
                    // Safe rounding for initial length
                    newDrum.currentLength = Math.round(cut.cableLength * 100.0) / 100.0; 
                    
                    cut.orderingDrumNumber = "Drum - " + newDrum.drumNumber;
                    activeDrums.add(newDrum);
                    
                    drumCounter++; // Increment counter for the next possible new drum
                }
            }

            // 3. Finalize all drums for this specific cable combination
            for (ActiveDrum drum : activeDrums) {
                schedule.add(createDrumRecord(combination, drum.drumNumber, drum.cuts, drum.maxCapacity));
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

        double totalLen = 0.0;
        Map<String, Integer> cutCounts = new LinkedHashMap<>();
        Map<String, Double> cutLengths = new LinkedHashMap<>();

        for (CutLengthRecord c : cuts) {
            totalLen += c.cableLength;
            // Use orderingTagNumber to differentiate split cut pieces in details
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
//public class DrumScheduleGenerator {
//	
//	public List<DrumRecord> generateLinkedDrumSchedule(List<CutLengthRecord> masterCutList, double limitSmall,
//			double limitLarge) {
//		List<DrumRecord> schedule = new ArrayList<>();
//
//		// Grouping cuts by combination
//		Map<String, List<CutLengthRecord>> groupedCuts = new LinkedHashMap<>();
//		for (CutLengthRecord cut : masterCutList) {
//			String combo = cut.core + "C x " + cut.diameterSize + " Sqmm " + cut.cableType;
//			groupedCuts.computeIfAbsent(combo, k -> new ArrayList<>()).add(cut);
//		}
//
//		for (Map.Entry<String, List<CutLengthRecord>> entry : groupedCuts.entrySet()) {
//			String combination = entry.getKey();
//			List<CutLengthRecord> cuts = entry.getValue();
//
//			int drumCounter = 1;
//			double currentDrumLength = 0.0;
//			List<CutLengthRecord> currentDrumCuts = new ArrayList<>();
//
//			for (CutLengthRecord cut : cuts) {
//				
//				double maxDrumCapacity = (cut.diameterSize <= 70.0) ? limitSmall : limitLarge;
//
//				if (currentDrumLength + cut.cableLength > maxDrumCapacity && !currentDrumCuts.isEmpty()) {
//					
//					schedule.add(createDrumRecord(combination, drumCounter, currentDrumCuts, maxDrumCapacity));
//					drumCounter++;
//					currentDrumLength = 0.0;
//					currentDrumCuts.clear();
//				}
//
//				cut.orderingDrumNumber = "Drum - " + drumCounter;
//				currentDrumCuts.add(cut);
//				currentDrumLength += cut.cableLength;
//			}
//
//			if (!currentDrumCuts.isEmpty()) {
//				double maxDrumCapacity = (currentDrumCuts.get(0).diameterSize <= 70.0) ? limitSmall : limitLarge;
//				schedule.add(createDrumRecord(combination, drumCounter, currentDrumCuts, maxDrumCapacity));
//			}
//		}
//		return schedule;
//	}
//
//
//	private DrumRecord createDrumRecord(String combo, int drumNo, List<CutLengthRecord> cuts, double maxLimit) {
//		DrumRecord drum = new DrumRecord();
//		drum.cableCombination = combo;
//		drum.drumNo = "Drum - " + drumNo;
//		drum.maxDrumLimit = maxLimit; 
//		drum.totalPieces = cuts.size();
//
//		double totalLen = 0;
//		Map<String, Integer> cutCounts = new LinkedHashMap<>();
//		Map<String, Double> cutLengths = new LinkedHashMap<>();
//
//		for (CutLengthRecord c : cuts) {
//			totalLen += c.cableLength;
//			String tag = c.orgTagNumber;
//			cutCounts.put(tag, cutCounts.getOrDefault(tag, 0) + 1);
//			cutLengths.put(tag, c.cableLength);
//			
//			
//	               
//		}
//
//		drum.exactOrderedLength = Math.round(totalLen * 100.0) / 100.0;
//		StringBuilder details = new StringBuilder();
//		for (String tag : cutCounts.keySet()) {
//			details.append(tag).append(": ").append(cutCounts.get(tag)).append(" cuts x ").append(cutLengths.get(tag))
//					.append("m each\n");
//		}
//		drum.cutDetails = details.toString().trim();
//		return drum;
//	}
//}