package com.cable.se;

public class CalculationEngine {

	private ExcelReaderService excelReader = new ExcelReaderService();
	private static final double ROOT_3 = 1.732;
	public ExcelDataCache cache = new ExcelDataCache();

	public CalculationEngine(ExcelDataCache cache) {
        this.cache = cache;
    }
	public CalculationEngine() {
		// TODO Auto-generated constructor stub
	}
	public double calculateTotalActivePowerKw(double singleUnitRatingKw, int feederQty) {
		return singleUnitRatingKw * feederQty;
	}

	public double calculateTotalApparentPowerKva(double totalActivePowerKw, double fullLoadPf, double loadFactor) {
		if (fullLoadPf <= 0)
			return 0.0;
		return (totalActivePowerKw / fullLoadPf) * loadFactor;
	}

// Column T: Full Load Power Factor Calculation
	public double calculateFullLoadPf(String loadType, double ratingKw) {
		if ("MOTOR".equalsIgnoreCase(loadType.trim())) {
			java.util.Map.Entry<Double, ExcelDataCache.MotorDetails> entry = cache.motorDataCache.floorEntry(ratingKw);
			if (entry != null)
				return entry.getValue().flPf;
		}
		return 0.80; // Default for PANEL/HEATER
	}

	// =========================================================
	// 📌 Column U: Starting Power Factor
	// =========================================================
//	public Double calculateStartingPowerFactor(String loadType, double ratingKw) {
//		if ("MOTOR".equalsIgnoreCase(loadType.trim())) {
//			java.util.Map.Entry<Double, ExcelDataCache.MotorDetails> entry = cache.motorDataCache.floorEntry(ratingKw);
//			if (entry != null)
//				return entry.getValue().stPf;
//		}
//		return null; // in PANEL/HEATER starting PA -NA
//	}
	public Double calculateStartingPowerFactor(String loadType, double ratingKw) {
		if ("MOTOR".equalsIgnoreCase(loadType.trim())) {

			// 🔥 Precision Fix: ratingKw me 0.001 add karein taaki exact row match ho
			double searchKey = ratingKw + 0.001;

			java.util.Map.Entry<Double, ExcelDataCache.MotorDetails> entry = cache.motorDataCache.floorEntry(searchKey);
			if (entry != null)
				return entry.getValue().stPf;
		}
		return null; // in PANEL/HEATER starting PA -NA
	}

	// Column W & V: Efficiency Calculations
	public double calculateEfficiencyPercent(String loadType, double ratingKw, String effClass) {

		if ("MOTOR".equalsIgnoreCase(loadType.trim())) {

			java.util.Map.Entry<Double, ExcelDataCache.MotorDetails> entry = cache.motorDataCache.floorEntry(ratingKw);

			if (entry != null) {
				if ("IE2".equalsIgnoreCase(effClass.trim())) {
					return entry.getValue().ie2Eff; // VLOOKUP Column 6
				} else {
					return entry.getValue().ie3Eff; // VLOOKUP Column 7 (Default for everything else)
				}
			}
		}
		return 90.0;
	}

	public double convertEfficiencyToDecimal(double efficiencyPercent) {
		return efficiencyPercent / 100.0;
	}

	// Column R FORMULA FOR CURRENT (A)
	public double calculateFullLoadCurrent(double totalKw, double ratingKva, double voltage, double loadFactor,
			double effDecimal) {
		double current = 0.0;
		double root3 = 1.732;

		if (voltage <= 0)
			return 0.0;
		// voltage = 415.0; // Safety check

		// RULE 1: if Load Factor < 1.0 (PANEL or HEATER) - Using kW & Load Factor
		if (loadFactor < 1.0) {
			if (voltage > 240) {
				// 3-Phase: (kW * 1000 / (1.732 * V)) * LF
				current = (totalKw * 1000 / (root3 * voltage)) * loadFactor;
			} else {
				// 1-Phase
				current = (totalKw * 1000 / voltage) * loadFactor;
			}
		}
		// RULE 2: if Load Factor = 1.0 (MOTOR) -
		// Using kVA & Efficiency
		else {
			// if effciency decimal then use it otherwise default to 1.0
			double eff = (effDecimal > 0.0) ? effDecimal : 1.0;

			if (voltage > 240) {
				// 3-Phase: (kVA * 1000) / (1.732 * VOLTAGE * Efficiency)
				current = (ratingKva * 1000) / (root3 * voltage * eff);
			} else {
				// 1-Phase
				current = (ratingKva * 1000) / voltage;
			}
		}
		return current;
	}

