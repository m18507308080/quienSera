/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.quien.sera.common.constant.Constants;
import com.quien.sera.common.entity.BaseEntity;
import com.quien.sera.common.entity.IndexField;
import com.quien.sera.common.vo.BaseVO;
import org.apache.commons.beanutils.*;
import org.apache.commons.beanutils.converters.AbstractConverter;
import org.apache.commons.beanutils.converters.StringConverter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class BaseUtils {
    
    private static final Logger logger =
            LoggerFactory.getLogger(BaseUtils.class);
    
    private static final Map<Class<?>, Logger> loggersMap =
            new ConcurrentHashMap<Class<?>, Logger>();
    
    private static final Map<String, Logger> namedLoggersMap =
            new ConcurrentHashMap<String, Logger>();

    private static final Pattern VERSION_PATTERN = 
            Pattern.compile( "^(\\d+)(\\.(\\d+))\\s*(\\.(\\d+))+$" );
    
    private final static GsonBuilder gsonBuilder;
    
    private final static Object NON_EXIST = new Object();
    
    static {
        gsonBuilder = new GsonBuilder().setDateFormat( 
                  Constants.DATETIME_FORMAT );
        
        BeanUtilsBean.setInstance(
                new BeanUtilsBean(new CustomizedConvertUtilsBean()));
        
    }

    public static boolean isNON( Object obj ) {
        return NON_EXIST == obj;
    }
    
    public static boolean isValidSid( Long sid ) {
        return sid != null && sid > 0;
    }
    
    public static boolean isNotValidSid( Long sid ) {
        return ! isValidSid( sid );
    }

    public static List<String> mapToString( Collection<?> objects ) {
        List<String> strList = new ArrayList<String>();
        if( objects == null || objects.isEmpty() ) {
            return strList;
        }

        for( Object obj : objects ) {
            strList.add( String.valueOf( obj ) );
        }

        return strList;
    }

    public static List<Long> extractSidList( List<?> objs ) {
        
        if( objs == null || objs.isEmpty() ) {
            return new ArrayList<Long>(0);
        }
        
        List<Long> sids = new ArrayList<Long>( objs.size() );
        
        for( Object obj : objs ) {
            Object v = BaseUtils.getProperty( obj, "sid" );
            if( v != null ) {
                if( v instanceof String && StringUtils.isNotEmpty((String) v) ) {
                    v = Long.parseLong( (String)v );
                }
            }
            sids.add( v != null ? (Long)v : null );
        }
        
        return sids;
    }

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    public static String toJson( Object srcObj ) {
        if( srcObj == null ) {
            return "";
        }
        
        return getGson().toJson(srcObj);
    }

    public static <T> T fromJson( String json, Type clazz ) {

        if( StringUtils.isEmpty( json ) ) {
            return null;
        }

        if( clazz == null ) {
            throw new IllegalArgumentException( "class is null!!" );
        }

        return getGson().fromJson(json, clazz);
    }

    public static String firstLetterToUpper(String str){
        char[] array = str.toCharArray();
        array[0] -= 32;
        return String.valueOf(array);
    }
    
    public static <T> T fromJson( String json, Class<T> clazz ) {
        if( StringUtils.isEmpty( json ) ) {
            return null;
        }
        
        if( clazz == null ) {
            throw new IllegalArgumentException( "class is null!!" );
        }
        
        return getGson().fromJson( json, clazz );
    }
    
    private static Gson getGson() {
      return gsonBuilder.create();
    }
    
    public static Logger getLogger( Class<?> clazz ) {
        Logger log = loggersMap.get( clazz );
        if( log == null ) {
            log = LoggerFactory.getLogger( clazz );
            loggersMap.put( clazz, log );
        }
        return log;
    }
    
    public static Logger getLogger( String name ) {
        if( StringUtils.isEmpty( name ) ) {
            throw new IllegalArgumentException( "arg name is empty!!" );
        }
        
        Logger log = namedLoggersMap.get(name);
        
        if( log == null ) {
            log = LoggerFactory.getLogger( name );
            namedLoggersMap.put( name, log );
        }
        return log;
    }

    public static void copyProperties( Object dest, Object orig ) {
        
        try {
            BeanUtilsBean.getInstance().copyProperties( dest, orig );
        }catch( Exception ex ) {
            throw new IllegalArgumentException( ex );
        }
    }
    
    public static Object getProperty( Object obj, String fieldName ) {
        try {
            return BeanUtilsBean.getInstance().getPropertyUtils().getProperty( obj, fieldName );
        } catch( NoSuchMethodException ex ) {
            return NON_EXIST;
        } catch ( Exception ex ) {
            throw new IllegalArgumentException( ex );
        }
    }
    
    public static void setProperty( Object obj, String fieldName, Object value ) {
        try {
            PropertyUtilsBean utils = BeanUtilsBean.getInstance().getPropertyUtils();
            Class type = utils.getPropertyType(obj, fieldName);
            if(type.equals(boolean.class) && value != null && value.getClass().equals(String.class)){
                Boolean bValue = Constants.BOOLEAN_TRUE_IN_STR.equals(value) ? true : false;
                utils.setProperty(obj, fieldName, bValue);
            } else {
                utils.setProperty( obj, fieldName, value );
            }
        }catch( Exception ex ) {
            throw new IllegalArgumentException( ex );
        }
    }
    
    public static void populate( Object bean, Map<String, Object> properties ) 
        throws IllegalAccessException, InvocationTargetException {
        
        BeanUtilsBean.getInstance().populate( bean, properties );
        
    }
    
    public static Map<String, String> describeAsString( Object bean ) 
                            throws IllegalAccessException, 
                                   InvocationTargetException, 
                                   NoSuchMethodException {
                
        Map<String, String> resultMap = new HashMap<String, String>();
        
        if( bean == null ) {
            return resultMap;
        }
        
        // if bean is a map
        if( bean instanceof Map ) {
            Map temp = (Map)bean;
            for( Object key : temp.keySet() ) {
                Object v = temp.get( key );
                if( v != null ) {
                    resultMap.put( key.toString(), v.toString() );
                }
            }
            return resultMap;
        }

        
        PropertyUtilsBean propertyUtils = BeanUtilsBean.getInstance()
                .getPropertyUtils();
        
        Map<String, Object> map = propertyUtils.describe( bean );
        
        if( map.size() > 0 ) {
            for( String key : map.keySet() ) {
                
                if( "class".equals( key ) ) {
                    continue; // ignore the Class property
                }
                
                Class<?> pClazz = 
                        propertyUtils.getPropertyType( bean, key );
                
                if( ! isDescribeSupportedType( pClazz ) ) {
                    continue;
                }
                
                Object value = map.get( key );
                
                if( value != null ) {
                    if( Date.class.isAssignableFrom( pClazz ) ) {
                        resultMap.put( key, DatetimeUtils.formatDate( (Date)value ) );
                    }else {
                        resultMap.put( key, String.valueOf( value ) );
                    }
                }
                
            }
        }
        
        return resultMap;
    }
    
    public static Map<String, List<?>> describe( Object bean ) 
                            throws IllegalAccessException, 
                                   InvocationTargetException, 
                                   NoSuchMethodException {
        
        Map<String, List<?>> mapList = new HashMap<String, List<?>>();
        
        if( bean == null ) {
            return mapList;
        }
        
        List<String> fields = new ArrayList<String>();
        mapList.put( "fields", fields );
        
        List<Object> values = new ArrayList<Object>();
        mapList.put( "values", values );
        
        PropertyUtilsBean propertyUtils = BeanUtilsBean.getInstance()
                .getPropertyUtils();
        
        Map<String, Object> map = propertyUtils.describe( bean );
        
        if( map.size() > 0 ) {
            for( String key : map.keySet() ) {
                
                if( "class".equals( key ) ) {
                    continue; // ignore the Class property
                }
                
                Class<?> pClazz = 
                        propertyUtils.getPropertyType( bean, key );
                
                if( ! isDescribeSupportedType( pClazz ) ) {
                    continue;
                }
                
                Object value = map.get( key );
                
                if( ( pClazz.isEnum() 
                            || IndexField.class.isAssignableFrom( pClazz ) ) 
                      && ( value != null ) ) {
                    value = value.toString();
                }
                
                fields.add( key );
                values.add( value );
            }
        }
        
        return mapList;
    }
    
    public static String digest( Object... values ) {
        StringBuilder sbd = new StringBuilder();
        if( values != null ) {
            sbd.append( Constants.SIGNATURE_SEPARATOR );
            for( Object value : values ) {
                if( value != null ) {
                    sbd.append( value.toString() );
                }
                sbd.append( Constants.SIGNATURE_SEPARATOR );
            }
        }
        return DigestUtils.md5Hex(sbd.toString());
    }
    
    private static boolean isDescribeSupportedType( Class<?> clazz ) {
        return clazz.isPrimitive() 
                || Number.class.isAssignableFrom( clazz )
                || Boolean.class.isAssignableFrom( clazz )
                || String.class.isAssignableFrom( clazz )
                || Date.class.isAssignableFrom( clazz )
                || Enum.class.isAssignableFrom( clazz )
                || IndexField.class.isAssignableFrom( clazz );
    }
    
    // class CustomizedConvertUtilsBean
    private static class CustomizedConvertUtilsBean extends ConvertUtilsBean2 {

        private static final CustomizedStringConverter
                    STRING_CONVERTER = new CustomizedStringConverter();
        
        private static final EnumConverter 
                    ENUM_CONVERTER = new EnumConverter();
        
        private static final DatetimeConverter
                    DATE_CONVERTER = new DatetimeConverter();
        
        private static final CustomizedNullConverter
                NULL_CONVERTER = new CustomizedNullConverter();
        
        private static final CustomizedBaseConverter
                BASE_CONVERTER = new CustomizedBaseConverter();
        
        @Override
        @SuppressWarnings({ "rawtypes" })
        public Converter lookup( Class pClazz ) {
            
            if( String.class.equals( pClazz ) ) {
                return STRING_CONVERTER;
            }else if( BaseVO.class.isAssignableFrom( pClazz )
                    || BaseEntity.class.isAssignableFrom( pClazz ) ) {
                return BASE_CONVERTER;
            }else if( Collection.class.isAssignableFrom( pClazz ) ) {
                return NULL_CONVERTER;
            }else if( pClazz.isEnum() ) {
                return ENUM_CONVERTER;
            }else if( Date.class.isAssignableFrom( pClazz ) ) {
                return DATE_CONVERTER;
            }else {
                return super.lookup( pClazz );
            }
            
        }
    }
    
    private static class CustomizedNullConverter extends AbstractConverter {
        
        @Override
        public <T> T convert(Class<T> type, Object value) {
            return null;
        }
        
        @Override
        protected String convertToString( final Object pValue ) throws Throwable {
            return null;
        }
        
        @Override
        protected <T> T convertToType(Class<T> type, Object value) throws Throwable {
            return null;
        }

        @Override
        protected Class<?> getDefaultType() {
            return null;
        }
        
    }
    
    private static class CustomizedBaseConverter extends AbstractConverter {
        
        @Override
        protected String convertToString( final Object pValue ) throws Throwable {
            return null;
        }
        
        @Override
        protected <T> T convertToType(Class<T> type, Object value) throws Throwable {
            return null;
        }

        @Override
        protected Class<?> getDefaultType() {
            return null;
        }
        
    }
    
    private static class CustomizedStringConverter extends AbstractConverter {

        StringConverter strConverter = new StringConverter();
        
        @Override
        protected String convertToString( final Object pValue ) 
                throws Throwable {
            
            if( pValue != null ) {
                if( Timestamp.class.isAssignableFrom( pValue.getClass() ) ) {
                    if( DatetimeUtils.isTimestampZero( 
                            ( Timestamp )pValue ) ) {
                        return "";
                    }
                    return String.valueOf((( Timestamp )pValue).getTime());
                }else if( Date.class.isAssignableFrom( 
                        pValue.getClass() ) ) {
                    
                    if( DatetimeUtils.isDatetimeZero( 
                            ( Date )pValue ) ) {
                        return "";
                    }
                    return String.valueOf(((Date) pValue).getTime());
                    
                }else if( Boolean.class.equals( pValue.getClass() ) ) {
                    return Boolean.TRUE.equals( pValue ) ? 
                            Constants.BOOLEAN_TRUE_IN_STR 
                            : Constants.BOOLEAN_FALSE_IN_STR;
                }else {
                    return pValue.toString();
                }
            }
            
            return null;
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        protected Class getDefaultType() {
            return String.class;
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        protected Object convertToType( Class type, Object value )
                throws Throwable {
            
            if( value == null ) {
                if( Date.class.isAssignableFrom( type ) ) {
                    return Timestamp.class.isAssignableFrom( type ) 
                                ? DatetimeUtils.TIMESTAMP_ZERO 
                                        : DatetimeUtils.DATETIME_ZERO;
                }else if( Number.class.isAssignableFrom( type ) ) {
                    return null;
                }
            }
            
            if( type.isEnum() ) {           
                return Constants.getEnumByValue( type, 
                        value != null ? value.toString() : null );
            }
            
            return strConverter.convert( type, value );
        }

    }    
    
    private static class EnumConverter extends AbstractConverter {

        @Override
        protected String convertToString( final Object pValue ) 
                throws Throwable {
            
            return pValue.toString();
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        protected Class getDefaultType() {
            return null;
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        protected Object convertToType( Class type, Object value )
                throws Throwable {
            
            if( value == null ) {
                return null;
            }
            
            return Constants.getEnumByValue( type, value.toString() );
        }

    }    
    
    private static class DatetimeConverter extends AbstractConverter {
        
        @Override
        protected String convertToString( final Object pValue ) 
                throws Throwable {
            
            if( DatetimeUtils.isDatetimeZero( ( Date )pValue ) ) {
                return "";
            }
            
            return DatetimeUtils.formatDate( ( Date )pValue );
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        protected Class getDefaultType() {
            return null;
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        protected Object convertToType( Class type, Object value )
                throws Throwable {
            
            if( value == null || "".equals( value ) ) {
                return DatetimeUtils.TIMESTAMP_ZERO;
            }
            
            return DatetimeUtils.parseTimestamp( ( String )value );
        }

    }    
    
    /**
     * 将一个集合里的数据反序排列
     * @author: 王树杞/wangshuqi560@163.com
     * @version: 1.0
     */
    public static List<?>  reverse(List<?> list){
        if(list != null && list.size() > 0){
            Collections.reverse(list);
        }
        return list;
    }

    public static String trimDotZero( String number ) {
        if( StringUtils.isEmpty( number ) ) {
            return number;
        }
        int i = number.lastIndexOf( "." );
        return i > 0 ? number.substring( 0, i ) : number;
    }

    /**

     * 将指定的字符串用MD5加密

     * @param originstr 需要加密的字符串

     * @return 加密后的字符串

     */

    public static String ecodeByMD5(String originstr) {

        String result = null;

        char hexDigits[] = {//用来将字节转换成 16 进制表示的字符

                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        if(originstr != null){

            try {

                //返回实现指定摘要算法的 MessageDigest 对象

                MessageDigest md = MessageDigest.getInstance("MD5");



                //使用utf-8编码将originstr字符串编码并保存到source字节数组

                byte[] source = originstr.getBytes("utf-8");

                //使用指定的 byte 数组更新摘要

                md.update(source);

                //通过执行诸如填充之类的最终操作完成哈希计算，结果是一个128位的长整数

                byte[] tmp = md.digest();

                //用16进制数表示需要32位

                char[] str = new char[32];

                for(int i=0,j=0; i < 16; i++){

                    //j表示转换结果中对应的字符位置

                    //从第一个字节开始，对 MD5 的每一个字节

                    //转换成 16 进制字符

                    byte b = tmp[i];

                    //取字节中高 4 位的数字转换

                    //无符号右移运算符>>> ，它总是在左边补0

                    //0x代表它后面的是十六进制的数字. f转换成十进制就是15

                    str[j++] = hexDigits[b>>>4 & 0xf];

                    // 取字节中低 4 位的数字转换

                    str[j++] = hexDigits[b&0xf];

                }

                result = new String(str);//结果转换成字符串用于返回

            } catch (NoSuchAlgorithmException e) {

                //当请求特定的加密算法而它在该环境中不可用时抛出此异常

                logger.error( "exception occurred", e );

            } catch (UnsupportedEncodingException e) {

                //当请求特定的加密算法而它在该环境中不可用时抛出此异常

                logger.error( "exception occurred", e );
            }

        }

        return result;

    }

    public static String hmacSha1(String value, String key) {
        try {
            byte[] keyBytes = key.getBytes();           
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(value.getBytes("UTF-8"));

            byte[] hexBytes = new Hex().encode(rawHmac);

            return new String(hexBytes, "UTF-8");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String sha1(String decript) {  
        try {  
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(decript.getBytes());  
            byte messageDigest[] = digest.digest();  
            // Create Hex String  
            StringBuffer hexString = new StringBuffer();  
            // 字节数组转换为 十六进制 数  
            for (int i = 0; i < messageDigest.length; i++) {  
                String shaHex = Integer.toHexString(messageDigest[i] & 0xFF);  
                if (shaHex.length() < 2) {  
                    hexString.append(0);  
                }  
                hexString.append(shaHex);  
            }  
            
            return hexString.toString();  

        } catch (NoSuchAlgorithmException e) {  
            throw new RuntimeException( e );
        }  
    }
    
    public static <T> List<T>  fromJsonEx( String json, Class<T> clazz ) {

        List<T> list = new ArrayList<T>();
        
        if( StringUtils.isEmpty( json ) ) {
            return null;
        }

        if( clazz == null ) {
            throw new IllegalArgumentException( "class is null!!" );
        }
        Type MAP_ARRAY_LIST_TYPE = 
                new TypeToken<ArrayList<HashMap>>() {}.getType();
                
        List<Map> maps = BaseUtils.fromJson( json, MAP_ARRAY_LIST_TYPE );
        
        if( maps == null || maps.isEmpty() ) {
            return null;
        }
        
        for( Map map : maps ) {
            try{
               
                Object obj = clazz.newInstance();
                BaseUtils.copyProperties( obj, map );
                list.add((T) obj);
            }catch( Exception e ){
                throw new IllegalArgumentException( "cast object error ", e);
            }
        }
        
        return list;
        
    } 
    
    public static String getCaptcha(int n ){
        if( n >10 ){
            throw new IllegalArgumentException("this captcha's max length is too long  ");
        }
        return ( String.valueOf(RandomUtils.nextInt(100000)) + "0000000000" ).substring(0,n) ;
    }
    
    public static String formatDoubleValueSimple( Double doubleValue , int digit ){
        
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits( digit );
        String formatValue = nf.format(doubleValue);
        return StringUtils.replace( formatValue , ".0", "" ).replace(",", "");
        
    }
    
    public static String formatDoubleValue( Double doubleValue , int digit ){

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits( digit );
        String formatValue = nf.format(doubleValue);
        if( !StringUtils.contains( formatValue, "." ) ){
            formatValue +=".00";
        }
        return StringUtils.replace( formatValue ,",", "");
        
    }

    // forbidden to initiate
    private BaseUtils() {}

    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress = null;
        ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0
                || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0
                || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0
                || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (ipAddress.equals("127.0.0.1")) {
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                ipAddress = inet.getHostAddress();
            }

        }

        if (ipAddress != null && ipAddress.length() > 15) {

            if (ipAddress.indexOf(",") > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }

    public static String stringFilter( String str ) throws PatternSyntaxException {
        String regEx = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]" ;
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }
    
    public static <T> void sortByField( List<T> objs, final String fieldName, final boolean asc ) {
        sortByField( null, objs, fieldName, asc );
    }

    public static <T> void sortByField( final Class<? extends Number> numberClass,
            List<T> objs, final String fieldName, final boolean asc ) {

        if( objs == null || objs.isEmpty() || StringUtils.isEmpty( fieldName )
                || objs.size() == 1 ) {
            return;
        }

        Collections.sort( objs, new Comparator<T>() {

            public int compare(T o1, T o2) {
                Number value1 = _toNumber(numberClass, getProperty( o1, fieldName ));
                double d1 = value1 == null ? 0d : value1.doubleValue();
                Number value2 = _toNumber(numberClass, getProperty( o2, fieldName ));
                double d2 = value2 == null ? 0d : value2.doubleValue();
                double d = asc ? d1 - d2 : d2 - d1;
                return d == 0d ? 0 : ( d > 0 ? 1 : -1 );
            }
        });
    }
    
    private static Number _toNumber( Class<? extends Number> numberClass, Object value ) {
        if( value == null ) {
            return null;
        }

        if( numberClass != null ) {
            try {
                return (Number) MethodUtils.invokeStaticMethod(numberClass, "valueOf", value);
            }catch( Exception ex ) {

            }
        }
        
        return (Number)value;
    }
    
    public static <T> Set<T> asSet( T... vals ) {
        if( vals == null || vals.length == 0 ) {
            return new HashSet<T>(0);
        }
        
        Set<T> s = new HashSet<T>( vals.length );
        s.addAll( Arrays.asList(vals) );
        return s;
    }
    
    public static <T> T findByField( Collection<T> coll, String fieldName, Object sid ) {
        if( coll == null || coll.isEmpty() ) {
            return null;
        }
        
        if( StringUtils.isEmpty( fieldName ) ) {
            throw new IllegalArgumentException( "fieldName is empty" );
        }
        
        for( T t : coll ) {
            if( sid.equals( getProperty( t, fieldName )) ) {
                return t;
            }
        }
        return null;
    }
    
}