/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.common.constant;

public final class ResultCode {
    
    public final static String OK = "0";
    
    public final static String ERROR = "1";
    
    public final static String SUCCESS = OK;
    
    public final static String FAILURE = ERROR;

    public final static String REQUEST_NOT_SUPPORTED = "2";

    // keep from initiation
    private ResultCode() {}
}