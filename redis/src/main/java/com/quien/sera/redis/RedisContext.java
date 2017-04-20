/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisContext {
    
    private final static Logger logger = LoggerFactory.getLogger( RedisContext.class );
    
    private final static String CONFIG = "config";
    private final static String DEFAULT = "default";
    
    private final static RedisContextPolicy policy = new RedisContextPolicyImpl();
    
    private final Map<String, Redis> mapRedis = new HashMap<>();
    
    private final List<RedisConfig> redisConfigs;
    
    public RedisContext( List<RedisConfig> redisConfigs ) {
        this.redisConfigs = redisConfigs;
        init();
    }
    
    public Redis getConfigRedis() {
        return mapRedis.get( CONFIG );
    }
    
    public Redis getDefaultRedis() {
        return mapRedis.get( DEFAULT );
    }
    
    public Redis getBy( Class clazz ) {
        return getByPolicy( clazz.getName() );
    }
    
    protected Redis getByPolicy( String name ) {
        return mapRedis.get( policy.getRedisServerIdGroupByName( name ) );
    }
    
    private void init() {
        if( redisConfigs == null || redisConfigs.isEmpty() ) {
            return;
        }
        
        StringBuilder sbd = new StringBuilder();
        sbd.append( "-----------------------------------------------\n" )
           .append( "RedisContext initiating...\n" );
        
        for( RedisConfig rc : redisConfigs ) {
            
            mapRedis.put( rc.getId(), new Redis( rc ) );
            
            sbd.append( "{id=" ).append( rc.getId() )
               .append( ", host=" ).append( rc.getIp() )
               .append( ", port=" ).append( rc.getPort() )
               .append( ", password=" )
                    .append( StringUtils.isEmpty( rc.getPassword() ) ? "" : "***" )
               .append( "}\n" );
        }
        
        sbd.append( "RedisContext initiated successfully\n" )
           .append( "-----------------------------------------------\n" );
        
        logger.warn( sbd.toString() );
    }
    
}