	// =========================================================
	// Column S: Starting Current (A)
	// =========================================================

//	public Double calculateStartingCurrent(String loadType, String starterType, String effClass, double ratingKw,
//			double fullLoadCurrent) {
//		if (!"MOTOR".equalsIgnoreCase(loadType.trim())) {
//			return null;
//		}
//
//		String stType = starterType != null ? starterType.trim().toUpperCase() : "";
//
//		if (stType.equals("DOL") || stType.equals("HDOL") || stType.equals("BDOL") || stType.equals("RDOL")) {
//
//			double multiplier = 6.0; // Safe default
//
//			// Excel VLOOKUP floorEntry data
//			java.util.Map.Entry<Double, ExcelDataCache.MotorDetails> entry = cache.motorDataCache.floorEntry(ratingKw);
//			if (entry != null) {
//				// Check IE2 or IE3 (General Notes)
//				if ("IE3".equalsIgnoreCase(effClass.trim())) {
//					multiplier = entry.getValue().ie3StCurrentMulti; // Column F (Index 5)
//				} else {
//					multiplier = entry.getValue().ie2StCurrentMulti; // Column E (Index 4)
//				}
//			}
//			return multiplier * fullLoadCurrent;
//		}
//
//		// SOFT Starter (Direct 3x)
//		if (stType.equals("SOFT")) {
//			return 3.0 * fullLoadCurrent;
//		}
//
//		// VFD Starter (Direct 2x)
//		if (stType.equals("VFD")) {
//			return 2.0 * fullLoadCurrent;
//		}
//
//		// STAR-DELTA Starter (Direct 3.5x)
//		if (stType.equals("STAR-DELTA") || stType.equals("STAR DELTA")) {
//			return 3.5 * fullLoadCurrent;
//		}
//
//		return 0.0;
//	}
	public Double calculateStartingCurrent(String loadType, String starterType, String effClass, double ratingKw,
			double fullLoadCurrent) {
		System.out.println("Calculating Starting Current for LoadType: " + loadType + ", StarterType: " + starterType
				+ ", EffClass: " + effClass + ", RatingKw: " + ratingKw + ", FullLoadCurrent: " + fullLoadCurrent);
		if (!"MOTOR".equalsIgnoreCase(loadType.trim())) {
			return null;
		}

		String stType = starterType != null ? starterType.trim().toUpperCase() : "";

		if (stType.equals("DOL") || stType.equals("HDOL") || stType.equals("BDOL") || stType.equals("RDOL")) {

			double multiplier = 6.0; // Safe default (if cache is empty)

			
			double searchKey = ratingKw + 0.001; // because of Precision issue
			System.out.println("Searching for Motor Data with key: " + searchKey);
			// Excel VLOOKUP floorEntry data
			java.util.Map.Entry<Double, ExcelDataCache.MotorDetails> entry = cache.motorDataCache.floorEntry(searchKey);

			System.out.println("Motor Data Entry found: " + (entry != null));
			if (entry != null) {
				// Check IE2 or IE3 (General Notes)
				if ("IE3".equalsIgnoreCase(effClass.trim())) {
					multiplier = entry.getValue().ie3StCurrentMulti; // Column F (Index 5)
					System.out.println("IE3 Multiplier found: " + multiplier + " for ratingKw: " + ratingKw);
				} else {
					multiplier = entry.getValue().ie2StCurrentMulti; // Column E (Index 4)
					System.out.println("IE2 Multiplier found: " + multiplier + " for ratingKw: " + ratingKw);
				}
			}
			return multiplier * fullLoadCurrent;
		}

		// SOFT Starter (Direct 3x)
		if (stType.equals("SOFT")) {
			return 3.0 * fullLoadCurrent;
		}

		// VFD Starter (Direct 2x)
		if (stType.equals("VFD")) {
			return 2.0 * fullLoadCurrent;
		}

		// STAR-DELTA Starter (Direct 3.5x)
		if (stType.equals("STAR-DELTA") || stType.equals("STAR DELTA")) {
			return 3.5 * fullLoadCurrent;
		}

		return 0.0;
	}

