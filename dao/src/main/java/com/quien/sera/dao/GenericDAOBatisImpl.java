/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.dao;

import com.quien.sera.common.constant.Constants;
import com.quien.sera.common.entity.IndexEntity;
import com.quien.sera.common.entity.IndexField;
import com.quien.sera.common.entity.TimestampEntity;
import com.quien.sera.common.util.BaseUtils;
import com.quien.sera.common.util.DatetimeUtils;
import com.quien.sera.dao.exception.TryToUpdateException;
import com.quien.sera.dao.util.ReadDatetimePolicy;
import com.quien.sera.dao.util.SidGenerator;
import com.quien.sera.dao.util.SplitTablePolicy;
import com.quien.sera.redis.RedisHelper;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.PostConstruct;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Callable;

import com.quien.sera.common.entity.BaseEntity;

public abstract class GenericDAOBatisImpl<E extends BaseEntity,
                                 T extends GenericDAOBatisImpl<E, T>>
    extends SqlSessionDaoSupport
    implements GenericDAO<E> { 
    
    private static final String NAME_SPACE_SUFFIX = "Mapper";
    
    protected static final String VAR_TABLE_NAME = "table_name";
    
    protected static final String VAR_TABLE_NAME_SEPARATOR = "_";
    
    protected static final String VAR_TABLES_WITH_SIDS = "tablesWithSids";
    
    protected static final String VAR_TABLES_WITH_FIELDS_VALUES = 
            "tablesWithFieldsValues";
    
    protected static final String VAR_FIELDS = "xfields";
    protected static final String VAR_VALUES = "xvalues";
    
    private static final String SID = "sid";
    
    protected static final int SUCCESS = 1;
    protected static final int FAIL = 0;
    
    protected static final int PAGESIZE_MAX = 100;
    
    private static final int BATCH_SIZE = 100;
    
    private final static int MAX_SEARCH_TABLES = 3;
    
    private final static int MAX_TRY_TIMES = 3;
    
    private static final Map<Class, String> 
                            NAME_SPACE_MAP = new HashMap<Class, String>();
    
    protected Class<E> entityClass;
    protected Class<T> daoImplClass;
    
    private final ObjectFactory objectFactory = new DefaultObjectFactory();
    
    @Autowired
    private SqlSessionFactory sqlSessionFactory;
    
    @Autowired
    protected SplitTablePolicy splitTablePolicy;
    
    @Autowired
    protected ReadDatetimePolicy readDatetimePolicy;
    
    @Autowired
    protected SidGenerator sidGenerator;
    
    @Autowired
    protected RedisHelper redisHelper;
    
    @SuppressWarnings( "unchecked" )
    public GenericDAOBatisImpl() {
        Type[] types = ( ( ParameterizedType )getClass()
                .getGenericSuperclass() ).getActualTypeArguments();
        this.entityClass = ( Class<E> )types[0];
        this.daoImplClass = ( Class<T> )types[1];
    }
    
    @PostConstruct
    public void init() { 
        super.setSqlSessionFactory( sqlSessionFactory );
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public List<E> selectAll() {
        List<E> entityList = new ArrayList<>();
        List<Map> list = this.getSqlSession().selectList(
                getNameSpace(GenericDAOBatisImpl.class, "selectAll"),
                newParams().put(VAR_TABLE_NAME, getTableName()));
        if (list != null) {
            for (Map map : list) {
                entityList.add(this.transMapToEntity(map));
            }
        }
        return entityList;
    }
    
    @Override
    public List<E> selectTheFirstOnePerTable() {
        
        List<String> tableNames = new ArrayList<String>();
        String tableName = getTableName();
        int no = getTableNo();
        for( int i = MAX_SEARCH_TABLES; i > 0 && no >= 0; i--, no-- ) {
            tableNames.add( tableName + ( no > 0 ? VAR_TABLE_NAME_SEPARATOR + no : "" ) );
        }
        
        Map<String, String> jsonMap = redisHelper.getFromCache( entityClass, tableNames );
        
        List<E> resultEntities = new ArrayList<E>();
        List<String> notCached = new ArrayList<String>();
        
        if( jsonMap != null && ! jsonMap.isEmpty() ) {
            for( String tname : tableNames ) {
                String json = jsonMap.get( tname );
                if( StringUtils.isNotEmpty( json ) ) {
                    resultEntities.add( BaseUtils.fromJson(json, entityClass) );
                }else {
                    notCached.add( tname );
                }
            }
        }else {
            notCached.addAll( tableNames );
        }
        
        if( ! notCached.isEmpty() ) {
            List<E> es = this.genericSelectList( "selectTheFirstOnePerTable", 
                    newParams().put( "tableNames", notCached ) );
            
            if( es != null && ! es.isEmpty() ) {
                
                resultEntities.addAll( es );
                
                Map<String, String> needToPutInCache = new HashMap<>();
                for( E e : es ) {
                    needToPutInCache.put( getTableName( e.getSid() ), 
                            BaseUtils.toJson( e ) );
                }
                redisHelper.putInCache( entityClass, needToPutInCache );
            }
        }
        
        // sort by table No. in desc order
        Collections.sort(resultEntities, new Comparator<E>() {

            @Override
            public int compare(E o1, E o2) {
                return getTableNo(o2.getSid()) - getTableNo(o1.getSid());
            }

        });
        
        return resultEntities;
    }
    
    protected <RE extends BaseEntity, R extends GenericDAO<RE>>
    List<E> selectByRelation( Class<R> relationDAOImplClass, 
            Class<RE> relationEntityClass,
            Long rsid,
            String relationFieldName,
            String targetFieldName,
            Long datetime,
            Constants.ReadDirection readDirection,
            int n,
            Long... excludeSids ) {
        
        String relationTableName = splitTablePolicy.getTableName( 
                relationDAOImplClass, 
                relationEntityClass, 
                rsid );
        
        List<E> targets = new ArrayList<>();
        
        Long tt = readDatetimePolicy.getReadTimestamp( 
                entityClass, 
                datetime, 
                readDirection );
        
        if( tt == null ) {
            readDirection = Constants.ReadDirection.DOWN;
            datetime = System.currentTimeMillis();
        }
        
        Map<String, Object> params = newParams();
        
        params.put( "relationTable", relationTableName );
        params.put( "relationFieldName", relationFieldName );
        params.put( "targetFieldName", targetFieldName );
        params.put( "datetime", tt );
        params.put( "excludeSids", excludeSids );
        
        boolean isUP = Constants.ReadDirection.UP.equals( readDirection );
        
        if( isUP ) {
            params.put( "order", "asc" );
            params.put( "op", ">=" );
        }else { // DOWN
            params.put( "order", "desc" );
            params.put( "op", "<=" );
        }
        
        List<E> theFirstOnePerTable = selectTheFirstOnePerTable();
        
        if( isUP ) {
            // if UP, then just search in the lastest 2 split tables
            int size = theFirstOnePerTable.size();
            if( size > 0 ) {
                TimestampEntity theFirstOne = (TimestampEntity)theFirstOnePerTable.get( 0 );
                if( theFirstOne.getCreatedTimestamp() < datetime ) {
                    // select in the current updated most table
                    selectRelatedTargets( targets, (E)theFirstOne, params, n );
                    
                }else if( size > 1 ) {
                    theFirstOne = (TimestampEntity)theFirstOnePerTable.get( 1 );
                    selectRelatedTargets( targets, (E)theFirstOne, params, n );
                    if( targets.size() < n ) {
                        // select in the current updated most table
                        selectRelatedTargets( targets, 
                                theFirstOnePerTable.get( 0 ), 
                                params, 
                                n );
                    }
                }
            }
        }else {
            for( E theFirstOne : theFirstOnePerTable ) {
                if( ((TimestampEntity)theFirstOne).getCreatedTimestamp() < datetime ) {
                    
                    selectRelatedTargets( targets, theFirstOne, params, n );

                    if( targets.size() >= n ) {
                        break;
                    }
                }
            }
        }
        
        return isUP ? (List<E>)BaseUtils.reverse( targets ) : targets;
    }
    
    private void selectRelatedTargets( List<E> targets, 
            E theFirstOne, 
            Map<String, Object> params, 
            int n ) {

        params.put( "targetTable", getTableName( theFirstOne.getSid() ) );
        params.put( "n", n - targets.size() );

        targets.addAll( this.genericSelectList( "selectByRelation", params ) );
    }
    
    protected List<E> selectByMaster( Long masterSid, 
            String masterSidFieldName,
            Long datetime, 
            Constants.ReadDirection readDirection,
            int n,
            Long... excludeSids ) {
        
        Long tt = readDatetimePolicy.getReadTimestamp( 
                entityClass, 
                datetime, 
                readDirection );
        
        if( tt == null ) {
            readDirection = Constants.ReadDirection.DOWN;
        }
        
        Map<String, Object> params = newParams();
        
        params.put( VAR_TABLE_NAME, getTableName( masterSid ) );
        params.put( "masterSidFieldName", masterSidFieldName );
        params.put( "masterSid", masterSid );
        params.put( "datetime", tt );
        params.put( "excludeSids", excludeSids );
        
        boolean isUP = Constants.ReadDirection.UP.equals( readDirection );
        
        if( isUP ) {
            params.put( "order", "asc" );
            params.put( "op", ">=" );
        }else { // DOWN
            params.put( "order", "desc" );
            params.put( "op", "<=" );
        }
        
        params.put( "n", n );
        
        List<E> entities = this.genericSelectList("selectByMaster", params);
        return isUP ? (List<E>)BaseUtils.reverse( entities ) : entities;
    }
    
    @Override
    public List<E> selectByRange( int i, int n, IndexField indexField ) {
        return selectByRange(i, n, indexField, null);
    }

    protected List<E> selectByRange( int i, int n, IndexField indexField, 
            String orderBy ) {
        
        List<E> entityList = new ArrayList<E>();
        Integer[] rangeArray = indexField.range( i, n );
        if( rangeArray.length > 0 ) {
            List<Map> ranges = new ArrayList<Map>();
            ranges.add( generateRangeMap( rangeArray[0], rangeArray[1], rangeArray[2] ) );
            for( int k = 3; k < rangeArray.length; k += 2 ) {
                ranges.add( generateRangeMap( rangeArray[k], 0, rangeArray[k+1] ) );
            }
            List<Map> list = this.getSqlSession().selectList(
                    getSelectByRangeNameSpace(),
                    newParams().put( VAR_TABLE_NAME, getTableName() )
                                .put( "ranges", ranges )
                                .put( "orderBy", StringUtils.isEmpty(orderBy) 
                                        ? "createdDatetime" : orderBy )
                                .put( "order", i >= 0 ? "asc" : "desc" )
            );
            if (list != null) {
                for (Map map : list) {
                    entityList.add(this.transMapToEntity(map));
                }
            }
        }
        return entityList;
    }

    protected String getSelectByRangeNameSpace() {
        return getNameSpace(GenericDAOBatisImpl.class, "selectByRange");
    }
    
    private Map<String, String> generateRangeMap( Integer tableSplitIndex, 
            Integer i, Integer m ) {
        Map<String, String> map = new HashMap<>();
        map.put( "tableSplitIndex", tableSplitIndex != null && tableSplitIndex > 0 
                ? "_" + String.valueOf( tableSplitIndex ) : "" );
        map.put( "i", String.valueOf( i ) );
        map.put( "m", String.valueOf( m ) );
        return map;
    }
    
    protected void putInCache( Class<?> clazz, List<?> objs ) {
        if( objs == null || objs.isEmpty() ) {
            return;
        }
        try {
            Map<String, String> objsMap = new HashMap<String, String>();
            for( Object obj : objs ) {
                objsMap.put( String.valueOf( PropertyUtils.getProperty( obj, "sid" ) ), 
                        BaseUtils.toJson( obj ) );
            }
            
            redisHelper.putInCache( clazz, objsMap );
            
        }catch( Exception ex ) {
            getLogger().error( "putInCache exception: ", ex );
            throw new RuntimeException( ex );
        }
    }
    
    @Override
    public E findBySid( Long sid ) {
        return findBySid( sid, false );
    }

    @Override
    public E findByMasterSid( Long masterSid, Long sid ){
        return findByMasterSid( masterSid , sid, false );
    }
    
    @Override
    public E findBySid( Long sid, boolean ignoreFlag ) {
        
        String json = redisHelper.getFromCache( entityClass, String.valueOf( sid ) );
        
        if( StringUtils.isNotEmpty( json ) ) {
            return fromJsonCached( json );
        }
        
        E e = findBySidFromDB( sid, ignoreFlag );
        
        if( e != null ) {
            redisHelper.putInCache( entityClass, String.valueOf( sid ), 
                    BaseUtils.toJson( e ) );
        }
        
        return e;
    }
    
    protected E findBySidInHash( Long sid, boolean ignoreFlag ) {
        E e = redisHelper.getFromHash( entityClass, sid.toString() );
        if( e != null ) {
            return e;
        }
        
        e = findBySidFromDB( sid, ignoreFlag );
        
        if( e != null ) {
            redisHelper.putInHash( entityClass, sid.toString(), e );
        }
        
        return e;
    }

    protected E findBySidFromDB( Long sid, boolean ignoreFlag ) {
        return genericSelectOne( "findBySid", 
                newParams().put( VAR_TABLE_NAME, getTableName( sid ) )
                           .put( "sid", sid )
                           .put( "ignoreFlag", ignoreFlag ) );
    }
    
    @Override
    public final E findByMasterSid(Long masterSid ,  Long sid, boolean ignoreFlag ) {
        
        String json = redisHelper.getFromCache( entityClass, String.valueOf( sid ) );
        
        if( StringUtils.isNotEmpty( json ) ) {
            return BaseUtils.fromJson( json, entityClass );
        }
        
        E e = findByMasterSidFromDB( masterSid, sid, ignoreFlag );
        
        if( e != null ) {
            redisHelper.putInCache( entityClass, String.valueOf( sid ), 
                    BaseUtils.toJson( e ) );
        }
        
        return e;
    }
    
    protected E findByMasterSidFromDB( Long masterSid ,  Long sid, boolean ignoreFlag ) {
        return genericSelectOne( "findBySid", 
                newParams().put( VAR_TABLE_NAME, getTableName( masterSid ) )
                           .put( "sid", sid )
                           .put( "ignoreFlag", ignoreFlag ) );
    }
    
    @Override
    public List<E> findBySid( Collection<Long> sids ) {
        
        if( sids == null || sids.isEmpty() ) {
            return new ArrayList<E>();
        }
        
        List<E> result = new ArrayList<E>();
        
        List<String> keys = new ArrayList<String>();
        
        for( Long sid : sids ) {
            keys.add( String.valueOf( sid ) );
        }
        
        List<Long> notCached = new ArrayList<Long>();
        
        Map<String, String> contentMap = redisHelper.getFromCache( 
                entityClass, keys );
        
        if( contentMap != null ) {
            for( Long i : sids ) {
                String json = contentMap.get( String.valueOf( i ) );
                if( StringUtils.isNotEmpty( json ) ) {
                    result.add( BaseUtils.fromJson( json, entityClass ) );
                }else {
                    notCached.add( i );
                }
            }
        }else {
            notCached.addAll( sids );
        }
        
        if( ! notCached.isEmpty() ) {
            
            List<E> fromStorage = findBySidFromDB( notCached );
            
            if( fromStorage != null && ! fromStorage.isEmpty() ) {
                result.addAll( fromStorage );
                putInCache( entityClass, fromStorage );
            }
        }

        return orderBySid( sids, result );
    }
    
    @Override
    public List<E> findBySid( Collection<Long> sids, boolean ignoreFlag ) {
        
        if( sids == null || sids.isEmpty() ) {
            return new ArrayList<E>();
        }
        
        List<E> result = new ArrayList<E>();
        
        List<String> keys = new ArrayList<String>();
        
        for( Long sid : sids ) {
            keys.add( String.valueOf( sid ) );
        }
        
        List<Long> notCached = new ArrayList<Long>();
        
        Map<String, String> contentMap = redisHelper.getFromCache( 
                entityClass, keys );
        
        if( contentMap != null ) {
            for( Long i : sids ) {
                String json = contentMap.get( String.valueOf( i ) );
                if( StringUtils.isNotEmpty( json ) ) {
                    result.add( fromJsonCached( json ) );
                }else {
                    notCached.add( i );
                }
            }
        }else {
            notCached.addAll( sids );
        }
        
        if( ! notCached.isEmpty() ) {
            
            List<E> fromStorage = findBySidFromDB( notCached, ignoreFlag );
            
            if( fromStorage != null && ! fromStorage.isEmpty() ) {
                result.addAll( fromStorage );
                putInCache( entityClass, fromStorage );
            }
        }

        return orderBySid( sids, result );
    }
    
    protected E fromJsonCached( String json ) {
        return BaseUtils.fromJson( json, entityClass );
    }
    
    protected List<E> findBySidInHash( Collection<Long> sids ) {
               
        return findBySidInHash( sids, false );
    }
    
    protected List<E> findBySidInHash( Collection<Long> sids , boolean ignoreFlag) {
        
        List<E> entities = new ArrayList<E>();
        
        if( sids == null || sids.isEmpty() ) {
            return entities;
        }
        
        List<String> ss = new ArrayList<String>();
        for( Long sid : sids ) {
            ss.add( sid.toString() );
        }
        
        Map<String, E> chachedEntites = redisHelper.getFromHash( entityClass, ss );
        
        List<Long> notCached = new ArrayList<>();
        
        for( Long sid : sids ) {
            String key = sid.toString();
            if( chachedEntites.containsKey( key ) ) {
                entities.add( chachedEntites.get( key ) );
            }else {
                notCached.add( sid );
            }
        }
        
        if( ! notCached.isEmpty() ) {
            List<E> notCachedEntities = this.findBySidFromDB( notCached ,ignoreFlag);
            Map<String, E> notCachedEntitiesMap = new HashMap<>();
            for( E nce : notCachedEntities ) {
                notCachedEntitiesMap.put( nce.getSid().toString(), nce );
            }
            redisHelper.putInHash( entityClass, notCachedEntitiesMap );
            
            entities.addAll( notCachedEntities );
        }

        
        return orderBySid( sids, entities );
    }
    
    private List<E> orderBySid( Collection<Long> sids, List<E> entities ) {
        
        if( ! ( sids instanceof List ) ) {
            return entities;
        }
        
        List<E> tempEntities = new ArrayList<>( entities.size() );
        
        for (Long sid : sids) {
            for( E e : entities ) {
                if( sid.equals( e.getSid() ) ) {
                    tempEntities.add( e );
                    break;
                }
            }
        }
        
        return tempEntities;
    }
    
    protected List<E> findBySidFromDB( List<Long> sids ) {
        
        return findBySidFromDB( sids, false );
    }
    
    protected List<E> findBySidFromDB( List<Long> sids, boolean ignoreFlag  ) {
        if( sids == null || sids.isEmpty() ) {
            return new ArrayList<>();
        }
        
        return genericSelectList( "findBySids",
                newParams().put( VAR_TABLES_WITH_SIDS, collapseByTable( sids ) )
                           .put( "ignoreFlag", ignoreFlag ) );
    }
    
    @Override
    public final int insert( E entity ) {
        if( entity == null ) {
            return FAIL;
        }

        List<E> entities = new ArrayList<>();
        entities.add( entity );

        if( BaseUtils.isNotValidSid( entity.getSid() ) ) {
            entity.setSid( generateSid() );
        }

        return insert( entities );
            
    }
    
    @Override
    public final void insert( Long masterSid , E entity ) {
        
        List<E> entities = new ArrayList<>();
        entities.add( entity );

        if( BaseUtils.isNotValidSid( entity.getSid() ) ) {
            entity.setSid( generateSid() );
        }
        
        insert(  masterSid , entities );
            
    }
    
    @Override
    public int insert( List<E> entities ) {

        if( entities == null ) {
            return 0;
        }
        
        for( E e : entities ) {
            if( BaseUtils.isNotValidSid( e.getSid() ) ) {
                e.setSid( generateSid() );
            }
        }
        
        return genericInsert( GenericDAOBatisImpl.class, entities );
    }

    @Override
    public void insert( Long masterSid , List<E> entities ) {
                
        if( entities == null ) {
            return;
        }
        
        for( E e : entities ) {
            if( BaseUtils.isNotValidSid( e.getSid() ) ) {
                e.setSid( generateSid() );
            }
        }
        
        genericInsertByMaster(masterSid, entities );
    }
    protected int genericInsert( List<? extends BaseEntity> entities ) {
        return genericInsert( GenericDAOBatisImpl.class, entities );
    }
    
    @SuppressWarnings({"rawtypes", "UseSpecificCatch"})
    protected int genericInsert( 
            Class<? extends GenericDAOBatisImpl> daoImplclass, 
            List<? extends BaseEntity> entities ) {
        
        if(entities == null || entities.size()<=0){
        	return 0;
        }
        
        int n = 0;
        
        try {
        
            Map<String, Map<String, List<?>>> tableFieldsAndValues =
                    flatTableFieldsAndValues( entities );
            
            Map<String, Map<String, List<?>>> tempTableFieldsAndValues 
                    = new HashMap<>();
            
            int i = 0;
            
            for( String tableName : tableFieldsAndValues.keySet() ) {
             
                Map<String, List<?>> fvs = tableFieldsAndValues.get( tableName );
                
                Map<String, List<?>> tempFieldsAndValues = new HashMap<>();
                
                List<List<?>> tempValuesList = new ArrayList<>();

                tempFieldsAndValues.put( VAR_FIELDS, fvs.get( VAR_FIELDS ) );
                tempFieldsAndValues.put( VAR_VALUES, tempValuesList );

                tempTableFieldsAndValues.put( tableName, tempFieldsAndValues );
                
                List<List<?>> vList = (List<List<?>>)fvs.get( VAR_VALUES );
                int k = vList.size();
                
                for( int j = 0; j < k; j++ ) {
                    
                    tempValuesList.add( vList.get( j ) );
                    
                    if( ( ++i % BATCH_SIZE ) == 0 ) {
                        // insert
                        n += getSqlSession().insert( 
                            getNameSpace( daoImplclass, "batchInsert" ), 
                             newParams().put( VAR_TABLES_WITH_FIELDS_VALUES, 
                                     tempTableFieldsAndValues) );
                        
                        tempTableFieldsAndValues.clear();
                        
                        if( j < ( k - 1 ) ) {
                            tempValuesList.clear();
                            tempTableFieldsAndValues.put( tableName, 
                                    tempFieldsAndValues );
                        }
                        
                        i = 0;
                    }
                }
            }
            
            if( i > 0 ) {
                // insert
                n += getSqlSession().insert( 
                    getNameSpace( daoImplclass, "batchInsert" ), 
                     newParams().put( VAR_TABLES_WITH_FIELDS_VALUES, 
                             tempTableFieldsAndValues) );
            }
            
            return n;
            
        }catch( Exception ex ) {
            getLogger().error( "insert entities failed with exception: " 
                         + entities.get( 0 ).getClass().getName(), ex );
            
            throw new RuntimeException( ex );
        }
    }
    
    protected void genericInsertByMaster( Long masterSid, E entity ) {
        List<E> entities = new ArrayList<>();
        entities.add( entity );
        genericInsertByMaster( masterSid, entities );
    }
    
    protected void genericInsertByMaster( Long masterSid, 
            List<? extends BaseEntity> entities ) {
        
        genericInsertByTable( getTableName( masterSid ), entities );
    }    
    
    protected void genericInsertByTable(String tableName,
            List<? extends BaseEntity> entities ) {
        
        if(entities == null || entities.size()<=0){
        	return;
        }
        List<List<?>> valuesList = new ArrayList<>();
        
        Map<String, Object> params = new HashMap<>();
        params.put( VAR_TABLE_NAME,  tableName);
        
        try {
            
            Timestamp currentTimestamp = DatetimeUtils.currentTimestamp();
            
            for( int i = 0; i < entities.size(); i++ ) {
                
                BaseEntity e = entities.get( i );
                
                if( e.getCreatedDatetime() == null ) {
                    e.setCreatedDatetime( currentTimestamp );
                }
                
                if( e.getUpdatedDatetime() == null ) {
                    e.setUpdatedDatetime( currentTimestamp );
                }
                
                Map<String, List<?>> mapFieldsAndValues
                              = BaseUtils.describe( e );
                if( i == 0 ) {
                    params.put( 
                            "fields", mapFieldsAndValues.get( "fields" ) );
                }
            
                valuesList.add( 
                        ( List<?> )mapFieldsAndValues.get( "values" ) );
            }
            
            List<List<?>> tempValuesList = new ArrayList<>();
            for( List<?> list : valuesList ) {
                tempValuesList.add( list );
                if( tempValuesList.size() % BATCH_SIZE == 0 ) {
                    params.put( "values", tempValuesList );
                    getSqlSession().insert( 
                            getNameSpace( GenericDAOBatisImpl.class, "batchInsertInOneTable" ), 
                            params );
                    tempValuesList.clear();
                }
            }
            
            if( tempValuesList.size() > 0 ) {
                params.put( "values", tempValuesList );
                getSqlSession().insert( 
                    getNameSpace( GenericDAOBatisImpl.class, "batchInsertInOneTable" ), 
                    params );
            }
            
        }catch( Exception ex ) {
            getLogger().error( "insert entities failed with exception: " 
                         + entities.get( 0 ).getClass().getName(), ex );
            
            throw new RuntimeException( ex );
        }
    }    
    
    protected Map<String, Map<String, List<?>>> 
            flatTableFieldsAndValues( List<? extends BaseEntity> entities ) 
                    throws Exception {
        
        Map<String, Map<String, List<?>>> tableFieldsAndValues = new HashMap<>();
        
        Map<String, List<? extends BaseEntity>> collapedEntities = 
                collapseByTableEh( entities );
        
        for( String tableName : collapedEntities.keySet() ) {
            
            tableFieldsAndValues.put( tableName, 
                extractFieldsAndValuesPerTable( 
                        collapedEntities.get( tableName ) ) );
        }
        
        return tableFieldsAndValues;
    }
    
    protected Map<String, List<?>>
        extractFieldsAndValuesPerTable( List<? extends BaseEntity> entities ) 
                throws Exception {
        
        Map<String, List<?>> fieldsAndValuesPerTable = new HashMap<>();
        
        int i = 0;
        
        for( BaseEntity e : entities ) {
            Map<String, List<?>> fieldsAndValues = extractFieldsAndValues( e );
            if( i++ == 0 ) {
                fieldsAndValuesPerTable.put( VAR_FIELDS, 
                        fieldsAndValues.get( VAR_FIELDS ) );
            }
            List<Object> values = 
                    ( List<Object> )fieldsAndValuesPerTable.get( VAR_VALUES );
            if( values == null ) {
                values = new ArrayList<>();
                fieldsAndValuesPerTable.put( VAR_VALUES, values );
            }
            values.add( fieldsAndValues.get( VAR_VALUES ) );
        }
        
        return fieldsAndValuesPerTable;
    }
    
    protected <ET extends BaseEntity> Map<String, List<?>> 
        extractFieldsAndValues( ET e ) throws Exception {
        
        Timestamp currentTimestamp = DatetimeUtils.currentTimestamp();
        
        if( e.getCreatedDatetime() == null ) {
            e.setCreatedDatetime( currentTimestamp );
        }

        if( e.getUpdatedDatetime() == null ) {
            e.setUpdatedDatetime( currentTimestamp );
        }
        
        Map<String, List<?>> described = BaseUtils.describe( e );
        
        Map<String, List<?>> fieldsAndValues = new HashMap<>();
        fieldsAndValues.put( VAR_FIELDS, described.get( "fields" ) );
        fieldsAndValues.put( VAR_VALUES, described.get( "values" ) );
        
        return fieldsAndValues;
    }
    
    @Override
    public int update( E entity ) {
        entity.setUpdatedDatetime( DatetimeUtils.currentTimestamp() );
        return update( "update", entity );
    }
    
    protected int update( String operate, Object param ) {
        return getSqlSession().update( getNameSpace( operate ), param );
    }
    
    @Override
    public int update( List<E> entities ) {
        if( entities != null && entities.size() > 0 ) {
            return getSqlSession().update( getNameSpace( "batchUpdate" ), entities );
        }
        return 0;
    }
    
    protected void update( String operate, List<E> entities ) {
        update( operate, ( Object )entities );
    }
    
    @Override
    public final int deleteBySid( Long sid ) {
        int n = getSqlSession().update( 
                getNameSpace( GenericDAOBatisImpl.class, "deleteBySid" ), 
                newParams().put( VAR_TABLE_NAME, getTableName( sid ) )
                           .put( "sid", sid ) );
        
        removeFromCache( sid );
        
        return n;
    }

    @Override
    public final int deleteBySid( Collection<Long> sids ) {
        
        int n = -1;
        
        if( sids.size() > 0 ) {
            
            n = getSqlSession().update(
                getNameSpace( GenericDAOBatisImpl.class, "batchDeleteBySids" ),
                newParams().put( VAR_TABLES_WITH_SIDS, 
                        collapseByTable( sids ) ) ); 
            
            removeFromCache( sids );
        }
        
        return n <= 0 ? 0 : sids.size();
    }
    
    @Override
    public final int delete( E entity ) {
        return deleteBySid( entity.getSid() );
    }

    @Override
    public final int delete( Collection<E> entities ) {
        List<Long> sids = new ArrayList<>();
        if( entities != null && entities.size() > 0 ) {
            for( E e : entities ) {
                sids.add( e.getSid() );
            }
        }
        
        return deleteBySid( sids );
    }

    @Override
    public void setActived( Long sid, boolean actived ) {
        if( BaseUtils.isNotValidSid(sid) ) {
            return;
        }
        setActivedInDB( sid, actived );
        removeFromCache( sid );
    }
    
    protected void setActivedInDB( Long sid, boolean actived ) {
        
        this.getSqlSession().update( 
                getNameSpace( GenericDAOBatisImpl.class, "setActived" ),
                newParams().put( VAR_TABLE_NAME, getTableName( sid ) )
                    .put( "sid", sid )
                    .put( "actived", actived ));
    }
    
    protected void removeFromCache( Long sid ) {
        if( BaseUtils.isNotValidSid(sid) ) {
            return;
        }
        List<Long> sids = new ArrayList<Long>( 1 );
        sids.add( sid );
        
        removeFromCache( sids );
    }
    
    protected void removeFromCache( Collection<Long> sids ) {
        if( sids == null || sids.isEmpty() ) {
            return;
        }
        
        redisHelper.removeFromCache( entityClass, BaseUtils.mapToString( sids ) );
    }
    
    // 指定字段数值+1
    @Override
    public void increaseFieldValue( Long sid, String fieldName ) {
        increaseFieldValue( sid, fieldName, 1 );
    }
    
    // 增加指定字段数值
    @Override
    public void increaseFieldValue( Long sid, String fieldName, int n ) {
        
        increaseFieldValue( entityClass, sid, fieldName, n );
    }
    
    protected void increaseFieldValue( Class<?> clazz, Long sid, String fieldName ) {
        
        increaseFieldValue( clazz, sid, fieldName, 1 );
    }
    
    protected void increaseFieldValue( Class<?> clazz, Long sid, String fieldName, int n ) {
        
        increaseFieldValueInDB( sid, fieldName, n );
        
        redisHelper.removeFromCache( clazz, String.valueOf( sid ) );
    }
    
    protected void increaseFieldValueInHash( Long sid, String fieldName, int n ) {
        increaseFieldValueInHash( sid, fieldName, n, false );
    }
    
    protected void increaseFieldValueInHash( Long sid, String fieldName, int n, boolean needUpdateDatetime ) {
        
        increaseFieldValueInHash( entityClass, sid, fieldName, n, needUpdateDatetime );
    }
    
    protected void increaseFieldValueInHash( Class<?> clazz, Long sid, String fieldName, int n, boolean needUpdateDatetime ) {
        
        int i = increaseFieldValueInDB( sid, fieldName, n, needUpdateDatetime );
        
        if( i == 1 ) {
           redisHelper.increaseFieldValueInHash( clazz, 
                   sid.toString(), 
                   fieldName, 
                   n );
        }
        
        if( i == 1 && needUpdateDatetime ) {
            
           redisHelper.setFieldValueInHash( clazz, sid.toString(), 
                        "updatedDatetime", 
                        DatetimeUtils.formatTimestamp( DatetimeUtils.currentTimestamp() ) );
            
            
        }
    }
    
    protected int increaseFieldValueInDB( Long sid, String fieldName, int n ) {
        return increaseFieldValueInDB( sid, fieldName, n, false );
    }
    
    protected int increaseFieldValueInDB( Long sid, String fieldName, int n, boolean needUpdateDatetime ) {
        
        Map params = newParams().put( VAR_TABLE_NAME, getTableName( sid ) )
                    .put( "sid", sid )
                    .put( "fieldName", fieldName )
                    .put( "n", n );
        
        if( needUpdateDatetime ) {
            params.put( "updatedDatetime", DatetimeUtils.currentTimestamp() );
        }
        
        return this.getSqlSession().update( 
                getNameSpace( GenericDAOBatisImpl.class, "increaseFieldValue" ),
                params );
    }
    
    @Override
    public void resetFieldValue( Long sid, String fieldName ) {
        resetFieldValueInDB( sid, fieldName );
        redisHelper.removeFromCache( entityClass, String.valueOf( sid ) );
    }
    
    protected void resetFieldValueInHash( Long sid, String fieldName ) {
        resetFieldValueInHash( entityClass, sid, fieldName );
    }
    
    protected void resetFieldValueInHash( Class<?> clazz, Long sid, String fieldName ) {
        if( clazz == null || BaseUtils.isNotValidSid(sid) || StringUtils.isEmpty( fieldName ) ) {
            return;
        }
        resetFieldValueInDB( sid, fieldName );
        redisHelper.setFieldValueInHash( clazz, String.valueOf( sid ), fieldName, "0" );
    }
    
    protected void resetFieldValueInDB( Long sid, String fieldName ) {
        
        this.getSqlSession().update( 
                getNameSpace( GenericDAOBatisImpl.class, "resetFieldValue" ),
                newParams().put( VAR_TABLE_NAME, getTableName( sid ) )
                    .put( "sid", sid )
                    .put( "fieldName", fieldName ));
    }
    
    public String getNameSpace( String operate ) {
        return getNameSpace( daoImplClass, operate );
    }
    
    @SuppressWarnings("rawtypes")
    public static String getNameSpace( Class daoImplClass, String operate ) {
        String nameSpace = NAME_SPACE_MAP.get( daoImplClass );
        if( nameSpace == null || "".equals( nameSpace ) ) {
            nameSpace = daoImplClass.getName() + NAME_SPACE_SUFFIX;
            NAME_SPACE_MAP.put( daoImplClass, nameSpace );
        }
        return nameSpace + "." + operate;
    }
    
    protected String getTableName() {
        return getTableName( 0L );
    }
    
    protected String getTableName( Long sid ) {
        return splitTablePolicy.getTableName( daoImplClass, entityClass, sid );
    }
    
    protected String getTableName( String tableName, Long sid ) {
        return splitTablePolicy.getTableName( tableName, sid );
    }
    
    protected String getTableNoSuffix( Long sid ) {
        return getTableNameSuffixByTableNo( getTableNo( sid ) );
    }
    
    protected String getTableNoSuffix( String tableName, Long sid ) {
        return getTableNameSuffixByTableNo( getTableNo( tableName, sid ) );
    }
    
    protected String getTableNameSuffixByTableNo( int tableNo ) {
        return tableNo > 0 ? VAR_TABLE_NAME_SEPARATOR + String.valueOf( tableNo ) : "";
    }
    
    @Override
    public int getTableNo( Long sid ) {
        return splitTablePolicy.getTableNo( daoImplClass, entityClass, sid );
    }
    
    protected int getTableNo() {
        return getTableNo( daoImplClass, entityClass );
    }
    
    protected int getTableNo( String tableName, Long sid ) {
        return splitTablePolicy.getTableNo( tableName, sid );
    }
    
    protected 
    <ET extends BaseEntity, D extends GenericDAO<ET>>
    int getTableNo( Class<D> daoImplClass, Class<ET> entityClass ) {
        return splitTablePolicy.getTableNo( daoImplClass, 
                entityClass, 
                sidGenerator.peek( entityClass ) );
    }
    
    protected Map<String, Set<Long>> collapseByTableSplitSuffix( Collection<Long> sids ) {
        return collapseByTableSplitSuffix( getTableName(), sids );
    }
    
    protected Map<String, Set<Long>> collapseByTableSplitSuffix( String xtableName, Collection<Long> sids ) {
        
        Map<String, Set<Long>> tableSplitSuffixsWithSids = new HashMap<>();
        
        if( sids != null && sids.size() > 0 ) {
            for( Long sid : sids ) {
                String tableName = this.getTableNoSuffix( xtableName, sid );
                Set<Long> sidsPerTable = tableSplitSuffixsWithSids.get( tableName );
                if( sidsPerTable == null ) {
                    sidsPerTable = new HashSet<>();
                    tableSplitSuffixsWithSids.put( tableName, sidsPerTable );
                }
                sidsPerTable.add( sid );
            }
        }
        
        return tableSplitSuffixsWithSids;
    }    
    
    protected Map<String, Set<Long>> collapseByTable( Collection<Long> sids ) {
        
        return collapseByTable( getTableName(), sids );
    }
    
    protected Map<String, Set<Long>> collapseByTable( String xtableName, Collection<Long> sids ) {
        
        Map<String, Set<Long>> tablesWithSids = new HashMap<>();
        
        if( sids != null && sids.size() > 0 ) {
            for( Long sid : sids ) {
                String tableName = this.getTableName( xtableName, sid );
                Set<Long> sidsPerTable = tablesWithSids.get( tableName );
                if( sidsPerTable == null ) {
                    sidsPerTable = new HashSet<>();
                    tablesWithSids.put( tableName, sidsPerTable );
                }
                sidsPerTable.add( sid );
            }
        }
        
        return tablesWithSids;
    }    
    
    protected Map<String, List<? extends BaseEntity>> 
        collapseByTableEh( List<? extends BaseEntity> entities ) {
        
        return collapseByTableEh( entities, SID );
    }
        
    protected Map<String, List<? extends BaseEntity>> 
        collapseByTableEh( List<? extends BaseEntity> entities, String sidFieldName ) {
        
        Map<String, List<? extends BaseEntity>> tablesWithEntities = new HashMap<>();
        
        try {
            
            if( entities != null && ! entities.isEmpty() ) {
                for( BaseEntity e : entities ) {
                    
                    Long sid;
                    
                    if( SID.equals( sidFieldName ) ) {
                        sid = e.getSid();
                    }else {
                        sid = (Long)PropertyUtils.getProperty( e, sidFieldName );
                    }
                    
                    String tableName = this.getTableName( sid );
                    
                    List<BaseEntity> entitiesPerTable = 
                            ( List<BaseEntity> )tablesWithEntities.get( tableName );
                    
                    if( entitiesPerTable == null ) {
                        entitiesPerTable = new ArrayList<>();
                        tablesWithEntities.put( tableName, entitiesPerTable );
                    }
                    entitiesPerTable.add( e );
                }
            }
            
        }catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
        
        return tablesWithEntities;
    }
    
    protected Logger getLogger() {
        return BaseUtils.getLogger( this.daoImplClass );
    }
    
    protected List<E> selectList( String operate ) {
        return selectList( operate, null );
    }
    
    protected List<E> selectList( String operate, Object param ) {
        return selectList( this.daoImplClass, operate, param );
    }
    
    protected List<E> selectList( Class<?> clzss, 
            String operate, 
            Object param ) {
        
        return getSqlSession()
                .selectList( getNameSpace( clzss, operate ), param );
    }
    
    protected E selectOne( Class<?> clzss, 
            String operate, 
            Object param ) {
        
        return getSqlSession()
                .selectOne( getNameSpace( clzss, operate ), param );
    }
    
    protected List<E> selectList( String operate, 
            int pageIndex, int pageSize ) {
        
        return selectList( operate, null, pageIndex, pageSize );
    }
    
    protected List<E> selectList( String operate, Object param, 
            int pageIndex, int pageSize ) {
        
        return selectList( this.daoImplClass, operate, param, 
                pageIndex, pageSize );
    }
    
    protected List<E> selectList( Class<?> clzss, 
            String operate, 
            Object param, 
            int pageIndex, int pageSize ) {
        
        return getSqlSession()
                .selectList( getNameSpace( clzss, operate ), param, 
                        makeRowBounds( pageIndex, pageSize ) );
    }
   
    protected E selectOne( String operate ) {
        return selectOne( operate, null );
    }
    
    @SuppressWarnings("unchecked")
    protected E selectOne( String operate, Object param ) {
        return ( E )getSqlSession()
                      .selectOne( getNameSpace( operate ), param );
    }

    protected E genericSelectOne( String operate, Object param ) {
        
        GenericResultHandler resultHandler = new GenericResultHandler();

        getSqlSession().select(
                getNameSpace( GenericDAOBatisImpl.class, operate ),
                param,
                resultHandler );
        
        List<E> entities = resultHandler.getResult();
        
        return entities != null && entities.size() > 0 
                ? entities.get( 0 ) : null;
    }

    protected List<E> genericSelectList( String operate, Object param ) {
        
        GenericResultHandler resultHandler = new GenericResultHandler();

        getSqlSession().select( 
                getNameSpace( GenericDAOBatisImpl.class, operate ), 
                param,
                resultHandler );
        
        return resultHandler.getResult();
    }

    protected RowBounds makeRowBounds( int pageIndex, int pageSize ) {
        
        pageIndex = pageIndex < 1 ? 1 : pageIndex;
        pageSize = ( pageSize <= 0 || pageSize > PAGESIZE_MAX )
                        ? PAGESIZE_MAX : pageSize;
        
        return new RowBounds( ( pageIndex - 1 ) * pageSize, pageSize );
    }
    
    /**
     * internal ResultHandler class
     *
     */    
    protected class GenericResultHandler implements ResultHandler {

        private final List<E> entities = new ArrayList<>();
        
        @Override
        public void handleResult(ResultContext context) {
            @SuppressWarnings( "unchecked" )
            Map<String, Object> map = 
            ( Map<String, Object> )context.getResultObject();
            E entity = transMapToEntity(map);
            if( entity != null ) {
                entities.add( entity );
            }
        }
        
        public List<E> getResult() {
            return entities;
        }

    }

    protected List<E> transMapToEntity( List<Map> values ) {
        List<E> entities = new ArrayList<>();
        if( values != null ) {
            for( Map map : values ) {
                entities.add( transMapToEntity( map ) );
            }
        }
        return entities;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected E transMapToEntity(Map map) {
        if( map != null ) {
            E entity = newEntity();
            
            if( entity instanceof IndexEntity) {
                try {
                    for( Object key : map.keySet() ) {
                        Class<?> pClass =
                        PropertyUtils.getPropertyType( entity, key.toString() );
                        if( IndexField.class.isAssignableFrom( pClass ) ) {
                            IndexField indexField = 
                                    ( IndexField )pClass.newInstance();
                            indexField.setIndexContent( 
                                    map.get( key ).toString() );
                            map.put( key, indexField );
                        }
                    }
                }catch( Exception ex ) {
                    getLogger().error( 
                        "GenericResultHandler.handleResult exception",
                    ex );
                }
            }
            
            try {
                BaseUtils.populate( entity, map );
            }catch( Exception ex ) {
                getLogger().error( 
                    "GenericResultHandler.handleResult exception",
                    ex );
            }
            return entity;
        }
        return null;
    }
    
    protected E newEntity() {
        return objectFactory.create( 
                    GenericDAOBatisImpl.this.entityClass );
    }
    
    protected Boolean tryToUpdate( Callable<Boolean> callback ) {
        try {
            int i = 0;
            do {
                if( callback.call() ) {
                    return Boolean.TRUE;
                }
                
                // sleep for a while if someone else update the same record simultaneously.
                Thread.sleep( 50 );
                
            } while( ++i < MAX_TRY_TIMES );
            
            // failed to do the update action in max try times
            throw new TryToUpdateException( "tryToUpdate exceeded max retry times: " + MAX_TRY_TIMES );
            
        }catch( TryToUpdateException ex ) {
            throw ex;
        }catch( Exception ex ) {
            getLogger().error( "tryToUpdate exception occurred!!!", ex );
            throw new TryToUpdateException( ex );
        }
    }
    
    protected static Params newParams() {
        return new Params();
    }
    
    /**
     * convenient class to hold params
     * 
     */
    protected static class Params extends HashMap<String, Object> {
        
        private static final long serialVersionUID = 5944659754462710346L;

        @Override
        public Params put( String key, Object value ) {
            super.put( key, value );
            return this;
        }
    }
    
    protected Long generateSid() {
        return this.sidGenerator.generate( entityClass );
    }
    
}