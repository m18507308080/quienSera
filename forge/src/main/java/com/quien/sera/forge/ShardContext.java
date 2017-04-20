/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.forge;

import com.quien.sera.base.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class ShardContext implements ApplicationContextAware {
    
    private final static Logger logger = 
            LoggerFactory.getLogger( ShardContext.class );
    
    private final static Pattern SHARD_CTX_FILE = 
            Pattern.compile( "^(shardContext.*)\\.xml$" );
    
    private final static Map<String, ApplicationContext> shardContexts = 
                new ConcurrentHashMap<String, ApplicationContext>();

    
    public final static String MASTER_CTX_ID = "0";
    
    public Collection<ApplicationContext> getShardContexts() {
        return shardContexts.values();
    }
    
    public ApplicationContext getShardContextById( String id ) {
        return shardContexts.get( id );
    }
    
    public ApplicationContext getMasterShardContext() {
        return getShardContextById( MASTER_CTX_ID );
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if( logger.isWarnEnabled() ) {
            logger.warn( "ShardContext = " + this );
            logger.warn( "application contextPath = " + Environment.contextPath() );
        }
        
        Map<String, String> ctxFiles = new HashMap<String, String>();
        
        Path contextDir = Environment.contextPath();
        
        scanShardContextFiles( contextDir.resolve( "shard-context" ), 
                ctxFiles );
        
        Path envPath = contextDir.resolve( "shard-context/" + Environment.ENV );
        
        // scan & initiate shard contexts
        for( String name : envPath.toFile().list() ) {
            
            Path p = envPath.resolve( name );
            File f = p.toFile();
            
            if( f.isDirectory() ) {
                Map<String, String> envFiles = new HashMap<String, String>();
                envFiles.putAll( ctxFiles );

                scanShardContextFiles( p, envFiles );
                
                initShardContext( name, envFiles.values() );
            }
        }
    
    }

    private void scanShardContextFiles( Path dir, Map<String, String> ctxFiles ) {
        for( String name : dir.toFile().list() ) {
            Path p = dir.resolve( name );
            File f = p.toFile();
            if( f.isFile() && SHARD_CTX_FILE.matcher( name ).matches() ) {
                ctxFiles.put( name, "file:" + String.valueOf( p ) );
            }
        }
    }

    protected void initShardContext( String id, Collection<String> configFiles ) {
        StringBuilder sbd = null;
        if( logger.isWarnEnabled() ) {
            sbd = new StringBuilder();
            sbd.append( "================================================================\n" );
            sbd.append( "initiating shard context: " + id + "\n" );

            for( String fileName : configFiles ) {
                sbd.append( fileName ).append( "\n" );
            }
        }
        
        ApplicationContext tempCtx = (FileSystemXmlApplicationContext)shardContexts.get( id );
        
        if( tempCtx != null ) {
            
            sbd.append( "\nshard context [" + id + "] already created, then skip to create it again.\n" );
            
        }else {

            FileSystemXmlApplicationContext ctx = 
                    new FileSystemXmlApplicationContext( 
                       configFiles.toArray( new String[]{} ) );

            ctx.setId( id );

            shardContexts.put( id, ctx );
        }
        
        if( logger.isWarnEnabled() ) {
            sbd.append( "\nshard context [" + id + "] initiated successfully!!\n" );
            sbd.append( "================================================================\n" );
            logger.warn( sbd.toString() );
        }
    }
    
}