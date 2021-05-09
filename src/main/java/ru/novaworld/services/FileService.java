package ru.novaworld.services;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
}
