/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.forge.user.impl;

import com.quien.sera.common.entity.user.Idol;
import com.quien.sera.common.vo.IdolVO;
import com.quien.sera.forge.GenericForgeServiceImpl;
import com.quien.sera.forge.user.IdolForgeService;
import com.quien.sera.shard.user.IdolShardService;
import org.springframework.stereotype.Service;


@Service("idolForgeService")
public class IdolForgeServiceImpl
    extends GenericForgeServiceImpl<IdolVO,
            Idol,
            IdolShardService,
            IdolForgeServiceImpl>
    implements IdolForgeService {

}