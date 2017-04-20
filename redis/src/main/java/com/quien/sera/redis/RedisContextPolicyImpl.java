/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.redis;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component("redisServerGroupPolicy")
public class RedisContextPolicyImpl implements RedisContextPolicy {

    private static final Logger logger = LoggerFactory.getLogger( RedisContextPolicyImpl.class );
    
    private static final Map<String, String> redisMappingMap = new HashMap<>();
    
    private static final List<Pattern> patterns = new ArrayList<>();
    private static final List<String> patternIds = new ArrayList<>();

    static {

        InputStream ii = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream( "redis.mapping" );
        try {
            if (ii != null) {
                
                BufferedReader r = new BufferedReader(new InputStreamReader(ii));
                
                String line = null;
                
                while ((line = r.readLine()) != null) {
                    
                    line = StringUtils.trim( line );
                    
                    if( StringUtils.isNotEmpty( line ) 
                            && ( ! line.startsWith( "#" ) ) ) {
                        
                        int idx = line.lastIndexOf( '=' );
                        
                        if( idx > 0 ) {
                            patterns.add( Pattern.compile( line.substring( 0, idx ) ) ); 
                            patternIds.add( line.substring( idx + 1 ) );
                        }
                    }
                }
            }
        }catch( Exception ex ) {
            logger.error( "RedisContextPolicyImpl static init exception", ex );
        } finally {
            try {
                if( ii != null ) {
                    ii.close();
                    ii = null;
                }
            } catch (Exception ex) {

            }
        }
    }

    @Override
    public String getRedisServerIdGroupByName(String name) {

        if( StringUtils.isEmpty( name ) ) {
            throw new IllegalArgumentException( "name is empty" );
        }
        
        String id = redisMappingMap.get( name );
        
        if( StringUtils.isEmpty( id ) ) {
            for( int i = 0; i < patterns.size(); i++ ) {
                Pattern p = patterns.get( i );
                if( p.matcher( name ).matches() ) {

                    id = patternIds.get( i );
                    
                    if( StringUtils.isNotEmpty( id ) ) {
                        redisMappingMap.put( name, id );
                    }
                    
                    break;
                }
            }
        }
        
        return id;
    }

}