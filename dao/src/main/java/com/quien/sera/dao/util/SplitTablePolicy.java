/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao.util;

import com.quien.sera.common.entity.BaseEntity;
import com.quien.sera.dao.GenericDAO;

public interface SplitTablePolicy {

    <E extends BaseEntity, T extends GenericDAO<E>>
    String getTableName(Class<T> daoImplClass, Class<E> entityClass, long sid);

    <E extends BaseEntity, T extends GenericDAO<E>> 
    String getTableName(Class<T> daoImplClass, Class<E> entityClass);
    
    String getTableName(String tableName, long sid);
    
    <E extends BaseEntity, T extends GenericDAO<E>> 
    int getTableNo(Class<T> daoImplClass, Class<E> entityClass, long sid);
    
    int getTableNo(String tableName, long sid);
}