/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.common.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.quien.sera.common.constant.ResultCode;
import org.apache.commons.lang.StringUtils;
import java.util.HashMap;
import java.util.Map;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ResultVO {

    private String code;
    private Map<String, String> messages;
    private String defaultMessage;
    private Object result;

    public ResultVO() {
        this( ResultCode.SUCCESS );
    }

    public ResultVO(String code) {
        this( code, null );
    }

    public ResultVO(String code, Object result) {
        this( code, null, result );
    }

    public ResultVO(String code, String message) {
        this( code, message, null );
    }

    public ResultVO(String code, String message, Object result) {
        this.code = code;
        if( StringUtils.isNotEmpty(message) ) {
            this.messages = new HashMap<String, String>();
            this.defaultMessage = message;
        }
        this.result = result;
    }

    public static ResultVO success( String message ) {
        return new ResultVO( ResultCode.SUCCESS, message );
    }

    public static ResultVO success( Object result ) {
        return new ResultVO( ResultCode.SUCCESS, result );
    }

    public static ResultVO fail( String message ) {
        return new ResultVO( ResultCode.FAILURE, message );
    }

    public static ResultVO fail( Object result ) {
        return new ResultVO( ResultCode.FAILURE, result );
    }

    public String getCode() {
        return code;
    }

    public ResultVO setCode(String code) {
        this.code = code;
        return this;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    public String getMessage( String key ) {
        return this.messages != null ? this.messages.get( key ) : null;
    }

    public ResultVO appendMessage( String key, String message ) {
        if( key == null || "".equals( key ) ) {
            return this;
        }
        if( message == null || "".equals( message ) ) {
            return this;
        }
        if( this.messages == null ) {
            this.messages = new HashMap<String, String>();
        }

        this.messages.put( key, message );

        return this;
    }

    public ResultVO setDefaultMessage( String message ) {
        this.defaultMessage = message;
        return this;
    }

    public String getDefaultMessage() {
        return this.defaultMessage;
    }

}