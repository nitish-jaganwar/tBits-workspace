package com.cable.se;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelReaderService {

//
//	public Double performVlookup(String filePath, String sheetName, double lookupVal, int searchCol, int returnCol) {
//
//		try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
//
//			Sheet sheet = workbook.getSheet(sheetName);
//			if (sheet == null) {
//				System.out.println(" Error: Sheet '" + sheetName + "' nahi mili!");
//				return null;
//			}
//
//			
//			for (Row row : sheet) {
//				Cell searchCell = row.getCell(searchCol);
//
//				
//				if (searchCell != null && searchCell.getCellType() == CellType.NUMERIC) {
//					double cellValue = searchCell.getNumericCellValue();
//
//					
//					if (Math.abs(cellValue - lookupVal) < 0.001) { // decimal match check
//						Cell returnCell = row.getCell(returnCol);
//						if (returnCell != null && returnCell.getCellType() == CellType.NUMERIC) {
//							return returnCell.getNumericCellValue();
//						}
//					}
//				}
//			}
//		} catch (IOException e) {
//			System.out.println(" Error reading Excel file: " + e.getMessage());
//		}
//
//		System.out.println(" Value " + lookupVal + " sheet " + sheetName + "not found.");
//		return null;
//	}
	public Double performVlookup(String filePath, String sheetName, double lookupVal, int searchCol, int returnCol) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                System.out.println("❌ Error: Sheet '" + sheetName + "' nahi mili!");
                return null;
            }

            for (Row row : sheet) {
                Cell searchCell = row.getCell(searchCol);
                
                if (searchCell != null) {
                    double cellValue = -1.0;
                    boolean isValueFound = false;

                    // 1. Agar cell sach me Numeric hai
                    if (searchCell.getCellType() == CellType.NUMERIC) {
                        cellValue = searchCell.getNumericCellValue();
                        isValueFound = true;
                    } 
                    // 2. Agar Excel me "Number Stored As Text" hai (Green Triangle)
                    else if (searchCell.getCellType() == CellType.STRING) {
                        try {
                            cellValue = Double.parseDouble(searchCell.getStringCellValue().trim());
                            isValueFound = true;
                        } catch (NumberFormatException e) {
                            // Agar purely text hai (jaise table header "SIZE"), toh ignore karo
                        }
                    } 
                    // 3. Agar value kisi formula se aayi hai
                    else if (searchCell.getCellType() == CellType.FORMULA) {
                        if (searchCell.getCachedFormulaResultType() == CellType.NUMERIC) {
                            cellValue = searchCell.getNumericCellValue();
                            isValueFound = true;
                        }
                    }

                    // Agar humari dhoondhi hui value match ho gayi!
                    if (isValueFound && Math.abs(cellValue - lookupVal) < 0.001) {
                        Cell returnCell = row.getCell(returnCol);
                        
                        if (returnCell != null) {
                            // Return cell ko bhi safely read karenge
                            if (returnCell.getCellType() == CellType.NUMERIC) {
                                return returnCell.getNumericCellValue();
                            } else if (returnCell.getCellType() == CellType.STRING) {
                                try {
                                    return Double.parseDouble(returnCell.getStringCellValue().trim());
                                } catch (NumberFormatException e) {
                                    return null;
                                }
                            } else if (returnCell.getCellType() == CellType.FORMULA) {
                                if (returnCell.getCachedFormulaResultType() == CellType.NUMERIC) {
                                    return returnCell.getNumericCellValue();
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading Excel file: " + e.getMessage());
        }

        System.out.println("⚠️ Value " + lookupVal + " sheet " + sheetName + " me nahi mili.");
        return null;
    }
	
	/**
     * Text (String) return karne wala VLOOKUP.
     */
    public String performVlookupString(String filePath, String sheetName, double lookupVal, int searchCol, int returnCol) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) return null;

            for (Row row : sheet) {
                Cell searchCell = row.getCell(searchCol);
                if (searchCell != null) {
                    double cellValue = -1.0;
                    boolean isValueFound = false;

                    // Search logic same as before
                    if (searchCell.getCellType() == CellType.NUMERIC) {
                        cellValue = searchCell.getNumericCellValue();
                        isValueFound = true;
                    } else if (searchCell.getCellType() == CellType.STRING) {
                        try {
                            cellValue = Double.parseDouble(searchCell.getStringCellValue().trim());
                            isValueFound = true;
                        } catch (NumberFormatException e) { }
                    }

                    // Agar match ho gaya, toh return cell se STRING read karenge
                    if (isValueFound && Math.abs(cellValue - lookupVal) < 0.001) {
                        Cell returnCell = row.getCell(returnCol);
                        if (returnCell != null) {
                            if (returnCell.getCellType() == CellType.STRING) {
                                return returnCell.getStringCellValue();
                            } else if (returnCell.getCellType() == CellType.NUMERIC) {
                                return String.valueOf(returnCell.getNumericCellValue());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading Excel file: " + e.getMessage());
        }
        return null;
    }
    /**
     * Bounded VLOOKUP: Sirf di gayi startRow aur endRow ke beech me search karega.
     * Ye alag-alag core tables (3C, 3.5C, 4C) me collision rokne ke liye hai.
     */
    public Double performBoundedVlookup(String filePath, String sheetName, double lookupVal, 
                                        int searchCol, int returnCol, int startRow, int endRow) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) return null;

            // Sirf startRow se endRow tak loop chalayenge
            for (int i = startRow; i <= endRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell searchCell = row.getCell(searchCol);
                if (searchCell != null) {
                    double cellValue = -1.0;
                    boolean isValueFound = false;

                    // Numeric or String conversion (Same as our bulletproof logic)
                    if (searchCell.getCellType() == CellType.NUMERIC) {
                        cellValue = searchCell.getNumericCellValue();
                        isValueFound = true;
                    } else if (searchCell.getCellType() == CellType.STRING) {
                        try {
                            cellValue = Double.parseDouble(searchCell.getStringCellValue().trim());
                            isValueFound = true;
                        } catch (NumberFormatException e) { }
                    }

                    if (isValueFound && Math.abs(cellValue - lookupVal) < 0.001) {
                        Cell returnCell = row.getCell(returnCol);
                        if (returnCell != null && returnCell.getCellType() == CellType.NUMERIC) {
                            return returnCell.getNumericCellValue();
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error in Bounded VLOOKUP: " + e.getMessage());
        }
        return null;
    }
	
	public Double readSpecificCell(String filePath, String sheetName, int rowIndex, int colIndex) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                System.out.println("❌ Error: Sheet '" + sheetName + "' nahi mili!");
                return null;
            }

            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                Cell cell = row.getCell(colIndex);
                
                if (cell != null) {
                    // Condition 1: direct number type (e.g., 0.73)
                    if (cell.getCellType() == CellType.NUMERIC) {
                        return cell.getNumericCellValue();
                    } 
                    // Condition 2: formula type  (e.g., =0.73 ya =A1+B1)
                    else if (cell.getCellType() == CellType.FORMULA) {
                     
                        if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                            return cell.getNumericCellValue();
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading Excel file: " + e.getMessage());
        }

        System.out.println(" Cell Row:" + rowIndex + ", Col:" + colIndex + " sheet " + sheetName + " not valid no.");
        return null;
    }
}