	// Column Y: Number of Cores
	public double calculateNumberOfCores(String loadType, double cableSizeSqmm) {
		// Condition 1: MOTOR or HEATER
		if ("MOTOR".equalsIgnoreCase(loadType) || "HEATER".equalsIgnoreCase(loadType)) {
			return 3.0;
		}
		// Condition 2: NOT Motor/Heater AND Cable Size <= 16 sq.mm
		else if (cableSizeSqmm <= 16.0) {
			return 4.0;
		}
		// Condition 3: NOT Motor/Heater AND Cable Size > 16 sq.mm
		else {
			return 3.5;
		}
	}

	// Column AA: Derating Factor Calculation
	public Double getDeratingFactor(String excelFilePath) {
		return excelReader.readSpecificCell(excelFilePath, "GENERAL NOTES ", // Sheet name
				10, // Row index for 11
				4 // Column index for E
		);
	}

	// Cable Base Ampacity (From INPUT DATA Sheet)
	public Double calculateCurrentRatingOfCable(Double baseAmpacity, int noOfRuns) {
		if (baseAmpacity == null) {
			System.out.println(" Warning: Base Ampacity is null , .");
			return null;
		}
		return baseAmpacity * noOfRuns;
	}

	// Column AC: Derated Current of the Cable
	public Double calculateDeratedCurrentOfCable(Double deratingFactor, Double currentRatingOfCable) {
		if (deratingFactor == null || currentRatingOfCable == null) {
			System.out.println(": Derating Factor or Current Rating missing . Derated Current not calculate .");
			return null;
		}

		return deratingFactor * currentRatingOfCable;
	}

	// =========================================================
	// Column AF: Starting Voltage Drop (%) Calculation
	// =========================================================
	/**
	 * Calculates Starting Voltage Drop (%). Excel Formula:
	 * =IF(S13="-NA-","-NA-",(((1.732*S13*(AS13/1000)*((AD13*U13)+(AE13*(SIN(ACOS(U13)))))/X13)/M13)*100))
	 * * @param startingCurrent Column S (Can be null if "-NA-")
	 * 
	 * @param unitLength      Column AS (Length in meters)
	 * @param cableResistance Column AD (R)
	 * @param startingPf      Column U (Starting Power Factor)
	 * @param cableReactance  Column AE (X)
	 * @param noOfRuns        Column X (Number of Runs)
	 * @param voltage         Column M (Voltage in V)
	 * @return Voltage drop percentage, or null if "-NA-"
	 */
	public Double calculateStartingVoltageDrop(Double startingCurrent, double unitLength, Double cableResistance,
			Double startingPf, Double cableReactance, int noOfRuns, double voltage) {

		if (startingCurrent == null || startingPf == null) {
			return null;
		}

		if (noOfRuns == 0 || voltage == 0) {
			System.out.println("⚠️ Warning: Number of Runs ya Voltage 0 hai. Division by zero bacha liya gaya!");
			return null;
		}

		double lengthInKm = unitLength / 1000.0;

		// Trigonometry: sin(acos(PF))
		double angleRadians = Math.acos(startingPf);
		double sinPhi = Math.sin(angleRadians);

		// Voltage Drop (V) Calculation
		double voltageDropVolts = 1.732 * startingCurrent * lengthInKm
				* ((cableResistance * startingPf) + (cableReactance * sinPhi));

		// Percentage Calculation
		double voltageDropPercentage = (voltageDropVolts / (noOfRuns * voltage)) * 100.0;
		return voltageDropPercentage;
	}

	// =========================================================
	// 📌 Column AG: Total Starting Voltage Drop (%)
	// =========================================================
	/**
	 * Calculates Total Starting Voltage Drop (%). Excel Formula:
	 * =IF(AF13="-NA-","-NA-",(AF13)) * @param startingVoltageDrop Column AF (Can be
	 * null if "-NA-")
	 * 
	 * @return Total starting voltage drop (same as input)
	 */
	public Double calculateTotalStartingVoltageDrop(Double startingVoltageDrop) {
		// Since in Java we are handling "-NA-" as null,
		// we can directly return the old value itself.
		// If it is null, then AG will also automatically become null ("-NA-").
		return startingVoltageDrop;
	}

