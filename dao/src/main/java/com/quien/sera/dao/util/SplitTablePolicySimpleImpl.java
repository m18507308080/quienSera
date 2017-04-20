/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao.util;

import com.quien.sera.base.Environment;
import com.quien.sera.common.entity.BaseEntity;
import com.quien.sera.dao.GenericDAO;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SplitTablePolicySimpleImpl implements SplitTablePolicy {
    
    protected static final String TABLE_NAME_PREFIX = "t_";
    
    private static final Pattern P_DAOIMPL_NAME = 
                            Pattern.compile( "^(.+)DAOImpl$" );
    
    private static final Pattern P_MILLION = 
                            Pattern.compile( "^(\\d+)(m|M)?$" );
                            
    private static final Map<Class, String> 
                            TABLE_NAME_MAP = new HashMap<>();

    private final Map<String, Long> splitProperties;
    
    public SplitTablePolicySimpleImpl() {
       
        splitProperties = new HashMap<>();
         
        Properties properties = Environment.loadProperties(
                "split-table-policy-simple.properties");
        
        for( Object key : properties.keySet() ) {
            String number = StringUtils.trim( properties.getProperty( (String)key ) );
            if( StringUtils.isNotEmpty( number ) ) {
                Matcher matcher = P_MILLION.matcher( number );
                if( matcher.matches() ) {
                    int i = Integer.valueOf( matcher.group( 1 ) );
                    long m = ( "m".equalsIgnoreCase( matcher.group( 2 ) ) ? 1000000L : 1L );
                    Long rows = i * m;
                    for( String tableName : ((String)key).split( "," )) {
                        splitProperties.put( tableName, rows );
                    }
                }
            }
        }
        
    }
    
    @Override
    public <E extends BaseEntity, T extends GenericDAO<E>> 
    String getTableName( Class<T> daoImplClass, Class<E> entityClass, long sid ) {
        
        return generateTableName( 
                generateRawTableName( daoImplClass, entityClass ), 
                sid );
    }
    
    @Override
    public String getTableName( String tableName, long sid ) {
        return generateTableName( tableName, sid );
    }
    
    @Override
    public <E extends BaseEntity, T extends GenericDAO<E>>
    String getTableName( Class<T> daoImplClass, Class<E> entityClass ) {
        
        return generateTableName( 
                generateRawTableName( daoImplClass, entityClass ), 
                1L );
    }
    
    @Override
    public <E extends BaseEntity, T extends GenericDAO<E>> 
    int getTableNo( Class<T> daoImplClass, Class<E> entityClass, long sid ) {
        return generateTableNo( 
                generateRawTableName( daoImplClass, entityClass ), 
                sid );
    }
    
    @Override
    public int getTableNo( String tableName, long sid ) {
        return generateTableNo( tableName, sid );
    }
    
    protected String generateTableName( String tableName, Long sid ) {
        int tno = generateTableNo( tableName, sid );
        return tableName + ( tno <= 0 ? "" : "_" + tno );
    }
    
    protected <E extends BaseEntity, T extends GenericDAO<E>>
    String generateRawTableName( Class<T> daoImplClass, 
                                        Class<E> entityClass ) {
        
        String tableName = TABLE_NAME_MAP.get( daoImplClass );
        
        if( StringUtils.isNotEmpty( tableName ) ) {
            return tableName;
        }
        
        StringBuilder sbd = new StringBuilder();
        
        Matcher m = P_DAOIMPL_NAME.matcher( daoImplClass.getSimpleName() );
        
        if( ! m.matches() ) {
            throw new IllegalArgumentException( 
                    daoImplClass.getSimpleName() 
                    + " is illegal DAOImpl class!!" );
        }
        
        for( char c : m.group( 1 ).toCharArray() ) {
            if( c >= 'A' && c <= 'Z' ) {
                if( sbd.length() > 0 ) {
                    sbd.append( '_' );
                }
                sbd.append( String.valueOf( c ).toLowerCase() );
                
            }else {
                sbd.append( c );
            }
        }
        
        tableName = TABLE_NAME_PREFIX + sbd.toString();
        
        TABLE_NAME_MAP.put( daoImplClass, tableName );
        
        return tableName;
        
     }
    
    protected int generateTableNo( String tableName, Long sid ) {
        Long maxRows = splitProperties.get( tableName );
        int tno = 0;
        if( maxRows != null && sid >= maxRows ) {
            tno = ( int )Math.round( Math.floor( sid / maxRows ) );
        }
        return tno;
    }
    
}