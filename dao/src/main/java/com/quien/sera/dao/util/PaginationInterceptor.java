/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao.util;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.Properties;

@Intercepts({
    @Signature(type = StatementHandler.class,
            method = "prepare", args={ Connection.class } )
})
public class PaginationInterceptor implements Interceptor {

    private final ObjectFactory objectFactory = new DefaultObjectFactory();
    private final ObjectWrapperFactory objectWrapperFactory =
            new DefaultObjectWrapperFactory();

    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler =
                (StatementHandler)invocation.getTarget();  
        
        MetaObject metaStatementHandler = MetaObject.forObject(
                statementHandler, objectFactory, objectWrapperFactory);
            
        RowBounds rowBounds =
           ( RowBounds )metaStatementHandler.getValue( "delegate.rowBounds" ); 
        
        if( rowBounds == null || rowBounds == RowBounds.DEFAULT ){    
            return invocation.proceed();
        }
        
        String originalSql = ( String )metaStatementHandler
                        .getValue( "delegate.boundSql.sql" );
        
        String boundSql = originalSql 
             + " limit " + rowBounds.getOffset() + "," + rowBounds.getLimit();
        
        metaStatementHandler.setValue( "delegate.boundSql.sql", boundSql );
        metaStatementHandler.setValue( "delegate.rowBounds.offset", 
                RowBounds.NO_ROW_OFFSET );    
        metaStatementHandler.setValue( "delegate.rowBounds.limit", 
                RowBounds.NO_ROW_LIMIT );
        
        return invocation.proceed();
    }

    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    public void setProperties(Properties properties) {
        // TODO Auto-generated method stub
        
    }

}