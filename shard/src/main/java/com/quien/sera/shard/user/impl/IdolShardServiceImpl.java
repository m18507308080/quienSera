/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.shard.user.impl;

import com.quien.sera.common.entity.user.Idol;
import com.quien.sera.dao.user.IdolDAO;
import com.quien.sera.shard.GenericShardServiceImpl;
import com.quien.sera.shard.user.IdolShardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("idolShardService")
public class IdolShardServiceImpl
        extends GenericShardServiceImpl<
                Idol,
                IdolShardServiceImpl>
        implements IdolShardService {

    private final IdolDAO idolsDAO;

    @Autowired
    public IdolShardServiceImpl( IdolDAO idolsDAO) {
        super( idolsDAO );
        this.idolsDAO = idolsDAO;
    }

}