/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao.util;

import com.quien.sera.common.entity.BaseEntity;

public interface SidGenerator {
    
    <E extends BaseEntity>
    long generate(Class<E> entityClass);
    
    <E extends BaseEntity>
    long peek(Class<E> entityClass);
}