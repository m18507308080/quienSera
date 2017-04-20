/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao.util;

import com.quien.sera.base.Environment;
import com.quien.sera.common.constant.Constants;
import com.quien.sera.common.entity.BaseEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
public class ReadDatetimePolicyImpl implements ReadDatetimePolicy {

    private final Map<String, Long> valveMilliSeconds;
    
    public ReadDatetimePolicyImpl() {
       
        valveMilliSeconds = new HashMap<>();
         
        Properties properties = Environment.loadProperties(
                "read-datetime-policy.properties");
        
        for( Object key : properties.keySet() ) {
            String number = properties.getProperty( (String)key );
            if( StringUtils.isNotEmpty( number ) ) {
                    valveMilliSeconds.put( 
                            ( String )key, Long.valueOf( number ) * 1000 );
            }
        }
    }
    
    @Override
    public Long getReadTimestamp( Class<? extends BaseEntity> entityClass,
            Long timestampInMilliseconds, 
            Constants.ReadDirection direction ) {
        
        if( Constants.ReadDirection.DOWN.equals( direction ) ) {
            return timestampInMilliseconds;
        }
        
        if( timestampInMilliseconds == null ) {
            return null;
        }
        
        long delta = System.currentTimeMillis() - timestampInMilliseconds;

        Long time = valveMilliSeconds.get( entityClass.getSimpleName() );
        time = ( time == null ? Long.MAX_VALUE : time );

        if( delta <= 0 || delta > time ) {
            return null; // to read from the most updated one
        }
        
        return timestampInMilliseconds;
    }
    
}