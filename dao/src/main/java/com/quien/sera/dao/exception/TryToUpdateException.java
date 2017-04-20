/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao.exception;

public class TryToUpdateException extends RuntimeException {
    
    public TryToUpdateException() {
        super();
    }
    
    public TryToUpdateException(Exception ex) {
        super( ex );
    }
    
    public TryToUpdateException(String message) {
        super( message );
    }
    
    public TryToUpdateException(String message, Exception ex) {
        super( message, ex );
    }
   
}