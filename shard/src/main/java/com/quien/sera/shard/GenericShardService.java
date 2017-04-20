/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.shard;

import com.quien.sera.common.entity.BaseEntity;
import java.util.Collection;
import java.util.List;

public interface GenericShardService<E extends BaseEntity> {

    E getBySid(Long sid);

    E getBySid(Long sid, boolean ignoreFlag);
    
    E getByMasterSid(Long masterSid, Long sid);
    
    List<E> getBySid(Collection<Long> sids);

    List<E> getBySid(Collection<Long> sids, boolean ignoreFlag);
    
    /**
     * update or insert the entity base on the field 'sid' has value or not.
     * 
     * @param entity
     * @return 
     */
    int save(E entity);
    
    int add(E entity);

    int add(List<E> entities);
    
    Long generateSid();
    
    Long generateSid(Class<? extends BaseEntity> entityClass);
    
}