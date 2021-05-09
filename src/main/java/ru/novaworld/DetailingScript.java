package ru.novaworld;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.novaworld.models.YotaDetail;
import ru.novaworld.services.DetailingService;
import ru.novaworld.services.FileService;

import java.util.*;

public class DetailingScript {
    private final DetailingService detailingService = new DetailingService();
    private final FileService fileService = new FileService();

    private final Map<Date, List<YotaDetail>> monthlyDetails = new HashMap<>();

    public void execute(String path) {
        List<XSSFWorkbook> workbooks = fileService.getWorkbooksFrom(path);
        if (!workbooks.isEmpty()) {
            workbooks.forEach((workbook) -> {
                List <YotaDetail> detailsForMonth = new ArrayList<>();
                XSSFSheet sheet = workbook.getSheetAt(0);

                for (Row row : sheet) {
                    YotaDetail yotaDetail = detailingService.createYotaDetail(row);
                    if (yotaDetail != null) {
                        if (!detailsForMonth.isEmpty()) {
                            boolean areDatesEqual = detailingService.areDetailDatesEqual(
                                    detailsForMonth.get(0).getDate(),
                                    yotaDetail.getDate()
                            );
                            if (!areDatesEqual) {
                                monthlyDetails.put(
                                        detailsForMonth.get(0).getDate(),
                                        detailingService.getSummarizedDetailsListForMonth(detailsForMonth)
                                );
                                detailsForMonth = new ArrayList<>();
                            }
                        }
                        detailsForMonth.add(yotaDetail);
                    }
                }

                if (!detailsForMonth.isEmpty()) {
                    monthlyDetails.put(
                            detailsForMonth.get(0).getDate(),
                            detailingService.getSummarizedDetailsListForMonth(detailsForMonth)
                    );
                }

                System.out.println("-----------------------------------------------------------------------------");
                detailingService.printDetailsFrom(monthlyDetails);
            });
        }
    }
}
