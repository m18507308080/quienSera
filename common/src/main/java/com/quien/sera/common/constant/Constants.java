/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.common.constant;

import java.util.HashMap;
import java.util.Map;

public final class Constants {

    // 设置HTTP最大连接数为10
    public static final Integer OSS_UPLOAD_MAX_CONNECTIONS = 10 ;

    // 设置TCP连接超时为5000毫秒
    public static final Integer OSS_UPLOAD_CONNECTION_TIMEOUT = 5000;

    // 设置最大的重试次数为3
    public static final Integer OSS_UPLOAD_MAX_ERROR_RETRY = 3;

    public static final String PATH_SEPARATOR = "/";

    // 设置Socket传输数据超时的时间为2000毫秒
    public static final Integer OSS_UPLOAD_SOCKET_TIMEOUT = 2000;
    
    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded;charset=UTF-8";

    public static final String ACCEPT = "application/json";

    public static final String MOBILE_HEADER = "mobileheader";
    
    public static final String SIGNATURE_SEPARATOR = "&";
    
    public static final String BOOLEAN_TRUE_IN_STR = "1";
    
    public static final String BOOLEAN_FALSE_IN_STR = "0";
    
    public static final int QUERY_TYPE_UP = 1;
    
    public static final int QUERY_TYPE_DOWN = 0;
    
    public static final int DEFAULT_VERSION = 1 ;

    public static final Long SYS_OPERATOR = 0L;

    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    
    public static final String NULL = "<null>";
    
    //默认的redis服务器
    public static final String DEFAULT_REDIS_GROUP_ID = "default";
    
    //默认的redis服务器
    public static final String DEFAULT_REDIS_GROUP_NAME = "default";
    
    //默认的redis config服务器
    public static final String DEFAULT_CONFIG_REDIS_GROUP_ID = "config";
    
    //默认的redis sid存放的 服务器
    public static final String REDIS_SID_GROUP_NAME = "sid";
    
    //默认的redis lock存放的 服务器
    public static final String REDIS_LOCK_GROUP_NAME = "lock";

    //默认的redis config服务器
    public static final String DEFAULT_CONFIG_REDIS_GROUP_NAME = "config";

    private static final Map<Class<? extends Enum<?>>, Enum<?>[]>
            mapEnumTypes = new HashMap<Class<? extends Enum<?>>, Enum<?>[]>();

    // 记录读取方向枚举
    public static enum ReadDirection {
        UP( 0 ),
        DOWN( 1 );

        private final int value;

        ReadDirection( int value ) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static Enum<?> getEnumByValue( Class<? extends Enum<?>> enumClass,
                                          String value ) {

        if( enumClass == null || ! enumClass.isEnum() ) {
            throw new IllegalArgumentException(
                    "Argument enumClass is null!!" );
        }

        if( value == null || "".equals( value ) ) {
            return null;
        }

        for( Class<? extends Enum<?>> eclass : mapEnumTypes.keySet() ) {
            if( eclass.equals( enumClass ) ) {
                for( Enum<?> e : mapEnumTypes.get( eclass ) ) {
                    if( e.toString().equals( value ) ) {
                        return e;
                    }
                }
            }
        }

        throw new IllegalArgumentException(
                "Enum class \"" + enumClass.getName() + "\""
                        + " with value \"" + value + "\""
                        + " is not supported!!" );
    }

    // forbidden to initiate
    private Constants() {}
}