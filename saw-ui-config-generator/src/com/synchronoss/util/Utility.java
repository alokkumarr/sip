package com.synchronoss.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Utility {
	
	public static HashMap<Integer, HashMap<Integer, String>> readExcelFile(String filePath) throws IOException{
		HashMap<Integer,HashMap<Integer, String>> rowMap = new HashMap<Integer,HashMap<Integer,String>>();
		InputStream inputStream = new FileInputStream(filePath);
		XSSFWorkbook  workBook = new XSSFWorkbook(inputStream);
		XSSFSheet sheet = workBook.getSheetAt(0);
		XSSFRow row; 
		XSSFCell cell;
		Iterator<?> rows = sheet.rowIterator();
		while (rows.hasNext()){
			row=(XSSFRow) rows.next();
			if(!(row.getCell(0).getCellType() == Cell.CELL_TYPE_BLANK)){
				int index = row.getRowNum();
				Iterator<?> cells = row.cellIterator();
				HashMap<Integer, String> cellMap = new HashMap<Integer, String>();
				while (cells.hasNext())
				{
					cell=(XSSFCell) cells.next();
					if (cell.getCellType() == XSSFCell.CELL_TYPE_STRING)
					{
						cellMap.put(cell.getColumnIndex(), cell.getStringCellValue());
					}else if(cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC){
						cellMap.put(cell.getColumnIndex(), Integer.toString(new Double(cell.getNumericCellValue()).intValue()));
					}
				}
				rowMap.put(index, cellMap);
			}
		}
		return rowMap;
	}
	
	public static HashMap<String, String> readProperties(String filePath) throws IOException{
		Properties prop = new Properties();
		InputStream input = null;
		HashMap<String, String> configMap = new HashMap<String, String>();
		input = new FileInputStream(filePath);
		prop.load(input);
		for (String key : prop.stringPropertyNames()) {
		    String value = prop.getProperty(key);
		    configMap.put(key, value);
		}
		return configMap;
	}
	
	public static String getPropertyValue(String key,String filePath ){
		HashMap<String, String> configMap;
		String configFilePath = filePath!=null?filePath:"config/config.properties";
		try {
			configMap = readProperties(configFilePath);
			if(configMap!=null){
				return configMap.get(key);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void writeJsonFile(String filePath,String fileName,String jsonString) throws IOException{
		File directory = new File(filePath);
		if(!directory.exists()){
			directory.mkdir();
		}
		File file = new File(directory,fileName+".json");
		FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
		fileWriter.write(jsonString);
		fileWriter.flush();
		fileWriter.close();
	}
	public static void convertConsoleToFile(String dirPath){
		try {
			File directory = new File(dirPath);
			if (!directory.exists()) {
				directory.mkdir();
			}
			File file = new File(directory,"ExcelToJsonConvertor.log");
			FileOutputStream fos = new FileOutputStream(file);
			PrintStream fileout = new PrintStream(fos);
			System.setOut(fileout);
			System.out.println(new Date());
		} catch (FileNotFoundException e) {
			System.out.println(e);
		}
	}
}
