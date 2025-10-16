package cs4105p2.util;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Timestamp {
    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");

    public static Date parse(String timestamp) {
        Date date = null;
        try {
            date = sdf.parse(timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String now() {
        return sdf.format(new Date());
    }

    public static String toTimestamp(Date date) {
        return sdf.format(date);
    }

    public static boolean isValid(String timestamp) {
        boolean isValid = false;
        try{
            sdf.parse(timestamp);
            isValid = true;
        } catch (ParseException e) {
        }
        return isValid;
    }
    
}
