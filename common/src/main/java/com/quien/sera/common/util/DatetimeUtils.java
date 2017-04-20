/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.common.util;

import com.quien.sera.common.constant.Constants;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class DatetimeUtils {

    private static final Logger logger = LoggerFactory
            .getLogger(DatetimeUtils.class);
    
    private final static Pattern P_MILLISECONDS = Pattern.compile( "^\\d+$" );

    public static final long ONE_DAY_IN_MILISECONDS = 24 * 60 * 60 * 1000;

    private static final String[] WEEK_NAMES = { "", "周日", "周一", "周二", "周三",
            "周四", "周五", "周六" };
    
    private static final String[] WEEK_NAMESS = { "", "星期日", "星期一", "星期二", "星期三",
        "星期四", "星期五", "星期六" };

    public static final Timestamp TIMESTAMP_ZERO = parseTimestamp("1970-01-01 10:00:00");

    public static final Timestamp TIMESTAMP_MAX = parseTimestamp("2038-01-19 03:14:07");

    public static final Date DATETIME_ZERO = parseDatetime("1970-01-01 10:00:00");


    
    public static boolean isTimestampZero(Timestamp timestamp) {
        return TIMESTAMP_ZERO.equals(timestamp);
    }

    public static boolean isNotTimestampZero(Timestamp timestamp) {
        return !isTimestampZero(timestamp);
    }

    public static boolean isDatetimeZero(Date datetime) {
        if (datetime != null && datetime instanceof Timestamp) {
            return isTimestampZero((Timestamp) datetime);
        }
        return DATETIME_ZERO.equals(datetime);
    }

    public static Timestamp afterOneWeek(Timestamp datetime) {

        if (datetime == null) {
            throw new IllegalArgumentException("Argument datetime is null!!");
        }

        Calendar c = Calendar.getInstance();
        c.setTime(datetime);
        c.add(Calendar.WEEK_OF_YEAR, 1);
        return new Timestamp(c.getTime().getTime());
    }

    public static long daysBetween(Timestamp datetime1, Timestamp datetime2) {

        Timestamp currentTimestamp = currentTimestamp();

        datetime1 = (datetime1 == null) ? currentTimestamp : datetime1;
        datetime2 = (datetime2 == null) ? currentTimestamp : datetime2;

        long delta = Math.abs(datetime2.getTime() - datetime1.getTime());
        return (long) Math.ceil(delta * 1.0 / ONE_DAY_IN_MILISECONDS);
    }
    
    public static long between(Timestamp datetime1, Timestamp datetime2) {

        Timestamp currentTimestamp = currentTimestamp();

        datetime1 = (datetime1 == null) ? currentTimestamp : datetime1;
        datetime2 = (datetime2 == null) ? currentTimestamp : datetime2;

        long delta = Math.abs(datetime2.getTime() - datetime1.getTime());
        return delta;
    }

    public static Timestamp currentTimestamp() {
        return toTimestamp(System.currentTimeMillis());
    }

    public static Timestamp toTimestamp( Long milliseconds ) {
        return new Timestamp( milliseconds );
    }
    
    public static Timestamp parseTimestamp(String timestamp) {

        if( P_MILLISECONDS.matcher( timestamp ).matches() ) {
            return new Timestamp( Long.parseLong( timestamp ) );
        }

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATETIME_FORMAT);
        try {
            return new Timestamp(sdf.parse(timestamp).getTime());
        } catch (Exception ex) {
            logger.error("\"" + timestamp + "\" is invalid,"
                    + " it should be in pattern " + " \""
                    + Constants.DATETIME_FORMAT + "\"", ex);
        }
        return null;
    }

    public static Date parseDatetime(String datetime) {

        if( P_MILLISECONDS.matcher( datetime ).matches() ) {
            return new Date( Long.parseLong( datetime ) );
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATETIME_FORMAT);
        
        try {
            return sdf.parse(datetime);
        } catch (Exception ex) {
            logger.error("\"" + datetime + "\" is invalid,"
                    + " it should be in pattern " + " \""
                    + Constants.DATETIME_FORMAT + "\"", ex);
        }
        return null;
    }

    public static Date parseDatetimeFormPattern(String datetime ,String parttern) {

        if( StringUtils.isEmpty(parttern)){
            parttern = Constants.DATETIME_FORMAT;
        }
        SimpleDateFormat sdf = new SimpleDateFormat( parttern );
        
        try {
            return sdf.parse(datetime);
        } catch (Exception ex) {
            logger.error("\"" + datetime + "\" is invalid,"
                    + " it should be in pattern " + " \""
                    + Constants.DATETIME_FORMAT + "\"", ex);
        }
        return null;
    }
    
    public static Timestamp parseTimestampFormPattern(String timestamp ,String parttern) {

        if( StringUtils.isEmpty(parttern)){
            parttern = Constants.DATETIME_FORMAT;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(parttern);
        
        try {
            return new Timestamp(sdf.parse(timestamp).getTime());
        } catch (Exception ex) {
            logger.error("\"" + timestamp + "\" is invalid,"
                    + " it should be in pattern " + " \""
                    + Constants.DATETIME_FORMAT + "\"", ex);
        }
        return null;
    }
    
    public static Timestamp parseGMTTimestamp(String timestamp ) {

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATETIME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        try {
            return new Timestamp(sdf.parse(timestamp).getTime());
        } catch (Exception ex) {
            logger.error("\"" + timestamp + "\" is invalid,"
                    + " it should be in pattern " + " \""
                    + Constants.DATETIME_FORMAT + "\"", ex);
        }
        return null;
    }
    
    public static Timestamp parseGMTTimestampFormPattern(String timestamp ,String parttern) {

        if( StringUtils.isEmpty(parttern)){
            parttern = Constants.DATETIME_FORMAT;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(parttern);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        try {
            return new Timestamp(sdf.parse(timestamp).getTime());
        } catch (Exception ex) {
            logger.error("\"" + timestamp + "\" is invalid,"
                    + " it should be in pattern " + " \""
                    + Constants.DATETIME_FORMAT + "\"", ex);
        }
        return null;
    }
    
    
    public static String formatDate(Date timestamp) {
        return timestamp == null ? "" : new SimpleDateFormat(
                Constants.DATETIME_FORMAT).format(timestamp);
    }

    public static String formatTimestamp(Timestamp timestamp) {
        return formatDate(timestamp);
    }

    public static String formatTimestamp(Timestamp timestamp, String parttern) {
        parttern = (StringUtils.isEmpty(parttern)) ? Constants.DATETIME_FORMAT
                : parttern;
        SimpleDateFormat sdf = new SimpleDateFormat(parttern);
        try {
            return sdf.format(timestamp);
        } catch (Exception ex) {
            logger.error("\"" + timestamp + "\" is invalid,"
                    + " it should be in pattern " + " \"" + parttern + "\"", ex);
        }
        return null;
    }

    public static String formatDate(Date date, String parttern) {
        parttern = (StringUtils.isEmpty(parttern)) ? Constants.DATETIME_FORMAT
                : parttern;
        SimpleDateFormat sdf = new SimpleDateFormat(parttern);
        try {
            return sdf.format(date);
        } catch (Exception ex) {
            logger.error("\"" + date + "\" is invalid,"
                    + " it should be in pattern " + " \"" + parttern + "\"", ex);
        }
        return null;
    }

    public static Timestamp dayStartDatetime() {
        return dayStartDatetime( currentTimestamp() );
    }

    public static Timestamp dayStartDatetime(Timestamp timestamp) {
        String date = formatDate(timestamp, Constants.DATE_FORMAT);
        return parseTimestamp(date + " 00:00:00");
    }

    public static Timestamp dayEndDatetime() {
        return dayEndDatetime( currentTimestamp() );
    }
    
    public static Timestamp dayEndDatetime(Timestamp timestamp) {
        String date = formatDate(timestamp, Constants.DATE_FORMAT);
        return parseTimestamp(date + " 23:59:59");
    }

    /**
     * 中午
     * @param timestamp
     * @return
     */
    public static Timestamp dayMiddleDatetime(Timestamp timestamp) {
        String date = formatDate(timestamp, Constants.DATE_FORMAT);
        return parseTimestamp(date + " 12:00:00");
    }

    public static Date monthStartDatetime( Date t ) {
        Calendar c = Calendar.getInstance();
        c.setTime( t );
        c.set( Calendar.DAY_OF_MONTH, 1 );
        c.set( Calendar.HOUR_OF_DAY, 0 );
        c.set( Calendar.MINUTE, 0 );
        c.set( Calendar.SECOND, 0 );
        return c.getTime();
    }

    public static Date monthEndDatetime( Date t ) {
        Calendar c = Calendar.getInstance();
        c.setTime( t );
        c.set( Calendar.DAY_OF_MONTH, c.getActualMaximum( Calendar.DAY_OF_MONTH ) );
        c.set( Calendar.HOUR_OF_DAY, 23 );
        c.set( Calendar.MINUTE, 59 );
        c.set( Calendar.SECOND, 59 );
        return c.getTime();
    }
    
    public static Timestamp nextDay() {
        return dayPlus( currentTimestamp(), 1);
    }

    public static Timestamp nextDay(Timestamp timestamp) {
        return dayPlus(timestamp, 1);
    }

    public static Timestamp yearPlus(Timestamp timestamp, int years) {
        Calendar c = Calendar.getInstance();
        c.setTime(timestamp);
        c.add(Calendar.YEAR, years);
        return new Timestamp(c.getTimeInMillis());
    }

    public static Timestamp dayPlus(Timestamp timestamp, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(timestamp);
        c.add(Calendar.DAY_OF_MONTH, days);
        return new Timestamp(c.getTimeInMillis());
    }
    
    public static Timestamp hourPlus(Timestamp timestamp, int hours) {
        Calendar c = Calendar.getInstance();
        c.setTime(timestamp);
        c.add(Calendar.HOUR_OF_DAY, hours);
        return new Timestamp(c.getTimeInMillis());
    }
    
     public static Timestamp minutePlus(Timestamp timestamp, int minutes) {
        Calendar c = Calendar.getInstance();
        c.setTime(timestamp);
        c.add(Calendar.MINUTE, minutes);
        return new Timestamp(c.getTimeInMillis());
    }
    
    public static Timestamp secondPlus(Timestamp timestamp, int seconds) {
        Calendar c = Calendar.getInstance();
        c.setTime(timestamp);
        c.add(Calendar.SECOND, seconds);
        return new Timestamp(c.getTimeInMillis());
    }
      
    public static Timestamp hourPlusSimple(Timestamp timestamp, int hours) {
        Calendar c = Calendar.getInstance();
        c.setTime(timestamp);
        c.add(Calendar.HOUR_OF_DAY, hours);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        return new Timestamp(c.getTimeInMillis());
    }

    /**
     * 
     * @param timestamp 传入的时间点
     * @param days 要增加的天数
     * @return 增加的天数后的凌晨00:00:00
     */
    public static Timestamp getEarlyMorningAfterDays(Timestamp timestamp,
            int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(timestamp);
        c.add(Calendar.DAY_OF_MONTH, days);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        return new Timestamp(c.getTimeInMillis());
    }
    
    public static String getWeekInString(Timestamp timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTime(timestamp);
        return WEEK_NAMES[c.get(Calendar.DAY_OF_WEEK)];
    }
    
    
    public static String getDaysOfWeekInString(Timestamp timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTime(timestamp);
        return WEEK_NAMESS[c.get(Calendar.DAY_OF_WEEK)];
    }

    public static String getNextWeekFirstDayStr(Timestamp nextDay,
            String parttern) {
        if(StringUtils.isEmpty(parttern)){
            parttern = Constants.DATETIME_FORMAT;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nextDay);
        int day_of_week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (day_of_week == 0){
         day_of_week = 7;
        }
        calendar.add(Calendar.DATE, -day_of_week +8);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return DatetimeUtils.formatDate(calendar.getTime());
    }

    public static Timestamp getNextWeekFirstDay(Timestamp nextDay,
            String parttern) {
        if(StringUtils.isEmpty(parttern)){
            parttern = Constants.DATETIME_FORMAT;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nextDay);
        int day_of_week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (day_of_week == 0){
         day_of_week = 7;
        }
        calendar.add(Calendar.DATE, -day_of_week +8);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return Timestamp.valueOf(formatDate(calendar.getTime()));
    }
    
    public static Timestamp getCurrentWeekFirstDay(Timestamp nextDay,
            String parttern) {
        if(StringUtils.isEmpty(parttern)){
            parttern = Constants.DATETIME_FORMAT;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nextDay);
        int day_of_week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (day_of_week == 0){
         day_of_week = 7;
        }
        calendar.add(Calendar.DATE, -day_of_week + 1);
       
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return Timestamp.valueOf(formatDate(calendar.getTime()));
    }
    
    public static String getCurrentWeekFirstDayStr(Timestamp nextDay,
            String parttern) {
        if(StringUtils.isEmpty(parttern)){
            parttern = Constants.DATETIME_FORMAT;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nextDay);
        int day_of_week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (day_of_week == 0){
         day_of_week = 7;
        }
        calendar.add(Calendar.DATE, -day_of_week + 1);
       
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return DatetimeUtils.formatDate(calendar.getTime());
    }
    
    
    public static String getUTCDatetimeStr(){
        Calendar cal = Calendar.getInstance();
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        String str=DatetimeUtils.formatDate(cal.getTime(),"YYYY-MM-dd'T'HH:mm:ss'Z'");
        return str;
    }

    public static String getUTCDatetimeStr(Calendar cal ){
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        String str=DatetimeUtils.formatDate(cal.getTime(),"YYYY-MM-dd'T'HH:mm:ss'Z'");   
        return str;
   }

    public static String getDisplayTime(long milliseconds) {

        String returnDate = "";
        Calendar curCalendar = Calendar.getInstance(Locale.CHINA);
        Calendar cal = Calendar.getInstance();

        TimeZone tz = TimeZone.getTimeZone("GMT+08:00");
        cal.setTimeZone(tz);
        cal.setTime(new Date(milliseconds));
        if (curCalendar.get(Calendar.DAY_OF_YEAR) == cal
                .get(Calendar.DAY_OF_YEAR)) {
            long time = Calendar.getInstance().getTimeInMillis() - milliseconds;
            if (time < 60000) {
                returnDate = "刚刚";
            } else if (time < 3600000) {
                returnDate = time / 60000 + "分钟前";
            } else if (time >= 3600000 && time < 86400000) {
                returnDate = time / 3600000 + "小时前";
            }
        } else if ((curCalendar.get(Calendar.DAY_OF_YEAR) - 1) == cal
                .get(Calendar.DAY_OF_YEAR)) {
            returnDate = "昨天 "
                    + new SimpleDateFormat("HH:mm").format(cal.getTime());
        } else {
            if (curCalendar.get(Calendar.YEAR) == cal.get(Calendar.YEAR)) {
                returnDate = new SimpleDateFormat("MM/dd HH:mm").format(cal
                        .getTime());
            } else {
                returnDate = new SimpleDateFormat("yyyy/MM/dd HH:mm")
                        .format(cal.getTime());
            }
        }
        return " " + returnDate;
    }
    
    public static boolean isBeforeOneMonth( Timestamp time ) {
        Calendar c1 = Calendar.getInstance();
        c1.add( Calendar.MONTH , -1 );
        
        Calendar c2 = Calendar.getInstance();
        c2.setTime( time );
        
        return c1.after( c2 );
    }
    
    public static boolean isBeforeDays( Timestamp time, int days ) {
        Calendar c1 = Calendar.getInstance();
        c1.add( Calendar.DAY_OF_YEAR , -days );
        
        Calendar c2 = Calendar.getInstance();
        c2.setTime( time );
        
        return c1.after( c2 );
    }

    public static boolean isBeforeHours( Timestamp time, int hours ) {
        Calendar c1 = Calendar.getInstance();
        c1.add( Calendar.HOUR_OF_DAY , -hours );

        Calendar c2 = Calendar.getInstance();
        c2.setTime( time );

        return c1.after( c2 );
    }

    public static boolean isYesterday( Timestamp time ) {
        Timestamp next = nextDay( dayEndDatetime( time ) );
        
        Timestamp today = dayEndDatetime( currentTimestamp() );
        
        return next.equals( today );
    }
    
    public static boolean isToday( Timestamp time ) {
        Timestamp current = dayEndDatetime( time );
        
        Timestamp today = dayEndDatetime( currentTimestamp() );
        
        return current.equals( today );
    }
    
    public static int secondsLeftInToday() {
        Timestamp current = currentTimestamp();
        return (int) between( current, dayEndDatetime( current ) ) / 1000;
    }

    private static String getThree(){
        Random rad=new Random();
        return rad.nextInt(1000)+"";
    }

    public static String getItemNo(){
        return formatTimestamp(currentTimestamp(),"yyyyMMddHHmmss")+getThree();
    }
    
    public static String daysDisNow(Timestamp time){
        Long now = System.currentTimeMillis()/1000;
        Long date = time.getTime()/1000;
       int dis = (int)(now-date)/3600/24;
        return String.valueOf( Math.abs(dis) );
    }
    
    
}