	// =========================================================
	// 📌 Column AH: Running Voltage Drop (%) Calculation
	// =========================================================
	/**
	 * Calculates Running Voltage Drop (%). Excel Formula:
	 * =(((1.732*R13*(AS13/1000)*((AD13*T13)+(AE13*(SIN(ACOS(T13)))))/X13)/M13)*100)
	 * * @param fullLoadCurrent Column R (Running Current)
	 * 
	 * @param unitLength      Column AS (Length in meters)
	 * @param cableResistance Column AD (R)
	 * @param fullLoadPf      Column T (Full Load Power Factor)
	 * @param cableReactance  Column AE (X)
	 * @param noOfRuns        Column X (Number of Runs)
	 * @param voltage         Column M (Voltage in V)
	 * @return Running voltage drop percentage
	 */
	public Double calculateRunningVoltageDrop(double fullLoadCurrent, double unitLength, Double cableResistance,
			double fullLoadPf, Double cableReactance, int noOfRuns, double voltage) {

		// Safety checks for null values from VLOOKUPs
		if (cableResistance == null || cableReactance == null) {
			System.out
					.println("⚠️ Warning: Resistance or Reactance could not be fetched. Running Voltage Drop failed!");
			return null;
		}

		if (noOfRuns == 0 || voltage == 0) {
			System.out.println(
					"⚠️ Warning: Number of Runs or Voltage is 0. Division by zero, Running Voltage Drop failed!");
			return null;
		}

		double lengthInKm = unitLength / 1000.0;

		// Trigonometry: sin(acos(PF))
		double angleRadians = Math.acos(fullLoadPf);
		double sinPhi = Math.sin(angleRadians);

		// Voltage Drop (V) Calculation
		double voltageDropVolts = 1.732 * fullLoadCurrent * lengthInKm
				* ((cableResistance * fullLoadPf) + (cableReactance * sinPhi));

		// Percentage Calculation
		double voltageDropPercentage = (voltageDropVolts / (noOfRuns * voltage)) * 100.0;

		return voltageDropPercentage;
	}

	// =========================================================
	// ⚡ CALCULATE TOTAL RUNNING VOLTAGE DROP (Running + Upstream)
	// =========================================================
	public Double calculateTotalRunningVoltageDrop(Double runningVd, Double upstreamVd) {
		if (runningVd == null)
			return null;
		double upVd = (upstreamVd != null) ? upstreamVd : 0.0;
		return runningVd + upVd;
	}

	/*
	 * 📌 VALIDATION 1: Cable Derated Current > Motor FL Current
	 * =========================================================
	 */
	/**
	 * Validates if the selected cable can handle the motor's full load current
	 * safely. Excel Formula: =IF(AND(AC13>R13),"OK","not OK") * @param
	 * deratedCurrent Column AC (Derated Current of the Cable)
	 * 
	 * @param fullLoadCurrent Column R (Motor's Full Load Current)
	 * @return "OK" if safe, "not OK" if unsafe
	 */

	// Excel Formula: =IF(AND(AC>R), "OK", "not OK")
	public String validateCableCapacity(Double deratedCurrent, Double fullLoadCurrent) {
		if (deratedCurrent != null && fullLoadCurrent != null && deratedCurrent > fullLoadCurrent) {
			return "OK";
		}
		return "not OK"; // 
	}

	/*
	 * VOLTAGE DROP LIMITS FETCHING (From GENERAL NOTES)
	 * =========================================================
	 */
	/**
	 * Helper methods to fetch standard voltage drop limits. E12 -> Row 11, Col 4
	 * E13 -> Row 12, Col 4 E15 -> Row 14, Col 4
	 */
	public Double getVdLimitE12(String filePath) {
		return excelReader.readSpecificCell(filePath, "GENERAL NOTES ", 11, 4);
	}

	public Double getVdLimitE13(String filePath) {
		return excelReader.readSpecificCell(filePath, "GENERAL NOTES ", 12, 4);
	}

