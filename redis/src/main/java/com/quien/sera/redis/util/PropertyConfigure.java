/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.redis.util;

import com.quien.sera.redis.RedisHelper;
import com.quien.sera.redis.RedisHelperJedisImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertyConfigure extends PropertyPlaceholderConfigurer {

	private final static Map<String, Map<String, String>> configMap = new HashMap<String, Map<String, String>>();

	private final static Map<String, String> flatConfigMap = new HashMap<String, String>();
        
        static {
            // read config info from redis
            RedisHelper redisHelper = new RedisHelperJedisImpl();
            
            for (RedisConfigType config : RedisConfigType.values()) {

                    Map<String, String> map = redisHelper.getSpecialConfigMap( config.toString());

                    configMap.put(config.toString(), map);

                    flatConfigMap.putAll(map);

            }
        }
        
	public PropertyConfigure() {
	}

	@Override
	protected void processProperties(
			ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException {

		for (String key : flatConfigMap.keySet()) {
			props.put(key, flatConfigMap.get(key));
		}

		super.processProperties(beanFactory, props);
	}


    public String getRedisConfig() {
		return flatConfigMap.get("quien.sera.redis.config");
	}

	public Map<String, String> getAllConfig() {
		return flatConfigMap;
	}

	enum RedisConfigType {
		REDIS("redis"), 
		JDBC("jdbc"),
		USER("user");
		String key;

		RedisConfigType(String key) {
			this.key = key;
		}

		@Override
		public String toString() {
			return this.key;
		}
	}

}