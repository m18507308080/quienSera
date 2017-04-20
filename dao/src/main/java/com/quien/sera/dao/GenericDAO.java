/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao;

import com.quien.sera.common.entity.BaseEntity;
import com.quien.sera.common.entity.IndexField;
import java.util.Collection;
import java.util.List;

public interface GenericDAO<E extends BaseEntity> {
    
    E findBySid(Long sid);
    
    E findByMasterSid(Long masterSid, Long sid);
    
    E findBySid(Long sid, boolean ignoreFlag);
    
    E findByMasterSid(Long masterSid, Long sid, boolean ignoreFlag);
            
    List<E> findBySid(Collection<Long> sids);
    
    List<E> findBySid(Collection<Long> sids, boolean ignoreFlag);
    
    List<E> selectAll();
    
    List<E> selectByRange(int i, int n, IndexField indexField);
    
    List<E> selectTheFirstOnePerTable();
    
    int insert(E entity);
    
    void insert(Long masterSid, E entity);
    
    void insert(Long masterSid, List<E> entities);
    
    int insert(List<E> entities);
    
    int update(E entity);
    
    int update(List<E> entities);
    
    int deleteBySid(Long sid);
    
    int deleteBySid(Collection<Long> sids);
    
    int delete(E entity);
    
    int delete(Collection<E> entities);
    
    int getTableNo(Long sid);
    
    void increaseFieldValue(Long sid, String fieldName);
    
    void increaseFieldValue(Long sid, String fieldName, int n);
    
    void resetFieldValue(Long sid, String fieldName);
    
    void setActived(Long sid, boolean actived);
    
}