	public Double getVdLimitE15(String filePath) {
		return excelReader.readSpecificCell(filePath, "GENERAL NOTES ", 14, 4);
	}

	public Double getVdLimitE14(String filePath) {
		return excelReader.readSpecificCell(filePath, "GENERAL NOTES ", 13, 4);
	}

	/*
	 * ========================================================= VALIDATION
	 * 2:Voltage Drop Validation
	 * =========================================================
	 */
	/**
	 * Validates if the calculated voltage drops are within the permissible limits.
	 * Excel Formula: =IF(AG13="-NA-",IF(AH13>='GENERAL NOTES '!$E$12,"Not
	 * OK","OK"),IF(AG13>='GENERAL NOTES '!$E$15,"Not OK",(IF(AH13>='GENERAL NOTES
	 * '!$E$13,"Not OK","OK")))) * @param totalStartingVd Column AG (Can be null if
	 * "-NA-")
	 * 
	 * @param runningVd Column AH
	 * @param limitE12  Limit from GENERAL NOTES E12 (e.g., 1%)
	 * @param limitE13  Limit from GENERAL NOTES E13 (e.g., 4.5%)
	 * @param limitE15  Limit from GENERAL NOTES E15 (e.g., 15%)
	 * @return "OK" if safe, "Not OK" if limits exceeded
	 */

	// Excel Formula: =IF(AG="-NA-", IF(AJ>=E14,"Not OK","OK"), IF(AG>=E15,"Not OK",
	// IF(AJ>=E13,"Not OK","OK")))

	public String validateVoltageDrop(Double totalStartingVd, Double runningVd, Double totalRunningVd, Double limitE12,
			Double limitE13, Double limitE14, Double limitE15, String panelName) {

		if (runningVd == null || totalRunningVd == null)
			return "Not OK";

	
		// (totalRunningVd)
		boolean isPCC = (panelName != null && panelName.trim().toUpperCase().contains("PCC"));
		Double vdToCheck = isPCC ? runningVd : totalRunningVd;

	
		if (totalStartingVd == null) {
			Double limitToUse = isPCC ? limitE12 : limitE14;
			if (limitToUse != null && vdToCheck >= limitToUse) {
				return "Not OK";
			}
			return "OK";
		}
		// 
		else {
			if (limitE15 != null && totalStartingVd >= limitE15)
				return "Not OK";
			if (limitE13 != null && vdToCheck >= limitE13)
				return "Not OK";
			return "OK";
		}
	}

	/*
	 * Column AM: Cable Size Validation (Final Check)
	 * =========================================================
	 */
	// Excel Formula: =IF(AND(AK="OK", AL="OK"), "OK", "Not OK")
	public String validateFinalCableSize(String capacityStatus, String vdStatus) {
		if ("OK".equalsIgnoreCase(capacityStatus) && "OK".equalsIgnoreCase(vdStatus)) {
			return "OK";
		}
		return "Not OK";
	}

	// Column AN: Final Number of Runs
	public int calculateFinalRuns(String starterType, int existingRuns) {
		// In a Star-Delta starter, 6 cables go to the motor, so the runs are doubled.
		if ("STAR-DELTA".equalsIgnoreCase(starterType)) {
			return existingRuns * 2;
		}
		return existingRuns;
	}

	// Column AO: Cable Size Formatting
	public String formatCableSizeString(double noOfCores, double cableSizeSqmm) {
		// If cores value is 3.0, convert it to "3" so that "3.0C" is not displayed.
		String coreString = (noOfCores == Math.floor(noOfCores)) ? String.valueOf((int) noOfCores)
				: String.valueOf(noOfCores);
		String sizeString = (cableSizeSqmm == Math.floor(cableSizeSqmm)) ? String.valueOf((int) cableSizeSqmm)
				: String.valueOf(cableSizeSqmm);
		return coreString + "C x " + sizeString;
	}


	public String getCableType(double cableSizeSqmm) {
		// No need to open the Excel file, fetch it directly from RAM (0.0001
		// milliseconds!)
		return cache.cableTypeCache.getOrDefault(cableSizeSqmm, "XLPE,Al"); // Default XLPE,Al
	}

