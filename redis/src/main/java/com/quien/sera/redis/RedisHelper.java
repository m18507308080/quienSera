/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.redis;

import redis.clients.jedis.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public interface RedisHelper {

    int EXPIRE_IN_SECONDS = 30 * 60; // 30 mins
    
    Map<String,String> getSpecialConfigMap(String key);

	Boolean exists(final String key);
	
	Boolean exists(Class<?> clazz, final String key);
	
	List<String> mget(final String... keys);
	
	List<String> mget(Class<?> clazz, final String... keys);
	
    String set(String key, String value);

    String set(Class<?> clazz, String key, String value);

    String setex(final String key, final int seconds, final String value);
    
    String setex(Class<?> clazz, final String key, final int seconds, final String value);

    String get(String key);

    String get(Class<?> clazz, String key);

    long del(String... keys);

    long del(Class<?> clazz, String... keys);

    Long zrem(final String key, final String... members);
    
    Long zrem(Class<?> clazz, final String key, final String... members);

    Long zadd(String key, double score, String value);
    
    Long zadd(Class<?> clazz, String key, double score, String value);

    Long zadd(final String key, final Map<String, Double> scoreValue);

    Long zadd(Class<?> clazz, final String key, final Map<String, Double> scoreValue);

    Double zincrby(final String key, final double score, final String member);
    
    Set<String> zrevrange(String key, long start, long end);

    Set<String> zrange(final String key, final long start, final long end);
    
    Set<Tuple> zrevrangeByTuple(String key, long start, long end);
    
    Set<String> zrevrange(Class<?> clazz, String key, long start, long end);

    Set<Tuple> zrevrangeWithScores(String key, long start, long end);

    Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max,
                                       final int offset, final int count) ;
    
    Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max,
                                          final double min, final int offset, final int count);
    
    Long zrevrank(String key, String value);
    
    Double zscore(String key, String member);
    
    Long zrevrank(Class<?> clazz, String key, String value);

    long incr(String key);

    long incr(Class<?> clazz, String key);

    String hmset(String key, Map value);

    String hmset(Class<?> clazz, String key, Map value);

    List<String> hmget(String key, String... fields);
    
    List<String> hmget(Class<?> clazz, String key, String... fields);

    int expire(final String key, final int seconds);
    
    int expire(Class<?> clazz, final String key, final int seconds);

    Map<String, String> hgetAll(String key);
    
    Map<String, String> hgetAll(Class<?> clazz, String key);

    Long hincrBy(final String key, final String field, final long value);
    
    Long hincrBy(Class<?> clazz, final String key, final String field, final long value);
    
    String hget(final String key, final String field);
    
    String hget(Class<?> clazz, final String key, final String field);
    
    Long hset(final String key, final String field, final String value);
    
    Long hset(Class<?> clazz, final String key, final String field, final String value);

    Long sadd(String key, final String... members);
    
    Long sadd(Class<?> clazz, String key, final String... members);
    
    Long hdel(final String key, final String... fields);
	
    Long hdel(Class<?> clazz, final String key, final String... fields) ;
    
    void lpush(String key, String... values);
    
    void lpush(Class<?> clazz, String key, String... values);
    
    void rpush(String key, String... values);

    void lrem(String key, String values);
    
    void lrem(Class<?> clazz, String key, String values);

    List<String> lrange(String key);

    List<String> lrange(String key, Long start, Long end);
    
    List<String> lrange(Class<?> clazz, String key);

    Set<String> smembers(final String key);

    long sid(String name);

    long generateNO(String key);

    <T> T doWithLock(String lockName, Callable<T> callback);

    void putInCache(Class<?> clazz, String key, String value);

    void putInCache(Class<?> clazz, String key, String value, boolean expire);

    void putInCache(Class<?> clazz, Map<String, String> values);

    void putInCache(Class<?> clazz, Map<String, String> values, boolean expire);

    void expireCache(Class<?> clazz, String key);

    void expireCache(Class<?> clazz, String key, int seconds);
    
    String getFromCache(Class<?> clazz, String key);

    Map<String, String> getFromCache(Class<?> clazz, List<String> keys);

    void removeFromCache(Class<?> clazz, String key);

    long peekSid(String name) ;

    void removeFromCache(Class<?> clazz, List<String> keys);

    <T> void putInHashNX(Class<T> clazz, String key, T obj);

    <T> void putInHashNX(Class<T> clazz, String key, T obj, boolean expire);

    <T> void putInHash(Class<T> clazz, String key, T obj);

    <T> void putInHash(Class<T> clazz, String key, T obj, boolean expire);

    <T> void putInHash(Class<T> clazz, Map<String, T> keyObjMap);

    <T> void putInHash(Class<T> clazz, Map<String, T> keyObjMap, boolean expire);

    <T> T getFromHash(Class<T> clazz, String key);

    <T> Map<String, T> getFromHash(Class<T> clazz, List<String> keys);

    void setFieldValueInHash(Class<?> clazz, String key, String field,
                             String value);

    void setFieldValueInHash(Class<?> clazz, String key, Map<String, String> map);

    void increaseFieldValueInHash(Class<?> clazz, String key, String field,
                                  int n);
            
    void increaseFieldValueInHash(Class<?> clazz, String key,
                                  String field, int n, boolean expire) ;

    void increaseFieldValueInHash(Class<?> clazz, final String key,
                                  final Map<String, Integer> values);

    void removeFromHash(Class<?> clazz, String key);

    void removeFromHash(Class<?> clazz, List<String> keys);

}