/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.redis;

import com.google.gson.reflect.TypeToken;
import com.quien.sera.base.Environment;
import com.quien.sera.common.constant.Constants;
import com.quien.sera.common.exception.BaseException;
import com.quien.sera.common.util.BaseUtils;
import com.quien.sera.redis.util.PropertyConfigure;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.Callable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RedisHelperJedisImpl implements RedisHelper {

    private static final Logger logger = LoggerFactory
            .getLogger(RedisHelperJedisImpl.class);

    private static final int EXPIRE_IN_SECONDS = RedisHelper.EXPIRE_IN_SECONDS; // 30 mins

    private final static String INIT_REDIS_CONFIG = "redis.json";

    private final static String REDIS_CONFIG;

    private RedisContext redisContext;

    private final static String IP_ACCESS_RECORD_CLASS = "ip.access.path.method";

    static {

        logger.warn("========== 从redis.json读取配置信息 ==========");
        REDIS_CONFIG = extractRedisConfigProperties();

    }

    public RedisHelperJedisImpl() {
        this(REDIS_CONFIG);
    }

    public RedisHelperJedisImpl(PropertyConfigure propConfig) {
        this(propConfig.getRedisConfig());
    }

    public RedisHelperJedisImpl(String jsonStrConfig) {

        if (StringUtils.isNotEmpty(jsonStrConfig)) {

            Type type = new TypeToken<ArrayList<RedisConfig>>() {
            }.getType();

            List<RedisConfig> redisServerConfigList = BaseUtils.fromJson(
                    jsonStrConfig, type);

            redisContext = new RedisContext(redisServerConfigList);
        }

    }

    @Override
    public Map<String, String> getSpecialConfigMap(String key) {

        String configKey = RedisNameSpace.QUIEN_SERA_REDIS_CONFIG
                + Constants.PATH_SEPARATOR + key;

        return redisContext.getConfigRedis().hgetAll(configKey);
    }

    private static String extractRedisConfigProperties() {

        try {
            String content = Environment.loadContent(Environment.ENV
                    + Constants.PATH_SEPARATOR + INIT_REDIS_CONFIG);
            return content;

        } catch (IOException e) {

            logger.error(" can not find the file : redis.properties ！！！！！");
        }
        return null;

    }

    @Override
    public String set(String key, String value) {
        return redisContext.getDefaultRedis().set(key, value);
    }

    @Override
    public String set(Class<?> clazz, String key, String value) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return redisContext.getBy(clazz).set(key, value);
    }

    protected Boolean sisMember(String key, String value) {
        return redisContext.getDefaultRedis().sisMember(key, value);
    }

    protected Boolean sisMember(Class<?> clazz, String key, String value) {

        return redisContext.getBy(clazz).sisMember(key, value);
    }

    protected Set<String> sMembers(String key) {
        return redisContext.getDefaultRedis().sMembers(key);
    }

    protected Set<String> sMembers(Class<?> clazz, String key) {

        return redisContext.getBy(clazz).sMembers(key);
    }

    @Override
    public Long zadd(String key, double score, String value) {

        return redisContext.getDefaultRedis().zadd(key, score, value);
    }

    @Override
    public Long zadd(Class clazz, String key, double score, String value) {
        return redisContext.getBy(clazz).zadd(key, score, value);
    }

    @Override
    public Long zadd(final String key, final Map<String, Double> scoreValue) {
        return redisContext.getDefaultRedis().zadd(key, scoreValue);
    }

    @Override
    public Long zadd(Class<?> clazz, final String key,
                     final Map<String, Double> scoreValue) {

        return redisContext.getBy(clazz).zadd(key, scoreValue);
    }

    @Override
    public Double zincrby(final String key, final double score,
                          final String member) {
        return redisContext.getDefaultRedis().zincrby(key, score, member);
    }

    @Override
    public Long zrevrank(String key, String value) {
        return redisContext.getDefaultRedis().zrevrank(key, value);
    }

    @Override
    public Double zscore(String key, String member) {
        return redisContext.getDefaultRedis().zscore(key, member);
    }

    @Override
    public Long zrevrank(Class<?> clazz, String key, String value) {

        return redisContext.getBy(clazz).zrevrank(key, value);
    }

    @Override
    public Set<String> zrevrange(String key, long start, long end) {
        return redisContext.getDefaultRedis().zrevrange(key, start, end);
    }

    @Override
    public Set<String> zrange(final String key, final long start, final long end) {
        return redisContext.getDefaultRedis().zrange(key, start, end);
    }

    @Override
    public Set<Tuple> zrevrangeByTuple(String key, long start, long end) {
        return redisContext.getDefaultRedis().zrevrangeByTuple(key, start, end);
    }

    @Override
    public Set<String> zrevrange(Class<?> clazz, String key, long start,
                                 long end) {

        return redisContext.getBy(clazz).zrevrange(key, start, end);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {

        return redisContext.getDefaultRedis().zrevrangeWithScores(key, start,
                end);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final String key,
                                              final double min, final double max, final int offset,
                                              final int count) {
        return redisContext.getDefaultRedis().zrangeByScoreWithScores(key, min,
                max, offset, count);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final String key,
                                                 final double max, final double min, final int offset,
                                                 final int count) {

        return redisContext.getDefaultRedis().zrevrangeByScoreWithScores(key,
                max, min, offset, count);
    }

    @Override
    public String hmset(String key, Map value) {
        return redisContext.getDefaultRedis().hmset(key, value);
    }

    @Override
    public String hmset(Class<?> clazz, String key, Map value) {
        return redisContext.getBy(clazz).hmset(key, value);
    }

    @Override
    public String get(String key) {
        return redisContext.getDefaultRedis().get(key);
    }

    @Override
    public String get(Class<?> clazz, String key) {

        return redisContext.getBy(clazz).get(key);
    }

    @Override
    public List<String> hmget(String key, String... fields) {
        return redisContext.getDefaultRedis().hmget(key, fields);
    }

    @Override
    public List<String> hmget(Class<?> clazz, String key, String... fields) {

        return redisContext.getBy(clazz).hmget(key, fields);

    }

    @Override
    public Map<String, String> hgetAll(String key) {
        return redisContext.getDefaultRedis().hgetAll(key);
    }

    @Override
    public Map<String, String> hgetAll(Class<?> clazz, String key) {

        return redisContext.getBy(clazz).hgetAll(key);
    }

    @Override
    public Long zrem(final String key, final String... members) {
        return redisContext.getDefaultRedis().zrem(key, members);
    }

    @Override
    public Long zrem(Class<?> clazz, final String key, final String... members) {

        return redisContext.getBy(clazz).zrem(key, members);

    }

    @Override
    public long del(String... keys) {
        return redisContext.getDefaultRedis().del(keys);
    }

    @Override
    public long del(Class<?> clazz, String... keys) {

        return redisContext.getBy(clazz).del(keys);

    }

    @Override
    public long incr(String key) {
        return redisContext.getDefaultRedis().incr(key);
    }

    @Override
    public long incr(Class<?> clazz, String key) {

        return redisContext.getBy(clazz).incr(key);
    }

    @Override
    public Long hincrBy(final String key, final String field, final long value) {
        return redisContext.getDefaultRedis().hincrBy(key, field, value);
    }

    @Override
    public Long hincrBy(Class<?> clazz, final String key, final String field,
                        final long value) {

        return redisContext.getBy(clazz).hincrBy(key, field, value);

    }

    @Override
    public String hget(final String key, final String field) {
        return redisContext.getDefaultRedis().hget(key, field);
    }

    @Override
    public String hget(Class<?> clazz, final String key, final String field) {

        return redisContext.getBy(clazz).hget(key, field);
    }

    @Override
    public Long hset(final String key, final String field, final String value) {
        return redisContext.getDefaultRedis().hset(key, field, value);
    }

    @Override
    public Long hset(Class<?> clazz, final String key, final String field,
                     final String value) {

        return redisContext.getBy(clazz).hset(key, field, value);
    }

    @Override
    public Long hdel(final String key, final String... fields) {
        return redisContext.getDefaultRedis().hdel(key, fields);
    }

    @Override
    public Long hdel(Class<?> clazz, final String key, final String... fields) {

        return redisContext.getBy(clazz).hdel(key, fields);
    }

    @Override
    public String setex(final String key, final int seconds, final String value) {
        return redisContext.getDefaultRedis().setex(key, seconds, value);
    }

    @Override
    public String setex(Class<?> clazz, final String key, final int seconds,
                        final String value) {
        return redisContext.getBy(clazz).setex(key, seconds, value);
    }

    @Override
    public int expire(final String key, final int seconds) {
        return redisContext.getDefaultRedis().expire(key, seconds);
    }

    @Override
    public int expire(Class<?> clazz, final String key, final int seconds) {

        return redisContext.getBy(clazz).expire(key, seconds);
    }

    @Override
    public List<String> mget(final String... keys) {
        return redisContext.getDefaultRedis().mget(keys);
    }

    @Override
    public List<String> mget(Class<?> clazz, final String... keys) {

        return redisContext.getBy(clazz).mget(keys);
    }

    @Override
    public Boolean exists(final String key) {
        return redisContext.getDefaultRedis().exists(key);
    }

    @Override
    public Boolean exists(Class<?> clazz, final String key) {

        return redisContext.getBy(clazz).exists(key);
    }

    @Override
    public Long sadd(String key, final String... members) {
        return redisContext.getDefaultRedis().sadd(key, members);
    }

    @Override
    public Long sadd(Class<?> clazz, String key, final String... members) {

        return redisContext.getBy(clazz).sadd(key, members);
    }

    public List<String> srandmember(String key, int count) {
        return redisContext.getDefaultRedis().srandmember(key, count);
    }

    public List<String> srandmember(Class<?> clazz, String key, int count) {

        return redisContext.getBy(clazz).srandmember(key, count);

    }

    public Long srem(final String key, final String... members) {
        return redisContext.getDefaultRedis().srem(key, members);
    }

    public Long srem(Class<?> clazz, final String key, final String... members) {

        return redisContext.getBy(clazz).srem(key, members);
    }

    @Override
    public Set<String> smembers(final String key) {
        return redisContext.getDefaultRedis().sMembers(key);
    }

    /**
     * Use this method to generate the serial id base on Redis's HINCRBY command
     *
     * @param name the item name
     * @return long the generated serial id.
     */
    @Override
    public long sid(String name) {

        return redisContext.getConfigRedis().hincrBy(RedisNameSpace.QUIEN_SERA_SID,
                name, 1);
    }

    /**
     * 生成对应key下面的序列号
     *
     * @param key
     * @return
     */
    @Override
    public long generateNO(String key) {

        return redisContext.getConfigRedis().hincrBy(RedisNameSpace.QUIEN_SERA_NO,
                key, 1);

    }

    @Override
    public long peekSid(String name) {
        String sid = redisContext.getConfigRedis().hget(RedisNameSpace.QUIEN_SERA_SID,
                name);
        return StringUtils.isNotEmpty(sid) ? Long.valueOf(sid) : 0;
    }

    @Override
    public void putInCache(Class<?> clazz, String key, String value) {
        putInCache(clazz, key, value, true);
    }

    @Override
    public void putInCache(Class<?> clazz, String key, String value,
                           boolean expire) {

        if (StringUtils.isEmpty(key)) {
            return;
        }

        String fullkey = RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                + clazz.getName() + RedisNameSpace.SEPARATOR + key;

        if (expire) {
            redisContext.getBy(clazz).setex(fullkey, EXPIRE_IN_SECONDS, value);
        } else {
            redisContext.getBy(clazz).set(fullkey, value);
        }
    }

    @Override
    public void putInCache(Class<?> clazz, Map<String, String> values) {
        putInCache(clazz, values, true);
    }

    @Override
    public void putInCache(Class<?> clazz, Map<String, String> values,
                           boolean expire) {

        redisContext.getBy(clazz).setMultiValues(
                RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                        + clazz.getName(), values,
                expire ? EXPIRE_IN_SECONDS : -1);

    }

    @Override
    public void expireCache(Class<?> clazz, String key) {
        expireCache(clazz, key, EXPIRE_IN_SECONDS);
    }

    @Override
    public void expireCache(Class<?> clazz, String key, int seconds) {
        if (StringUtils.isEmpty(key)) {
            return;
        }

        String fullkey = RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                + clazz.getName() + RedisNameSpace.SEPARATOR + key;

        redisContext.getBy(clazz).expire(fullkey, seconds);
    }

    @Override
    public String getFromCache(Class<?> clazz, String key) {
        return redisContext.getBy(clazz).get(
                RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                        + clazz.getName() + RedisNameSpace.SEPARATOR + key);
    }

    @Override
    public Map<String, String> getFromCache(Class<?> clazz, List<String> keys) {
        return redisContext.getBy(clazz).getMultiValues(
                RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                        + clazz.getName(), keys);
    }

    @Override
    public void removeFromCache(Class<?> clazz, String key) {
        redisContext.getBy(clazz).del(
                RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                        + clazz.getName() + RedisNameSpace.SEPARATOR + key);
    }

    @Override
    public void removeFromCache(Class<?> clazz, List<String> keys) {
        List<String> keysToRemove = new ArrayList<String>();
        for (String key : keys) {
            keysToRemove.add(RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                    + clazz.getName() + RedisNameSpace.SEPARATOR + key);
        }
        if (!keysToRemove.isEmpty()) {
            redisContext.getBy(clazz)
                    .del(keysToRemove.toArray(new String[]{}));
        }
    }

    @Override
    public <T> void putInHashNX(Class<T> clazz, String key, T obj) {
        putInHashNX(clazz, key, obj, true);
    }

    @Override
    public <T> void putInHashNX(Class<T> clazz, String key, T obj,
                                boolean expire) {
        String fullKey = RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                + clazz.getName() + RedisNameSpace.SEPARATOR + key;

        try {
            redisContext.getBy(clazz).putInHashByLua(fullKey,
                    BaseUtils.describeAsString(obj), expire, true);
        } catch (Exception ex) {
            logger.error("putInHashNX exception", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public <T> void putInHash(final Class<T> clazz, final String key,
                              final T obj) {

        putInHash(clazz, key, obj, true);
    }

    @Override
    public <T> void putInHash(final Class<T> clazz, final String key,
                              final T obj, final boolean expire) {

        if (clazz == null || StringUtils.isEmpty(key) || obj == null) {
            return;
        }
        String fullKey = RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                + clazz.getName() + RedisNameSpace.SEPARATOR + key;
        try {
            redisContext.getBy(clazz).putInHashByLua(fullKey,
                    BaseUtils.describeAsString(obj), expire, true);
        } catch (Exception ex) {
            logger.error("putInHash exception", ex);
            throw new RuntimeException(ex);
        }

    }

    @Override
    public <T> void putInHash(final Class<T> clazz,
                              final Map<String, T> keyObjMap) {

        putInHash(clazz, keyObjMap, true);
    }

    @Override
    public <T> void putInHash(final Class<T> clazz,
                              final Map<String, T> keyObjMap, final boolean expire) {

        if (clazz == null || keyObjMap == null || keyObjMap.isEmpty()) {
            return;
        }

        readwrite_doWithPipeLine(clazz, new Redis.PipelineCallable<Boolean>() {

            @Override
            public Boolean call(Pipeline p) {
                try {
                    for (String key : keyObjMap.keySet()) {
                        String fullKey = RedisNameSpace.QUIEN_SERA_TEMP
                                + RedisNameSpace.SEPARATOR + clazz.getName()
                                + RedisNameSpace.SEPARATOR + key;
                        T obj = keyObjMap.get(key);
                        if (obj != null) {
                            p.hmset(fullKey, BaseUtils.describeAsString(obj));
                            p.pexpire(fullKey, EXPIRE_IN_SECONDS * 1000L);
                        }
                    }

                    p.sync();

                    return true;

                } catch (Exception ex) {
                    logger.error("putInHash failed: class=" + clazz.getName(),
                            ex);
                }
                return false;
            }
        });

    }

    @Override
    public <T> T getFromHash(Class<T> clazz, String key) {
        if (clazz == null || StringUtils.isEmpty(key)) {
            return null;
        }
        String fullKey = RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                + clazz.getName() + RedisNameSpace.SEPARATOR + key;

        Map map = redisContext.getBy(clazz).hgetAll(fullKey);

        if (map == null || map.isEmpty()) {
            return null;
        }

        T obj = null;

        try {
            obj = clazz.newInstance();
            BaseUtils.populate(obj, map);
        } catch (Exception ex) {
            logger.error("getFromHash failed: class=" + clazz.getName()
                    + ", key=" + key);
        }
        return obj;
    }

    @Override
    public <T> Map<String, T> getFromHash(final Class<T> clazz,
                                          final List<String> keys) {

        final Map<String, T> resultMap = new HashMap<>();

        if (clazz == null || keys == null || keys.isEmpty()) {
            return resultMap;
        }

        readonly_doWithPipeLine(clazz, new Redis.PipelineCallable<Boolean>() {

            @Override
            public Boolean call(Pipeline p) {

                Map<String, Response<Map<String, String>>> responseMap = new HashMap<>();

                for (String key : keys) {
                    String fullKey = RedisNameSpace.QUIEN_SERA_TEMP
                            + RedisNameSpace.SEPARATOR + clazz.getName()
                            + RedisNameSpace.SEPARATOR + key;

                    Response<Map<String, String>> response = p.hgetAll(fullKey);
                    responseMap.put(key, response);
                }

                p.sync();

                try {
                    for (String key : keys) {

                        Response<Map<String, String>> response = responseMap
                                .get(key);

                        if (response != null) {

                            Map map = response.get();

                            if (map != null && !map.isEmpty()) {
                                T obj = clazz.newInstance();
                                BaseUtils.populate(obj, map);
                                resultMap.put(key, obj);
                            }
                        }
                    }
                } catch (Exception ex) {
                    logger.error(
                            "getFromHash failed: class=" + clazz.getName(), ex);
                }

                return true;
            }

        });

        return resultMap;
    }

    @Override
    public void setFieldValueInHash(final Class<?> clazz, final String key,
                                    final String field, final String value) {

        if (clazz == null || StringUtils.isEmpty(key)
                || StringUtils.isEmpty(field)) {
            return;
        }

        Map<String, String> map = new HashMap<>();
        map.put(field, value);

        setFieldValueInHash(clazz, key, map);
    }

    @Override
    public void setFieldValueInHash(final Class<?> clazz, final String key,
                                    final Map<String, String> map) {

        final String fullKey = RedisNameSpace.QUIEN_SERA_TEMP
                + RedisNameSpace.SEPARATOR + clazz.getName()
                + RedisNameSpace.SEPARATOR + key;

        redisContext.getBy(clazz).setHashValuesByLua(fullKey, map);
    }

    @Override
    public void increaseFieldValueInHash(Class<?> clazz, String key,
                                         String field, int n) {

        increaseFieldValueInHash(clazz, key, field, n, true);
    }

    @Override
    public void increaseFieldValueInHash(Class<?> clazz, String key,
                                         String field, int n, boolean expire) {

        String fullKey = RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                + clazz.getName() + RedisNameSpace.SEPARATOR + key;

        Map<String, Integer> map = new HashMap<>(1);
        map.put(field, n);

        redisContext.getBy(clazz).increaseHashFieldValueByLua(fullKey, map,
                expire);
    }

    @Override
    public void increaseFieldValueInHash(Class<?> clazz, final String key,
                                         final Map<String, Integer> values) {

        final String namespace = RedisNameSpace.QUIEN_SERA_TEMP
                + RedisNameSpace.SEPARATOR + clazz.getName()
                + RedisNameSpace.SEPARATOR + key;

        redisContext.getBy(clazz)
                .increaseHashFieldValueByLua(namespace, values);
    }

    @Override
    public void removeFromHash(Class<?> clazz, String key) {

        if (clazz == null || key == null) {
            return;
        }

        String fullKey = RedisNameSpace.QUIEN_SERA_TEMP + RedisNameSpace.SEPARATOR
                + clazz.getName() + RedisNameSpace.SEPARATOR + key;

        redisContext.getBy(clazz).del(fullKey);
    }

    @Override
    public void removeFromHash(final Class<?> clazz, final List<String> keys) {

        if (clazz == null || keys == null || keys.isEmpty()) {
            return;
        }

        if (keys.size() == 1) {
            removeFromHash(clazz, keys.get(0));
        }

        readwrite_doWithPipeLine(clazz, new Redis.PipelineCallable<Boolean>() {

            @Override
            public Boolean call(Pipeline p) {
                for (String key : keys) {
                    String fullKey = RedisNameSpace.QUIEN_SERA_TEMP
                            + RedisNameSpace.SEPARATOR + clazz.getName()
                            + RedisNameSpace.SEPARATOR + key;

                    p.del(fullKey);
                }

                p.sync();

                return true;
            }
        });
    }

    @Override
    public <T> T doWithLock(String lockName, Callable<T> callback) {

        if (StringUtils.isEmpty(lockName) || callback == null) {
            throw new IllegalArgumentException("lockName=" + lockName
                    + ", callback=" + callback);
        }

        String uuid = "";

        try {
            uuid = lock(lockName);
            return callback.call();
        } catch (Throwable ex) {
            if (ex != null && (ex instanceof BaseException)) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException("!! failed to do with lock \""
                    + lockName + "\"", ex);
        } finally {
            unlock(lockName, uuid);
        }
    }

    protected <T> T readwrite_doWithPipeLine(Class clazz,
                                             Redis.PipelineCallable<T> callback) {

        return redisContext.getBy(clazz).readwrite_doWithPipeLine(callback);

    }

    protected <T> T readwrite_doWithPipeLine(String policy,
                                             Redis.PipelineCallable<T> callback) {

        return redisContext.getByPolicy(policy).readwrite_doWithPipeLine(callback);

    }

    protected <T> T readwrite_doWithPipeLine(Redis.PipelineCallable<T> callback) {

        return redisContext.getDefaultRedis()
                .readwrite_doWithPipeLine(callback);

    }

    @Deprecated
    protected <T> T readonly_doWithPipeLine(Class clazz,
                                            Redis.PipelineCallable<T> callback) {
        return redisContext.getBy(clazz).readwrite_doWithPipeLine(callback);
    }

    @Deprecated
    protected <T> T readonly_doWithPipeLine(Redis.PipelineCallable<T> callback) {
        return redisContext.getDefaultRedis().readwrite_doWithPipeLine(callback);
    }

    protected String lock(String lockName) {
        return redisContext.getDefaultRedis().lock(lockName);
    }

    protected String lock(Class<?> clazz, String lockName) {
        return redisContext.getBy(clazz).lock(lockName);
    }

    protected void unlock(String lockName, String uuid) {
        redisContext.getDefaultRedis().unlock(lockName, uuid);
    }

    protected void unlock(Class<?> clazz, String lockName, String uuid) {
        redisContext.getBy(clazz).unlock(lockName, uuid);
    }

    protected Object evalsha(String sha1, List<String> keys, List<String> args) {
        return redisContext.getDefaultRedis().evalsha(sha1, keys, args);
    }

    protected Object evalsha(Class<?> clazz, String sha1, List<String> keys,
                             List<String> args) {
        return redisContext.getBy(clazz).evalsha(sha1, keys, args);
    }

    @Override
    public void lpush(String key, String... values) {
        redisContext.getDefaultRedis().lpush(key, values);
    }

    @Override
    public void lpush(Class<?> clazz, String key, String... values) {
        redisContext.getBy(clazz).lpush(key, values);
    }

    @Override
    public void rpush(String key, String... values) {
        redisContext.getDefaultRedis().rpush(key, values);
    }

    @Override
    public void lrem(String key, String values) {
        redisContext.getDefaultRedis().lrem(key, values);
    }

    @Override
    public void lrem(Class<?> clazz, String key, String values) {
        redisContext.getBy(clazz).lrem(key, values);

    }

    @Override
    public List<String> lrange(String key) {
        return redisContext.getDefaultRedis().lrange(key);
    }

    @Override
    public List<String> lrange(String key, Long start, Long end) {
        return redisContext.getDefaultRedis().lrange(key, start, end);
    }

    @Override
    public List<String> lrange(Class<?> clazz, String key) {
        return redisContext.getBy(clazz).lrange(key);
    }

}