	// Column AQ: Cable Code Calculation
	// =========================================================
	public String getCableCode(double cableSizeSqmm) {
		if (cableSizeSqmm <= 4.0) {
			return "2XWY";
		} else if (cableSizeSqmm == 10.0) {
			return "A2XWY";
		} else {
			return "A2XFY";
		}
	}

	// Column AT: Total Length (m)
	public double calculateTotalLength(double noOfCores, double unitLength, int finalNoOfRuns) {
		if (noOfCores == 1.0) {
			return 3.0 * unitLength * finalNoOfRuns; // 3 phase Single Core = 3 cables
		} else {
			return unitLength * finalNoOfRuns;
		}
	}

	// Column AU: Cable Diameter (Bounded Lookup)
	public Double getCableDiameter(double cableSizeSqmm, double noOfCores, String excelFilePath) {
		int startRow = 0, endRow = 0;

		// Ranges based on Excel formula (0-indexed for Java)
		if (noOfCores == 3.0) {
			startRow = 8;
			endRow = 25; // Rows 9 to 26
		} else if (noOfCores == 3.5) {
			startRow = 26;
			endRow = 37; // Rows 27 to 38
		} else {
			startRow = 38;
			endRow = 55; // Rows 39 to 56 (For 4 Core / Others)
		}

//		System.out.println(" Fetching Diameter for " + cableSizeSqmm + " sqmm in range Row " + (startRow + 1) + " to "
//				+ (endRow + 1));
		// Search Col AF (Index 31), Return Col AG (Index 32)
		return excelReader.performBoundedVlookup(excelFilePath, "INPUT DATA ", cableSizeSqmm, 31, 32, startRow, endRow);
	}

	// Column AV: Total Cable Diameter
	public Double calculateTotalCableDiameter(Double cableDiameter, int finalNoOfRuns) {
		if (cableDiameter == null)
			return null;
		return cableDiameter * finalNoOfRuns;
	}

	// Column AW & AX: Lugs & Glands Quantity
	public int calculateLugsQty(double noOfCores, int finalNoOfRuns) {
		if (noOfCores == 3.0) {
			return finalNoOfRuns * 3 * 2;
		} else {
//			return finalNoOfRuns * 4 * 2 * 3;
			return finalNoOfRuns * 4 * 2 ;
		}
	}

	public int calculateGlandsQty(int finalNoOfRuns) {
		return finalNoOfRuns * 2;
	}

	// Column AY: Final Cable Selected Formatting
	public String formatCableSelected(double noOfCores, double cableSizeSqmm, String cableType) {
		// Cleaning up decimals for display (e.g., 400.0 -> "400", but 3.5 -> "3.5")
		String coreStr = (noOfCores == Math.floor(noOfCores)) ? String.valueOf((int) noOfCores)
				: String.valueOf(noOfCores);
		String sizeStr = (cableSizeSqmm == Math.floor(cableSizeSqmm)) ? String.valueOf((int) cableSizeSqmm)
				: String.valueOf(cableSizeSqmm);

		// Exact Match with Excel: 1.1kV, 3.5C X 400 XLPE,Al
		return "1.1kV, " + coreStr + "C X " + sizeStr + " " + (cableType != null ? cableType : "");
	}

	public Double getBaseCableAmpacity(String layingMode, double cableSizeSqmm, double noOfCores,
			String excelFilePath) {
		// Key format matches our cache: "AIR_400.0_3.5"
		String key = layingMode + "_" + cableSizeSqmm + "_" + noOfCores;
		return cache.ampacityCache.get(key);
	}

	public Double getCableResistance(double cableSizeSqmm, String excelFilePath) {
		// Resistance is cached just by size
		return cache.resistanceCache.get(cableSizeSqmm);
	}

	public Double getCableReactance(double cableSizeSqmm, double noOfCores, String excelFilePath) {
		// Key format: "400.0_3.5"
		String key = cableSizeSqmm + "_" + noOfCores;
		return cache.reactanceCache.get(key);
	}

	public Double fetchCablePrice(double core, double size, String conductor, String excelFilePath) {
		// Key format: "3.5_400.0_XLPE,Al"
		String key = core + "_" + size + "_" + conductor.trim();
		return cache.priceCache.getOrDefault(key, 0.0);
	}
}