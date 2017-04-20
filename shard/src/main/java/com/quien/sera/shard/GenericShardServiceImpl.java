/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.shard;

import com.quien.sera.common.entity.BaseEntity;
import com.quien.sera.common.util.BaseUtils;
import com.quien.sera.common.util.DatetimeUtils;
import com.quien.sera.dao.GenericDAO;
import com.quien.sera.dao.util.SidGenerator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public abstract class GenericShardServiceImpl<E extends BaseEntity,
                                 T extends GenericShardServiceImpl<E, T>>
    implements GenericShardService<E> {

    private final GenericDAO<E> genericDAO;
    
    private final Class<E> entityClass;
    private final Class<T> serviceImplClass;

    @Autowired
    private SidGenerator sidGenerator;
    
    public GenericShardServiceImpl( final GenericDAO<E> genericDAO ) {
         
        this.genericDAO = genericDAO;
        
        Type[] types = ( ( ParameterizedType )getClass()
                .getGenericSuperclass() ).getActualTypeArguments();
        
        entityClass = ( Class<E> )types[0];
        serviceImplClass = ( Class<T> )types[1];
       
    }

    @Transactional( propagation = Propagation.REQUIRED, readOnly = true )
    public E getBySid(Long sid) {
        return this.genericDAO.findBySid( sid );
    }

    @Transactional( propagation = Propagation.REQUIRED, readOnly = true )
    public E getBySid(Long sid, boolean ignoreFlag) {
        return this.genericDAO.findBySid( sid, ignoreFlag );
    }

    @Transactional( propagation = Propagation.REQUIRED, readOnly = true )
    public E getByMasterSid( Long masterSid, Long sid) {
        return this.genericDAO.findByMasterSid( masterSid, sid);
    }

    @Transactional( propagation = Propagation.REQUIRED, readOnly = true )
    public List<E> getBySid( Collection<Long> sids) {
        return this.genericDAO.findBySid( sids );
    }

    @Transactional( propagation = Propagation.REQUIRED, readOnly = true )
    public List<E> getBySid( Collection<Long> sids, boolean ignoreFlag ) {
        return this.genericDAO.findBySid( sids ,ignoreFlag );
    }

    @Transactional( propagation = Propagation.REQUIRED )
    public int save(E entity) {
        Long sid = entity.getSid();
        if( BaseUtils.isNotValidSid(sid) ) {
            return add( entity );
        }else {
            return this.genericDAO.update( entity );
        }
    }

    @Transactional( propagation = Propagation.REQUIRED )
    public int add(E entity) {
        Long sid = entity.getSid();
        if( sid == null ) {
            entity.setSid( generateSid() );
        }
        
        if( entity.getCreatedDatetime() == null ) {
            entity.setCreatedDatetime( DatetimeUtils.currentTimestamp() );
        }
        
        if( entity.getUpdatedDatetime() == null ) {
            entity.setUpdatedDatetime( entity.getCreatedDatetime() );
        }
        
        return this.genericDAO.insert( entity );
    }

    @Transactional( propagation = Propagation.REQUIRED )
    public int add( List<E> entities) {
        
        if( entities == null || entities.isEmpty() ) {
            return 0;
        }
        
        for( E entity : entities ){
            Long sid = entity.getSid();
            if( sid == null ) {
                entity.setSid( generateSid() );
            }
            
            if( entity.getCreatedDatetime() == null ) {
                entity.setCreatedDatetime( DatetimeUtils.currentTimestamp() );
            }
            
            if( entity.getUpdatedDatetime() == null ) {
                entity.setUpdatedDatetime( entity.getCreatedDatetime() );
            }
        }
        
        return this.genericDAO.insert( entities );
    }

    public Long generateSid() {
        return generateSid( entityClass );
    }

    public Long generateSid( Class<? extends BaseEntity> entityClass ) {
             
        return sidGenerator.generate( entityClass );
    }
    
    protected Logger getLogger() {
        return BaseUtils.getLogger( this.serviceImplClass );
    }
    
    protected E newEntityObject() {
        E obj = null;
        try {
            obj = this.entityClass.newInstance();
        }catch( Exception ex ) {
            getLogger().error( "newEntityObject exception with entityClass=" 
                    + this.entityClass.getName(), 
                    ex );
            throw new RuntimeException( ex );
        }
        return obj;
    }

}