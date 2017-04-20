/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.common.exception;

public class BaseException extends RuntimeException {
    
    private String resultCode;
    private String msgCode;
    private Object[] msgOptions;
    
    /**
     * 构造函数
     * 
     * @param resultCode 返回值号
     * @param msgCode 消息代码
     * @param msgOptions 消息参数
     */
    public BaseException(String resultCode,
                         String msgCode, Object[] msgOptions) {
        super( resultCode + ":" + msgCode );
        this.resultCode = resultCode;
        this.msgCode = msgCode;
        this.msgOptions = msgOptions;
    }

    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    public Object[] getMsgOptions() {
        return msgOptions;
    }

    public void setMsgOptions(Object[] msgOptions) {
        this.msgOptions = msgOptions;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }
    
}