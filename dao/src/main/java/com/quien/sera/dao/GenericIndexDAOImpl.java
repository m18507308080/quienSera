/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao;

import com.quien.sera.common.entity.IndexEntity;
import com.quien.sera.common.entity.IndexField;
import com.quien.sera.common.util.DatetimeUtils;
import com.quien.sera.redis.RedisHelper;
import com.quien.sera.redis.RedisNameSpace;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class GenericIndexDAOImpl<X extends IndexEntity,
                                          T extends GenericIndexDAOImpl<X, T>> 
    extends GenericDAOBatisImpl<X, T>
    implements GenericIndexDAO<X> {
    
    @Autowired
    private RedisHelper redisHelper;
    
    @Override
    final public boolean increase( long sid, int no ) {
        return increase( sid, no, 1 );
    }
    
    @Override
    final public boolean increase( final long sid, final int no, final int n ) {
        
        return tryToUpdate( new Callable<Boolean>(){

            @Override
            public Boolean call() throws Exception {
                X indexEntity = findBySid( sid );
                if( indexEntity == null ) {
                    try {
                        // lock globally
                        redisHelper.doWithLock( RedisNameSpace.LOCK_GENERIC_INDEX +
                           GenericIndexDAOImpl.this.getTableName( sid ) + ":" + sid, 
                                new Callable<Boolean>() { 

                            @Override
                            public Boolean call() throws Exception {
                                X xe = GenericIndexDAOImpl.this.newEntity();
                                xe.setSid( sid );
                                IndexField idf = new IndexField();
                                idf.increase( no );
                                xe.setIndexField( idf );
                                return GenericIndexDAOImpl.this.insert( xe ) > 0;
                            }
                        });
                    }catch( Exception ex ) {
                        return false; // need to try again
                    }
                    return true;
                }
                IndexField indexField = indexEntity.getIndexField();
                if( indexField == null ) {
                    indexField = new IndexField();
                    indexEntity.setIndexField( indexField );
                }

                indexField.increase( no, n );

                return _update( indexEntity ) > 0;
            }
        });
    }

    @Override
    final public boolean decrease( long sid, int no ) {
        return this.decrease( sid, no, 1 );
    }
    
    @Override
    final public boolean decrease( long sid, int no, int n ) {
        return this.increase( sid, no, -n );
    }
    
    @Override
    final public int update( X indexEntity ) {        
        List<X> list = new ArrayList<>();
        list.add( indexEntity );
        return this.update( list );
    }
    
    private int _update( X indexEntity ) {
        return getSqlSession().update( 
                getNameSpace( GenericIndexDAOImpl.class, "update" ), 
                newParams().put( VAR_TABLE_NAME, 
                                this.getTableName( indexEntity.getSid() ))
                    .put( "sid", indexEntity.getSid() )
                    .put( "version", indexEntity.getVersion() )
                    .put( "indexField", 
                            indexEntity.getIndexField() == null ? null
                                : indexEntity.getIndexField().toString() )
                    .put( "updatedDatetime", DatetimeUtils.currentTimestamp() ));
    }
    
    /**
     * !!注意！！
     * 该方法不能保证多用户同时修改冲突；
     * <br>调用该方法时请使用全局同步锁保证记录修改的完整性！！
     */
    @Override
    final public int update( List<X> indexEntities ) {
        if( indexEntities == null || indexEntities.isEmpty() ) {
            return 0;
        }
        List<Map<String, Object>> values = new ArrayList<>();
        for( X e : indexEntities ) {
            values.add( newParams().put( VAR_TABLE_NAME, 
                                this.getTableName( e.getSid() ))
                .put( "sid", e.getSid() )
                .put( "version", e.getVersion() )
                .put( "indexField", 
                        e.getIndexField() == null ? null
                                : e.getIndexField().toString() )
                .put( "updatedDatetime", DatetimeUtils.currentTimestamp() ) );
        }
        return getSqlSession().update( 
                getNameSpace( GenericIndexDAOImpl.class, "batchUpdate" ), 
                newParams().put( "values", values ) );
    }
    
}