package com.cable.se;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class ExcelDataCache {

	// RAM CACHES (HashMaps)
	public final Map<String, Double> ampacityCache = new HashMap<>();
	public final Map<Double, Double> resistanceCache = new HashMap<>();
	public final Map<String, Double> reactanceCache = new HashMap<>();
	public final Map<String, Double> priceCache = new HashMap<>();
// Conductor Type ("XLPE,Al" ya "XLPE,Cu")
	public final Map<Double, String> cableTypeCache = new HashMap<>();

	public static class MotorDetails {
		public double flPf;
		public double stPf;
		public double ie2StCurrentMulti;
		public double ie3StCurrentMulti;
		public double ie2Eff;
		public double ie3Eff;
	}

	// 2. MASTER MOTOR CACHE
	public final NavigableMap<Double, MotorDetails> motorDataCache = new TreeMap<>();

	private void loadMotorData(Sheet sheet) {
		if (sheet == null) {
			System.out.println("❌ Error: 'MOTOR DATA' sheet not found.");
			return;
		}

		// (Excel Row 8 = Java Index 7)
		for (int i = 7; i <= sheet.getLastRowNum(); i++) {
			Row row = sheet.getRow(i);
			if (row == null)
				continue;

			double kw = getSafeNumeric(row.getCell(1)); // Col B
			if (kw <= 0)
				continue;

			MotorDetails details = new MotorDetails();
			details.flPf = getSafeNumeric(row.getCell(2)); // Col C
			details.stPf = getSafeNumeric(row.getCell(3)); // Col D
			details.ie2StCurrentMulti = getSafeNumeric(row.getCell(4)); // Col E
			details.ie3StCurrentMulti = getSafeNumeric(row.getCell(5)); // Col F
			details.ie2Eff = getSafeNumeric(row.getCell(6)); // Col G
			details.ie3Eff = getSafeNumeric(row.getCell(7)); // Col H

			motorDataCache.put(kw, details);
		}
	}

//	public void loadAllDataIntoRam(String filePath) {
//		System.out.println(" [STEP 1 & 2] Loading Excel Data into RAM Caches...");
//
//		try (FileInputStream fis = new FileInputStream(filePath); Workbook wb = new XSSFWorkbook(fis)) {
//
//			loadInputData(wb.getSheet("INPUT DATA "));
//			loadPriceData(wb.getSheet("price"));
//
//			System.out.println(
//					" All Data Cached! (Ampacity: " + ampacityCache.size() + ", Resistance: " + resistanceCache.size()
//							+ ", Reactance: " + reactanceCache.size() + ", Prices: " + priceCache.size() + ") ");
//		} catch (Exception e) {
//			System.out.println("❌ Error caching Excel data: " + e.getMessage());
//		}
//	}
	public void loadAllDataIntoRam(String filePath) {
        System.out.println(" [STEP 1 & 2] Loading Excel Data into RAM Caches...");

        try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath); 
             org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(fis)) {

            loadInputData(wb.getSheet("INPUT DATA "));
            loadPriceData(wb.getSheet("price"));

            // =========================================================
            // 🔥 THIS IS THE CRITICAL MISSING PART 🔥
            // We must call loadMotorData, otherwise the cache stays empty!
            // =========================================================
            org.apache.poi.ss.usermodel.Sheet motorSheet = wb.getSheet("MOTOR DATA ");
            if (motorSheet == null) {
                motorSheet = wb.getSheet("MOTOR DATA"); // Fallback in case there is no space
            }
            loadMotorData(motorSheet);

            // Print the updated cache sizes including Motors
            System.out.println(" All Data Cached! (Ampacity: " + ampacityCache.size() + 
                               ", Resistance: " + resistanceCache.size() + 
                               ", Reactance: " + reactanceCache.size() + 
                               ", Prices: " + priceCache.size() + 
                               ", Motors: " + motorDataCache.size() + ") "); 
                               
        } catch (Exception e) {
            System.out.println("❌ Error caching Excel data: " + e.getMessage());
        }
    }


	private void loadInputData(Sheet sheet) {
		if (sheet == null) {
			System.out.println("❌ Error: 'INPUT DATA' sheet not found.");
			return;
		}

		// RESISTANCE & REACTANCE CACHE (Rows 9 to 56)
		for (int i = 9; i <= 56; i++) {
			Row row = sheet.getRow(i);
			if (row == null)
				continue;

			double size = getSafeNumeric(row.getCell(23)); // Col X (Index 23)
			if (size <= 0)
				continue; // Skip invalid sizes

			Cell typeCell = row.getCell(28);
			if (typeCell != null && typeCell.getCellType() == CellType.STRING) {
				cableTypeCache.put(size, typeCell.getStringCellValue().trim());
			}
			double resVal = getSafeNumeric(row.getCell(24)); // Col Y
			if (resVal >= 0)
				resistanceCache.put(size, resVal);

			double reac1C = getSafeNumeric(row.getCell(25)); // Col Z
			double reacMulti = getSafeNumeric(row.getCell(26)); // Col AA

			if (reac1C >= 0)
				reactanceCache.put(size + "_1.0", reac1C);
			if (reacMulti >= 0) {
				reactanceCache.put(size + "_2.0", reacMulti);
				reactanceCache.put(size + "_3.0", reacMulti);
				reactanceCache.put(size + "_3.5", reacMulti);
				reactanceCache.put(size + "_4.0", reacMulti);
			}
		}

		// AMPACITY CACHE (AIR) (Rows 9 to 28)
		for (int i = 9; i <= 28; i++) {
			Row row = sheet.getRow(i);
			if (row == null)
				continue;

			double size = getSafeNumeric(row.getCell(1)); // Col B
			if (size <= 0)
				continue;

			double amp1C = getSafeNumeric(row.getCell(2)); // 1C
			double amp2C = getSafeNumeric(row.getCell(3)); // 2C
			double amp3C = getSafeNumeric(row.getCell(4)); // 3C
			double amp3_5C = getSafeNumeric(row.getCell(5)); // 3.5C
			double amp4C = getSafeNumeric(row.getCell(6)); // 4C

			if (amp1C >= 0)
				ampacityCache.put("AIR_" + size + "_1.0", amp1C);
			if (amp2C >= 0)
				ampacityCache.put("AIR_" + size + "_2.0", amp2C);
			if (amp3C >= 0)
				ampacityCache.put("AIR_" + size + "_3.0", amp3C);
			if (amp3_5C >= 0)
				ampacityCache.put("AIR_" + size + "_3.5", amp3_5C);
			if (amp4C >= 0)
				ampacityCache.put("AIR_" + size + "_4.0", amp4C);
		}
	}

	private void loadPriceData(Sheet sheet) {
		if (sheet == null) {
			System.out.println("❌ Error: 'price' sheet not found.");
			return;
		}
		for (int i = 1; i <= sheet.getLastRowNum(); i++) {
			Row row = sheet.getRow(i);
			if (row == null)
				continue;

			double core = getSafeNumeric(row.getCell(2));
			double size = getSafeNumeric(row.getCell(3));
			String cond = "";
			if (row.getCell(4) != null) {
				cond = row.getCell(4).getStringCellValue().trim();
			}
			double rate = getSafeNumeric(row.getCell(5));

			if (core > 0 && size > 0 && rate > 0 && !cond.isEmpty()) {
				String key = core + "_" + size + "_" + cond;
				priceCache.put(key, rate);
			}
		}
	}

	private double getSafeNumeric(Cell cell) {
		if (cell == null)
			return -1.0;
		try {
			switch (cell.getCellType()) {
			case NUMERIC:
				return cell.getNumericCellValue();
			case FORMULA:
				// Agar cell me formula hai, toh uska evaluated result nikalo
				if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
					return cell.getNumericCellValue();
				}
				break;
			case STRING:
				return Double.parseDouble(cell.getStringCellValue().trim());
			}
		} catch (Exception e) {

		}
		return -1.0;
	}

}