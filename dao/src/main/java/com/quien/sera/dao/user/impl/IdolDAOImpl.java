/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao.user.impl;

import com.quien.sera.common.entity.user.Idol;
import com.quien.sera.dao.GenericDAOBatisImpl;
import com.quien.sera.dao.user.IdolDAO;
import org.springframework.stereotype.Repository;

@Repository
public class IdolDAOImpl
    extends GenericDAOBatisImpl<Idol, IdolDAOImpl>
    implements IdolDAO {

}