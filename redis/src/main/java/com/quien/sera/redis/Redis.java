/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.redis;

import com.quien.sera.base.Environment;
import com.quien.sera.common.constant.Constants;
import com.quien.sera.common.util.BaseUtils;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Redis {

    private static final Logger logger = LoggerFactory
            .getLogger(Redis.class);

    private JedisPool master;

    private JedisPool[] slaves;

    private int poolsAmount = 0;

    private int roundNo = 0;

    private static final Set<String> READ_ONLY_CMDS = new HashSet<String>();

    private final Map<String, String> SCRIPT_SHA = new ConcurrentHashMap<String, String>();

    private static final int EXPIRE_IN_SECONDS = 30 * 60; // 30 mins

    private static final String OK = "OK";

    private final RedisConfig redisConfig;
    
    static {

        READ_ONLY_CMDS.add("get");
        READ_ONLY_CMDS.add("hget");
        READ_ONLY_CMDS.add("hmget");
        READ_ONLY_CMDS.add("hgetAll");
        READ_ONLY_CMDS.add("mget");
        READ_ONLY_CMDS.add("zrange");
        READ_ONLY_CMDS.add("zcard");
        READ_ONLY_CMDS.add("lrange");
        READ_ONLY_CMDS.add("srandmember");

    }

    public Redis(RedisConfig redisServerConfig) {

        this.redisConfig = redisServerConfig;
        
        if (null != redisServerConfig) {
            JedisPoolConfig config = new JedisPoolConfig();
            BaseUtils.copyProperties(config, redisServerConfig);

            String pwd = redisServerConfig.getPassword();

            if (StringUtils.isEmpty(pwd)) {
                this.master = new JedisPool(config, redisServerConfig.getIp(),
                        redisServerConfig.getPort(), 30000);
            } else {
                this.master = new JedisPool(config, redisServerConfig.getIp(),
                        redisServerConfig.getPort(), 30000, pwd);
            }

            RedisConfig[] slavesConfigs = redisServerConfig.getSlaves();

            if (null != slavesConfigs) {

                this.slaves = new JedisPool[slavesConfigs.length];
                for (int i = 0; i < slavesConfigs.length; i++) {

                    RedisConfig slaveServerConfig = slavesConfigs[i];
                    JedisPoolConfig slaveCfg = new JedisPoolConfig();
                    BaseUtils.copyProperties(slaveCfg, slaveServerConfig);

                    pwd = slaveServerConfig.getPassword();

                    JedisPool slaveServer = null;
                    if (StringUtils.isNotEmpty(pwd)) {
                        slaveServer = new JedisPool(slaveCfg, slaveServerConfig.getIp(),
                                slaveServerConfig.getPort(), 30000, pwd);
                    } else {
                        slaveServer = new JedisPool(slaveCfg, slaveServerConfig.getIp(),
                                slaveServerConfig.getPort(), 30000);
                    }
                    slaves[i] = slaveServer;
                }
            }

            this.init();
        }
    }

    private void init() {
        this.poolsAmount = (this.master != null ? 1 : 0)
                + (this.slaves != null && this.slaves.length > 0 ? this.slaves.length : 0);
        this.roundNo = 0;
    }

    public JedisPool getJedisPoolByCmd(String cmd) {

        return READ_ONLY_CMDS.contains(cmd) ? roundAccess() : this.master;
    }

    private JedisPool roundAccess() {

        if (poolsAmount < 2) { // only has one server
            return this.master;
        }

        int n = this.roundNo++ % this.poolsAmount;
        if (n == 0) {
            this.roundNo = 1;
        }
        return n < 1 ? this.master : this.slaves[n - 1];

    }

    public <T> T readwrite_doWithPipeLine(PipelineCallable<T> callback) {
        return _doWithPipeLine(false, callback);
    }

    public <T> T readonly_doWithPipeLine(PipelineCallable<T> callback) {
        return _doWithPipeLine(true, callback);
    }

    private <T> T _doWithPipeLine(boolean readOnly,
            PipelineCallable<T> callback) {
        JedisPool pool = (readOnly ? roundAccess() : master);

        Jedis jedis = null;

        Pipeline p = null;

        try {

            jedis = pool.getResource();

            p = jedis.pipelined();

            return callback.call(p);

        } catch (JedisConnectionException ex) {
            if (jedis != null) {
                pool.returnBrokenResource(jedis);
                jedis = null;

            }
            logger.error("JedisConnectionException occurred!!", ex);
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            logger.error("Jedis method invoke exception occurred!!", ex);
            throw new RuntimeException(ex);
        } finally {
            if (jedis != null) {
                pool.returnResource(jedis);
            }
        }
    }

    public Object invoke(String cmd, Object... args) {

        if (cmd != null) {
            cmd = cmd.trim();
        }

        if (StringUtils.isEmpty(cmd)) {
            throw new IllegalArgumentException("arg cmd is empty!!");
        }

        Object result = null;

        JedisPool pool = getJedisPoolByCmd(cmd);

        Jedis jedis = null;
        
        try {
            jedis = pool.getResource();
            if ("mget".equals(cmd)) {
                result = jedis.mget((String[]) args);
            } else {
                result = MethodUtils.invokeMethod(jedis, cmd, args);
            }
        } catch (JedisConnectionException ex) {
            if (jedis != null) {
                pool.returnBrokenResource(jedis);
                jedis = null;

            }
            logger.error("JedisConnectionException occurred!!", ex);
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            logger.error("Jedis method invoke exception occurred!!", ex);
            throw new RuntimeException(ex);
        } finally {
            if (jedis != null) {

                pool.returnResource(jedis);
            }
        }

        return result;
    }

    public String scriptLoad(String scriptName) {
        String sha = SCRIPT_SHA.get(scriptName);
        if (StringUtils.isEmpty(sha)) {
            synchronized (this) {
                sha = SCRIPT_SHA.get(scriptName);
                if (StringUtils.isEmpty(sha)) {
                    try {
                        String script = Environment.loadContent("redis/"
                                + scriptName + ".lua");
                        if (StringUtils.isEmpty(script)) {
                            throw new IllegalArgumentException(
                                    "The content of script \"" + scriptName
                                    + "\" is empty.");
                        }
                        sha = (String) this.invoke("scriptLoad", script);
                        SCRIPT_SHA.put(scriptName, sha);
                    } catch (Exception ex) {
                        logger.error(
                                "Failed to load LUA script: " + scriptName, ex);
                    }
                }
            }
        }
        return sha;
    }

    public static interface PipelineCallable<T> {

        T call(Pipeline p);
    }

    public String set(String key, String value) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (String) this.invoke("set", key, value);
    }

    public Boolean sisMember(String key, String value) {

        if (StringUtils.isEmpty(key)) {
            return false;
        }
        return (Boolean) this.invoke("sismember", key, value);

    }

    public Set<String> sMembers(String key) {

        if (StringUtils.isEmpty(key)) {
            return null;
        }

        return (Set<String>) invoke("smembers", key);
    }

    public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max,
                                        final int offset, final int count) {
        if ( StringUtils.isEmpty( key ) ) {
            return null;
        }

        return (Set<Tuple>)invoke("zrangeByScoreWithScores", key , min , max , offset , count);
    }

    public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max,
          final double min, final int offset, final int count) {
        
        if( StringUtils.isEmpty( key ) ) {
            return null;
        }
        
        return (Set<Tuple>)invoke("zrevrangeByScoreWithScores", key, max, min, offset, count);
    }
    
    public String setex(final String key, final int seconds, final String value) {

        if ( StringUtils.isEmpty( key ) ) {
            return null;
        }

        return (String) invoke("setex", key, seconds, value);
    }
    
    public String psetex(final String key, final int millseconds, final String value) {

        if ( StringUtils.isEmpty( key ) ) {
            return null;
        }

        return (String) invoke("psetex", key, millseconds, value);
    }

    public Boolean exists(final String key) {

        return (Boolean) invoke("exists", key);
    }

    public String get(String key) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (String) invoke("get", key);
    }

    public long del(String... keys) {

        return (Long) invoke("del", (Object) keys);

    }

    public Long zrem(final String key, final String... members) {

        if ( StringUtils.isEmpty(key) ) {
            return null;
        }

        return (Long) invoke("zrem", key, members);
    }

    public Long zadd(String key, double score, String value) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (Long) invoke("zadd", key, score, value);
    }

    public Long zadd(final String key, final Map<String, Double> scoreValue) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (Long) invoke("zadd", key, scoreValue);
    }

    public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (Set<Tuple>) invoke("zrevrangeWithScores", key, start, end);
    }

    public Set<Tuple> zrevrangeByTuple(String key, long start, long end) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (Set<Tuple>) invoke("zrevrange", key, start, end);
    }

    public Set<String> zrevrange(String key, long start, long end) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (Set<String>) invoke("zrevrange", key, start, end);
    }

    public Set<String> zrange(String key, long start, long end) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (Set<String>) invoke("zrange", key, start, end);
    }

    public Double zincrby(String key, double value, String feild) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (Double) invoke("zincrby", key, value, feild);
    }

    public Double zscore(String key, String feild) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (Double) invoke("zscore", key, feild);
    }

    public Long zrevrank(String key, String value) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }
        return (Long) invoke("zrevrank", key, value);
    }

    public long incr(String key) {

        if (StringUtils.isEmpty(key)) {
            return 0;
        }
        return (Long) invoke("incr", key);
    }

    public long decr(final String key) {
        if (StringUtils.isEmpty(key)) {
            return 0;
        }
        return (Long) invoke("decr", key);
    }
    
    public String hmset(String key, Map value) {
        if (StringUtils.isEmpty(key)) {
            return null;
        }

        if (value != null && value.keySet() != null
                && value.keySet().size() > 0) {

            return (String) invoke("hmset", key, value);
        } else {
            return null;
        }
    }

    public List<String> hmget(String key, String... fields) {

        if (StringUtils.isEmpty(key)) {
            return null;
        }

        return (List<String>) invoke("hmget", key, fields);
    }

    public int expire(final String key, final int seconds) {

        return ((Long) invoke("expire", key, seconds)).intValue();
    }

    public Map<String, String> hgetAll(String key) {

        if (StringUtils.isEmpty(key)) {
            return null;
        }

        return (Map<String, String>) invoke("hgetAll", key);
    }

    public Long hincrBy(final String key, final String field, final long value) {

        if (StringUtils.isEmpty(key)) {

            return null;
        }

        return (Long) invoke("hincrBy", key, field, value);

    }

    public String hget(final String key, final String field) {

        if (StringUtils.isEmpty(key)) {

            return null;
        }

        return (String) invoke("hget", key, field);
    }

    public Long hset(final String key, final String field, final String value) {

        if (StringUtils.isEmpty(key)) {
            return null;
        }

        return (Long) invoke("hset", key, field, value);
    }

    public Long sadd(String key, final String... members) {

        return (Long) invoke("sadd", key, (Object) members);
    }

    public Long hdel(final String key, final String... fields) {

        return (Long) invoke("hdel", key, (Object) fields);
    }

    public List<String> mget(final String... keys) {

        return (List<String>) invoke("mget", (Object[]) keys);

    }

    public List<String> srandmember(String key, int count) {

        return (List<String>) invoke("srandmember", key, count);
    }

    public Long srem(final String key, final String... members) {

        return (Long) invoke("srem", key, (Object) members);
    }

    public List<String> lrange(String key) {
        return (List<String>) invoke("lrange", key, 0L, -1L);
    }

    public List<String> lrange(String key,Long start,Long end) {
        return (List<String>) invoke("lrange", key, start, end);
    }

    
    public Object evalsha(String sha1, List<String> keys, List<String> args) {

        return invoke("evalsha", sha1, keys, args);
    }

    public void lpush(String key, String... values) {

        invoke("lpush", key, values);
    }

    public void rpush(String key, String... values) {

        invoke("rpush", key, values);
    }

    public void lrem(String key, String values) {

        invoke("lrem", key,0l, values);

    }

    public void lset( String key, long index, String value ) {
        invoke( "lset", key, index, value );
    }
    
    public void setHashValuesByLua(String luaName, String key,
            Map<String, String> map) {
        String uuid = BaseUtils.uuid();
        String sha;
        sha = scriptLoad(luaName);

        List<String> keys = new ArrayList<String>();
        keys.add(key);
        keys.add(uuid);
        List<String> args = new ArrayList<String>();
        args.add(String.valueOf(map.size() + 2));
        args.add(uuid);
        for (String kk : map.keySet()) {
            keys.add(kk);
            String value = map.get(kk);
            if (value == null) {
                value = uuid;
            }
            args.add(value);
        }
        evalsha(sha, keys, args);
    }

    public Long zcard(String key) {

        return (Long) invoke("zcard", key);
    }

    public String lock(String lockName) {
        String uuid = BaseUtils.uuid();
        int waitPeriod = 10000; // 10 seconds
        int sleepPeriod = 100;
        while (waitPeriod >= 0) {

            String value = (String) this.invoke("set", lockName, uuid, "NX", "EX",
                    10);
            if (OK.equalsIgnoreCase(value)) {
                return uuid;
            }
            try {
                Thread.sleep(sleepPeriod);
            } catch (Exception ex) {

            }

            waitPeriod -= sleepPeriod;
        }

        throw new RuntimeException("!! failed to get lock with \"" + lockName
                + "\"");
    }

    public void unlock(String lockName, String uuid) {
        if (StringUtils.isEmpty(lockName) || StringUtils.isEmpty(uuid)) {
            return;
        }

        String sha = this.scriptLoad("unlock");
        List<String> keys = new ArrayList<String>();
        keys.add(lockName);
        List<String> args = new ArrayList<String>();
        args.add(uuid);
        evalsha(sha, keys, args);
    }

    public void increaseHashFieldValueByLua(String key, Map<String, Integer> map) {
        increaseHashFieldValueByLua(key, map, true);
    }

    public void increaseHashFieldValueByLua(String key, Map<String, Integer> map, boolean expire) {
        String sha = scriptLoad(expire ? "increaseFieldHashValuesInHash" : "increaseFieldHashValuesInHashNonExpire");
        List<String> keys = new ArrayList<String>();
        keys.add(key);
        List<String> args = new ArrayList<String>();
        args.add(String.valueOf(map.size() + 1));
        for (String kk : map.keySet()) {

            Integer value = map.get(kk);
            if (value == null) {
                continue;
            }
            keys.add(kk);
            args.add( String.valueOf( value ) );
        }
        evalsha(sha, keys, args);
    }

    public void putInHashByLua(String key, Map<String, String> map,
            boolean expire, boolean isExist) {
        String sha = scriptLoad("putInHash");
        List<String> keys = new ArrayList<String>();
        keys.add(key);
        keys.add(String.valueOf(EXPIRE_IN_SECONDS * 1000L));

        List<String> args = new ArrayList<String>();

        if (expire) {
            args.add("1");
        } else {
            args.add("0");
        }

        args.add(String.valueOf(map.size() + 3));
        
        if (isExist) {
            args.add("1");
            keys.add("1");
        } else {
            args.add("0");
            keys.add("0");
        }

        for (String kk : map.keySet()) {

            String value = map.get(kk);
            if (value == null) {
                continue;
            }
            keys.add(kk);
            args.add(value);
        }

        evalsha(sha, keys, args);
    }
    
    public void setHashValuesByLua( String key, Map<String, String> map ) {
        setHashValuesByLua("setFieldValuesInHash", key, map);
    }

    public Map<String, String> getMultiValues(String nameSpacePrefix,
            List<String> keys) {

        Map<String, String> map = new HashMap<String, String>();

        if (keys == null || keys.isEmpty()) {
            return map;
        }

        List<String> redisKeys = new ArrayList<String>();
        for (String key : keys) {
            redisKeys.add(nameSpacePrefix + RedisNameSpace.SEPARATOR + key);
        }

        List<String> values = this.mget(redisKeys.toArray(new String[]{}));

        if (values != null) {
            int k = (nameSpacePrefix + RedisNameSpace.SEPARATOR).length();
            for (int i = 0; i < redisKeys.size(); i++) {
                String v = values.get(i);
                if (StringUtils.isNotEmpty(v)) {
                    map.put(redisKeys.get(i).substring(k), v);
                }
            }
        }

        return map;
    }

    public void setMultiValues(final String nameSpacePrefix,
            final Map<String, String> keyValuesMap, final int expireInSeconds) {

        if (keyValuesMap == null || keyValuesMap.isEmpty()) {
            return;
        }

        if (keyValuesMap.size() == 1) {
            for (String key : keyValuesMap.keySet()) {
                if (expireInSeconds > 0) {
                    this.setex(
                            nameSpacePrefix + RedisNameSpace.SEPARATOR + key,
                            expireInSeconds, keyValuesMap.get(key));
                } else {
                    this.set(nameSpacePrefix + RedisNameSpace.SEPARATOR + key,
                            keyValuesMap.get(key));
                }
            }
            return;
        }

        readwrite_doWithPipeLine(new PipelineCallable<Boolean>() {

            public Boolean call(Pipeline p) {
                for (String key : keyValuesMap.keySet()) {
                    String value = keyValuesMap.get(key);
                    if (expireInSeconds > 0) {
                        p.setex(nameSpacePrefix + RedisNameSpace.SEPARATOR
                                + key, expireInSeconds,
                                value == null ? Constants.NULL : value);
                    } else {
                        p.set(nameSpacePrefix + RedisNameSpace.SEPARATOR + key,
                                value == null ? Constants.NULL : value);
                    }
                }

                p.sync();

                return true;
            }
        });
    }
    
    
}