package com.cable.se;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Reads unit prices from the "price" sheet and caches them in RAM for blazing
 * fast lookups. This prevents the 30-minute execution delay by only reading the
 * Excel file ONCE.
 */
public class ExcelPriceService implements PriceService {

	// Cache to store prices in RAM: Key = "Core_Size_Conductor", Value = Price
	private final Map<String, Double> priceCache = new HashMap<>();

	private static final int COL_CORE = 2;
	private static final int COL_SIZE = 3;
	private static final int COL_COND = 4;
	private static final int COL_RATE = 5;

	public ExcelPriceService(String filePath, String sheetName) {
		loadPricesIntoMemory(filePath, sheetName);
	}

	private void loadPricesIntoMemory(String filePath, String sheetName) {
		System.out.println(" Loading Price Sheet into Memory...");
		try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {

			Sheet sheet = workbook.getSheet(sheetName);
			if (sheet == null) {
				System.out.println("❌ Error: Price sheet '" + sheetName + "' not found!");
				return;
			}

			// Skip header row 0, read from row 1
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null)
					continue;

				double rowCore = readNumeric(row.getCell(COL_CORE));
				double rowSize = readNumeric(row.getCell(COL_SIZE));
				String rowCond = readString(row.getCell(COL_COND));
				double rowRate = readNumeric(row.getCell(COL_RATE));

				if (rowRate > 0) {
					String key = generateKey(rowCore, rowSize, rowCond);
					priceCache.put(key, rowRate);
				}
			}
			System.out.println("✅ Loaded " + priceCache.size() + " price entries into RAM.");

		} catch (IOException e) {
			System.out.println("❌ Error reading Price sheet: " + e.getMessage());
		}
	}

	@Override
	public double getUnitPrice(double noOfCores, double cableSizeSqmm, String condType) {
		String key = generateKey(noOfCores, cableSizeSqmm, condType);
		return priceCache.getOrDefault(key, 0.0); // Returns 0.0 instantly if not found
	}

	private String generateKey(double core, double size, String cond) {
		// e.g., "3.5_400.0_XLPE,Al"
		return core + "_" + size + "_" + (cond != null ? cond.trim().toUpperCase() : "");
	}

	// --- Data Extraction Helpers ---
	private double readNumeric(Cell cell) {
		if (cell == null)
			return -1.0;
		if (cell.getCellType() == CellType.NUMERIC)
			return cell.getNumericCellValue();
		if (cell.getCellType() == CellType.STRING) {
			try {
				return Double.parseDouble(cell.getStringCellValue().trim());
			} catch (Exception e) {
				return -1.0;
			}
		}
		return -1.0;
	}

	private String readString(Cell cell) {
		if (cell == null)
			return "";
		if (cell.getCellType() == CellType.STRING)
			return cell.getStringCellValue().trim();
		return "";
	}
}