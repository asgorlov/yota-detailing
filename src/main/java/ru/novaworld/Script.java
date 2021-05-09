package ru.novaworld;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.novaworld.models.YotaDetailing;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Script {
    private static final String XLSX_TYPE= ".xlsx";
    private static final String XLS_TYPE= ".xls";
    private static final String NUMERIC= "NUMERIC";
    private static final String STRING= "STRING";
    private static final String DATE_FORMAT= "dd.MM.yy";
    private static final String CALLS = "связь:";
    private static final String INTERNET= "Yota";
    private static final String SMS= "SMS";

    private String path;
    private final Map<String, List<YotaDetailing>> monthlyDetails = new HashMap<>();

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
                        List <YotaDetailing> detailsForMonth = new ArrayList<>();

                        for (Row row : sheet) {
                            YotaDetailing yotaDetailing = getYotaDetailingFromRow(row);
                            if (yotaDetailing != null) {
                                if (!detailsForMonth.isEmpty() && !detailsForMonth.get(0).getDate().equals(yotaDetailing.getDate())) {
                                    monthlyDetails.put(detailsForMonth.get(0).getDate(), getSummarizedDetailsListForMonth(detailsForMonth));
                                    detailsForMonth = new ArrayList<>();
                                }
                                detailsForMonth.add(yotaDetailing);
                            }
                        }
                        if (!detailsForMonth.isEmpty()) {
                            monthlyDetails.put(detailsForMonth.get(0).getDate(), getSummarizedDetailsListForMonth(detailsForMonth));
                        }

                        printDetails(monthlyDetails);
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

    private YotaDetailing getYotaDetailingFromRow(Row row) {
        List<String> cells = new ArrayList<>();
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, LocaleUtil.getUserLocale());
        dateFormat.setTimeZone(LocaleUtil.getUserTimeZone());

        Iterator<Cell> cellIter = row.cellIterator();
        while (cellIter.hasNext()) {
            XSSFCell cell = (XSSFCell) cellIter.next();
            switch (cell.getCellType().toString()) {
                case NUMERIC:
                    String cellValue;
                    if (DateUtil.isCellDateFormatted(cell)) {
                        cellValue = dateFormat.format(cell.getDateCellValue());
                    } else {
                        double num = cell.getNumericCellValue();
                        cellValue = Double.toString(num);
                    }
                    cells.add(cellValue);
                    break;
                case STRING:
                    cells.add(cell.getStringCellValue());
                    break;
                default:
                    cells.add(cell.toString());
            }
        }

        try {
            dateFormat.parse(cells.get(0));
            return new YotaDetailing(cells.get(0), cells.get(3).replace("\n", " "), cells.get(7));
        } catch (ParseException e) {
            return null;
        }
    }

    private List<YotaDetailing> getSummarizedDetailsListForMonth(List <YotaDetailing> detailsForMonth) {
        Map<String, YotaDetailing> detailsForMonthByType = new HashMap<>();

        detailsForMonth.forEach((detail) -> {
            if (detailsForMonthByType.containsKey(detail.getType())) {
                String[] splitNameType = detail.getType().split(" ");
                if (splitNameType.length > 1) {
                    switch (splitNameType[1]) {
                        case INTERNET:
                            double trafficFromDetail = getInternetTrafficInMb(detail.getQuantity());
                            double trafficFromMap = getInternetTrafficInMb(detailsForMonthByType.get(detail.getType()).getQuantity());
                            double sumOfTraffic = trafficFromDetail + trafficFromMap;

                            detail.setQuantity(getFormattedInternetTraffic(sumOfTraffic));
                            break;
                        case CALLS:
                            int timeFromDetail = getCallTimeInSeconds(detail.getQuantity());
                            int timeFromMap = getCallTimeInSeconds(detailsForMonthByType.get(detail.getType()).getQuantity());
                            int sumOfTime = timeFromDetail + timeFromMap;

                            detail.setQuantity(getFormattedCallTime(sumOfTime));
                            break;
                        case SMS:
                            try {
                                int smsQuantityFromDetail = Integer.parseInt(detail.getQuantity().split(" ")[0]);
                                int smsQuantityFromMap = Integer.parseInt(detailsForMonthByType.get(detail.getType()).getQuantity().split(" ")[0]);
                                int sumOfSmsQuantity = smsQuantityFromDetail + smsQuantityFromMap;

                                detail.setQuantity(sumOfSmsQuantity + " шт.");
                            } catch (NumberFormatException e) {
                                System.out.println("Can't parse SMS quantity.");
                                e.printStackTrace();
                            }
                            break;
                        default:
                            System.out.println("Unknown type of yota service details: " + detail);
                    }
                } else {
                    System.out.println("Wrong type of yota service details: " + detail);
                }
            }
            detailsForMonthByType.put(detail.getType(),detail);
        });

        return new ArrayList<>(detailsForMonthByType.values());
    }

    private double getInternetTrafficInMb(String internetTraffic) {
        String[] trafficArray = internetTraffic.split(" ");
        try {
            String traffic = trafficArray[0].replace(',', '.');
            double trafficQuantity = Double.parseDouble(traffic);
            if ("Gb".equals(trafficArray[1])) {
                trafficQuantity *= 1024;
            }

            return trafficQuantity;
        } catch (NumberFormatException e) {
            System.out.println("Can't parse internet traffic quantity.");
            e.printStackTrace();

            return 0.0;
        }
    }

    private String getFormattedInternetTraffic(double megabytes) {
        double gigabytes = megabytes / 1024;
        if (gigabytes >= 1) {
            return String.format("%.2f",gigabytes) + " Gb";
        }

        return String.format("%.2f",megabytes) + " Mb";
    }

    private int getCallTimeInSeconds(String callTime) {
        String[] timeArray = callTime.split(" ")[0].split(":");
        if (timeArray.length < 3) {
            return convertCallTime("00", timeArray[0], timeArray[1]);
        }

        return convertCallTime(timeArray[0], timeArray[1], timeArray[2]);
    }

    private int convertCallTime(String strHours, String strMinutes, String strSeconds) {
        try {
            int hours = Integer.parseInt(strHours);
            int minutes = Integer.parseInt(strMinutes);
            int seconds = Integer.parseInt(strSeconds);

            return seconds + (minutes + (hours * 60)) * 60;
        } catch (NumberFormatException e) {
            System.out.println("Can't parse call time quantity.");
            e.printStackTrace();

            return 0;
        }
    }

    private String getFormattedCallTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = seconds / 60 % 60;
        seconds = seconds % 60;

        return addZeroTo(hours) + ':' + addZeroTo(minutes) + ':' + addZeroTo(seconds);
    }

    private String addZeroTo(int time) {
        String strTime = Integer.toString(time);
        if (strTime.length() == 1) {
            return  '0' + strTime;
        }
        return strTime;
    }

    private void printDetails(Map<String, List<YotaDetailing>> details) {
        details.forEach((key, detailList) -> detailList.forEach(System.out::println));
    }
}
