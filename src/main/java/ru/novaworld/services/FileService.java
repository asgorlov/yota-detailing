package ru.novaworld.services;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static ru.novaworld.constants.Constants.XLSX_TYPE;
import static ru.novaworld.constants.Constants.XLS_TYPE;

public class FileService {

    public List<XSSFWorkbook> getWorkbooksFrom(String path) {
        List<XSSFWorkbook> workbooks = new ArrayList<>();

        File resources = new File(path);
        try {
            List<File> files = Arrays.asList(Objects.requireNonNull(resources.listFiles()));
            files.forEach((file) -> {
                String typeOfFile = file.getName().substring(file.getName().lastIndexOf('.'));
                if (XLSX_TYPE.equals(typeOfFile) || XLS_TYPE.equals(typeOfFile)) {
                    try {
                        OPCPackage opcPackage = OPCPackage.open(file);
                        XSSFWorkbook workbook = new XSSFWorkbook(opcPackage);
                        workbooks.add(workbook);
                    } catch (InvalidFormatException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (NullPointerException e) {
            System.out.println("There are no exel files.");
            e.printStackTrace();
        }

        return workbooks;
    }

    public void createDetailingExelFile(List<String> detailList) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Detailing");

        int rowCounter = 0;
        Row rowHeader = sheet.createRow(rowCounter);
        rowHeader.createCell(0).setCellValue("Дата");
        rowHeader.createCell(1).setCellValue("Услуга");
        rowHeader.createCell(2).setCellValue("Объем");

        for(String detail : detailList) {
            String[] yotaObjectFields = detail.split("\\|");
            Row row = sheet.createRow(++rowCounter);
            row.createCell(0).setCellValue(yotaObjectFields[0]);
            row.createCell(1).setCellValue(yotaObjectFields[1]);
            row.createCell(2).setCellValue(yotaObjectFields[2]);
        }

        try (FileOutputStream out = new FileOutputStream("yota detailing.xlsx")) {
            workbook.write(out);
            System.out.println("Excel file has been successfully created!");
        } catch (IOException e) {
            System.out.println("Can't create exel file");
            e.printStackTrace();
        }
    }
}
