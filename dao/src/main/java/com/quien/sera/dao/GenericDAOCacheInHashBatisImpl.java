/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao;

import com.quien.sera.common.entity.BaseEntity;
import com.quien.sera.common.util.BaseUtils;
import java.util.Collection;
import java.util.List;

public class GenericDAOCacheInHashBatisImpl <E extends BaseEntity,
                                 T extends GenericDAOBatisImpl<E, T>>
    extends GenericDAOBatisImpl<E, T> {
    
    
    @Override
    public E findBySid( Long sid, boolean ignoreFlag ) {
        return findBySidInHash( sid, ignoreFlag );
    }
    
    @Override
    public List<E> findBySid( Collection<Long> sids ) {
        
        return findBySidInHash( sids );
    }
    
    @Override
    public List<E> findBySid( Collection<Long> sids, boolean ignoreFlag ) {
        return findBySidInHash( sids, ignoreFlag );
    }
    
    // 增加指定字段数值
    @Override
    public void increaseFieldValue( Long sid, String fieldName, int n ) {
        increaseFieldValueInHash( sid, fieldName, n, true );
    }
    
    @Override
    public void resetFieldValue( Long sid, String fieldName ) { 
        resetFieldValueInHash( sid, fieldName );
    }
    
    @Override
    protected void removeFromCache( Collection<Long> sids ) {
        if( sids == null || sids.isEmpty() ) {
            return;
        }
        
        redisHelper.removeFromHash( entityClass, BaseUtils.mapToString(sids) );
    }
    
}