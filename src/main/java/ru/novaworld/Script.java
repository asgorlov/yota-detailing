package ru.novaworld;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.novaworld.models.YotaDetail;
import ru.novaworld.services.DetailingService;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static ru.novaworld.constants.Constants.*;

public class Script {
    private String path;
    private final DetailingService detailingService = new DetailingService();
    private final Map<Date, List<YotaDetail>> monthlyDetails = new HashMap<>();

    public Script() {}

    public Script(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void execute() {
        File resources = new File(path);
        try {
            List<File> files = Arrays.asList(Objects.requireNonNull(resources.listFiles()));
            files.forEach((file) -> {
                String typeOfFile = file.getName().substring(file.getName().lastIndexOf('.'));
                if (XLSX_TYPE.equals(typeOfFile) || XLS_TYPE.equals(typeOfFile)) {
                    try {
                        OPCPackage opcPackage = OPCPackage.open(file);
                        XSSFWorkbook workbook = new XSSFWorkbook(opcPackage);
                        XSSFSheet sheet = workbook.getSheetAt(0);
                        List <YotaDetail> detailsForMonth = new ArrayList<>();

                        for (Row row : sheet) {
                            YotaDetail yotaDetail = detailingService.createYotaDetail(row);
                            if (yotaDetail != null) {
                                if (!detailsForMonth.isEmpty() && !detailsForMonth.get(0).getDate().equals(yotaDetail.getDate())) {
                                    monthlyDetails.put(detailsForMonth.get(0).getDate(), detailingService.getSummarizedDetailsListForMonth(detailsForMonth));
                                    detailsForMonth = new ArrayList<>();
                                }
                                detailsForMonth.add(yotaDetail);
                            }
                        }
                        if (!detailsForMonth.isEmpty()) {
                            monthlyDetails.put(detailsForMonth.get(0).getDate(), detailingService.getSummarizedDetailsListForMonth(detailsForMonth));
                        }

                        detailingService.printDetailsFrom(monthlyDetails);
                    } catch (InvalidFormatException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("There are no exel files.");
                }
            });
        } catch (NullPointerException e) {
            System.out.println("Folder \"resources\" is empty.");
            e.printStackTrace();
        }
    }
}
