/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao;

import com.quien.sera.common.entity.IndexEntity;

public interface GenericIndexDAO<X extends IndexEntity> extends GenericDAO<X> {

    boolean increase(long sid, int no);
    
    boolean increase(long sid, int no, int n);

    boolean decrease(long sid, int no);
    
    boolean decrease(long sid, int no, int n);
}