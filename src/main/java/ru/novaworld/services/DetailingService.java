package ru.novaworld.services;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import ru.novaworld.models.YotaCall;
import ru.novaworld.models.YotaDetail;
import ru.novaworld.models.YotaInternet;
import ru.novaworld.models.YotaSms;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.novaworld.constants.Constants.*;

public class DetailingService {
    private final DateFormat dateFormat;

    public DetailingService() {
        dateFormat = new SimpleDateFormat(DATE_FORMAT, LocaleUtil.getUserLocale());
        dateFormat.setTimeZone(LocaleUtil.getUserTimeZone());
    }

    public YotaDetail createYotaDetail(Row row) {
        List<String> cells = getCellsFrom(row);

        try {
            cells.set(3, cells.get(3).replace("  ", " ").replace("\n", " "));
            String[] detailType = cells.get(3).split(" ");
            if (detailType.length > 1) {
                switch (detailType[1].replace(":", "")) {
                    case INTERNET:
                        return new YotaInternet(
                                dateFormat.parse(cells.get(0)),
                                cells.get(3),
                                getInternetTrafficInMb(cells));
                    case CALLS:
                        return new YotaCall(
                                dateFormat.parse(cells.get(0)),
                                cells.get(3),
                                getCallTimeInSeconds(cells));
                    case SMS:
                        return new YotaSms(
                                dateFormat.parse(cells.get(0)),
                                cells.get(3),
                                getSmsQuantityInPieces(cells));
                    default:
                        dateFormat.parse(cells.get(0));
                        System.out.println("Unknown type of yota detail from exel row");
                }
            }
        } catch (ParseException ignored) {} //Check to skip empty cells and with incorrect date

        return null;
    }

    public List<YotaDetail> getSummarizedDetailsListForMonth(List <YotaDetail> detailsForMonth) {
        Map<String, YotaDetail> summarizedDetailsForMonth = new HashMap<>();

        detailsForMonth.forEach((detail) -> {
            YotaDetail summarizedDetail = summarizedDetailsForMonth.get(detail.getType());
            if (summarizedDetail == null) {
                summarizedDetail = detail;
            } else if (summarizedDetail instanceof YotaInternet){
                double result = ((YotaInternet) summarizedDetail).getQuantity() + ((YotaInternet) detail).getQuantity();
                ((YotaInternet) summarizedDetail).setQuantity(result);
            } else if (summarizedDetail instanceof YotaCall) {
                int result = ((YotaCall) summarizedDetail).getQuantity() + ((YotaCall) detail).getQuantity();
                ((YotaCall) summarizedDetail).setQuantity(result);
            } else {
                int result = ((YotaSms) summarizedDetail).getQuantity() + ((YotaSms) detail).getQuantity();
                ((YotaSms) summarizedDetail).setQuantity(result);
            }
            summarizedDetailsForMonth.put(detail.getType(), summarizedDetail);
        });

        return new ArrayList<>(summarizedDetailsForMonth.values());
    }

    public void printDetailsFrom(Map<Date, List<YotaDetail>> details) {
        details.forEach((key, detailList) -> detailList.forEach((detail) -> {
            String quantity;
            if (detail instanceof YotaInternet) {
                quantity = getFormattedInternetTraffic(((YotaInternet) detail).getQuantity());
            } else if (detail instanceof YotaCall) {
                quantity = getFormattedCallTime(((YotaCall) detail).getQuantity());
            } else {
                quantity = getFormattedSms(((YotaSms) detail).getQuantity());
            }

            System.out.println(
                    "Дата=|" + dateFormat.format(detail.getDate()) + "| " +
                    "Услуга=|" + detail.getType() + "| " +
                    "Объем=|" + quantity + "|"
            );
        }));
    }

    public double getInternetTrafficInMb(List<String> cells) {
        String internetTraffic = findTrafficQuantityIn(cells);
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

    public String getFormattedInternetTraffic(double megabytes) {
        double gigabytes = megabytes / 1024;
        if (gigabytes >= 1) {
            return String.format("%.2f",gigabytes) + " Gb";
        }

        return String.format("%.2f",megabytes) + " Mb";
    }

    public int getCallTimeInSeconds(List<String> cells) {
        String callTime = findCallTimeIn(cells);
        String[] timeArray = callTime.split(" ")[0].split(":");
        if (timeArray.length < 3) {
            return convertCallTime("00", timeArray[0], timeArray[1]);
        }

        return convertCallTime(timeArray[0], timeArray[1], timeArray[2]);
    }

    public String getFormattedCallTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = seconds / 60 % 60;
        seconds = seconds % 60;

        return addZeroTo(hours) + ':' + addZeroTo(minutes) + ':' + addZeroTo(seconds);
    }

    public int getSmsQuantityInPieces(List<String> cells) {
        String smsQuantity = findSmsQuantity(cells);
        try {
            return Integer.parseInt(smsQuantity.split(" ")[0]);
        } catch (NumberFormatException e) {
            System.out.println("Can't parse sms quantity.");
            e.printStackTrace();

            return 0;
        }
    }

    public String getFormattedSms(int quantity) {
        return quantity + " шт";
    }

    public boolean areDetailDatesEqual(Date date1, Date date2) {
        return dateFormat.format(date1).equals(dateFormat.format(date2));
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

    private List<String> getCellsFrom(Row row) {
        List<String> cells = new ArrayList<>();

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

        return cells;
    }

    private String addZeroTo(int time) {
        String strTime = Integer.toString(time);
        if (strTime.length() == 1) {
            return  '0' + strTime;
        }
        return strTime;
    }

    private String findTrafficQuantityIn(List<String> cells) {
        if (isTraffic(cells.get(7))) return cells.get(7);

        for (String cell : cells) {
            if (isTraffic(cell)) return cell;
        }

        System.out.println("Can't find traffic quantity in cell array.");
        return TRAFFIC_DEFAULT_VALUE;
    }

    private boolean isTraffic(String value) {
        if (value.isEmpty()) {
            return false;
        } else {
            return value.contains("Mb") || value.contains("Gb");
        }
    }

    private String findCallTimeIn(List<String> cells) {
        if (isCallTime(cells.get(7))) return cells.get(7);

        for (String cell : cells) {
            if (isCallTime(cell)) return cell;
        }

        System.out.println("Can't find call time quantity in cell array.");
        return CALL_TIME_DEFAULT_VALUE;
    }

    private boolean isCallTime(String value) {
        if (value.isEmpty()) {
            return false;
        } else {
            Pattern regexp = Pattern.compile("\\b\\d{2}:\\d{2}\\b");
            Matcher match = regexp.matcher(value);
            return match.find();
        }
    }

    private String findSmsQuantity(List<String> cells) {
        if (isSmsQuantity(cells.get(7))) return cells.get(7);

        for (String cell : cells) {
            if (isSmsQuantity(cell)) return cell;
        }

        System.out.println("Can't find sms quantity in cell array.");
        return SMS_DEFAULT_VALUE;
    }

    private boolean isSmsQuantity(String value) {
        if (value.isEmpty()) {
            return false;
        } else {
            return value.contains("шт");
        }
    }
}