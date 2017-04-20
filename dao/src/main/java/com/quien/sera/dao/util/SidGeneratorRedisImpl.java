/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao.util;

import com.quien.sera.common.entity.BaseEntity;
import com.quien.sera.redis.RedisHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SidGeneratorRedisImpl implements SidGenerator {

    @Autowired
    private RedisHelper redisHelper;

    public <E extends BaseEntity> long generate( Class<E> entityClass ) {
        return redisHelper.sid( entityClass.getName() );
    }

    public <E extends BaseEntity> long peek( Class<E> entityClass ) {
        return redisHelper.peekSid( entityClass.getName() );
    }
    
}