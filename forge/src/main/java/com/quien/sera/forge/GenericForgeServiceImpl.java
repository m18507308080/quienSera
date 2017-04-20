/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.forge;

import com.quien.sera.base.Environment;
import com.quien.sera.common.entity.BaseEntity;
import com.quien.sera.common.util.BaseUtils;
import com.quien.sera.common.vo.BaseVO;
import com.quien.sera.shard.GenericShardService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GenericForgeServiceImpl<V extends BaseVO,
                                E extends BaseEntity,
                                S extends GenericShardService<E>,
                                F extends GenericForgeServiceImpl<V, E, S, F>>
    implements GenericForgeService<V, E> {

    private final static String MASTER_CTX_ID = ShardContext.MASTER_CTX_ID;
    private final static String MASTER = "master";
    
    private final Map<Integer, Integer> roundNumberMap = new HashMap<Integer, Integer>();
    
    private static final Pattern P_SHARD_SERVICE = 
            Pattern.compile( "^(.+ShardService)$" );
    
    private static final Pattern METHOD_SET = Pattern.compile( "^set_(.+)$" );

    private static final Pattern METHOD_SET2 = Pattern.compile( 
            "^set_([^_]+)_(.+)$" );
    
    private static final List<Pattern> methodSetPatterns;
    
    private final Class<V> viewObjectClass;
    private final Class<E> entityClass;
    private final Class<S> shardServiceClass;
    private final Class<F> forgeServiceImplClass;
    
    @Autowired
    private MessageSource messageSource;
    
    @Autowired
    private ShardContext shardContext;
    
    private S masterShardService;
    private List<S> shardServices;

    private final static Map<String, List<? extends GenericShardService>>
           shardServicesMap = new HashMap<>();
    
    private final static Map<String, List<? extends GenericShardService>>
           masterShardServicesMap = new HashMap<>();
    
    static {
        // note: the order must be correct
        methodSetPatterns = new ArrayList<Pattern>();
        methodSetPatterns.add( METHOD_SET2 );
        methodSetPatterns.add( METHOD_SET );
    }
    
    public GenericForgeServiceImpl() {
        Type[] types = ( ( ParameterizedType )getClass()
                .getGenericSuperclass() ).getActualTypeArguments();
        
        this.viewObjectClass = ( Class<V> )types[0];
        this.entityClass = ( Class<E> )types[1];
        this.shardServiceClass = ( Class<S> )types[2];
        this.forgeServiceImplClass = ( Class<F> )types[3];
    }
    
    public String getMessage( String code, 
            Object[] args, 
            String defaultMessage ) {
        return messageSource.getMessage(code,
                args,
                defaultMessage,
                Locale.CHINA);
    }
    
    public String getMessage( String code ) {
        return this.getMessage(code, null);
    }
    
    public String getMessage( String code, Object[] args ) {
        return messageSource.getMessage(code, args, Locale.CHINA);
    }
    
    @Override
    public V getBySid( Long sid ) {
        E e = getShardService().getBySid( sid );
        return this.transToViewObject( e );
    }
    
    @Override
    public List<V> getBySid( Collection<Long> sids ) {
        List<E> entities = getShardService().getBySid( sids );
        return this.transToViewObject( entities );
    }
    
    @Override
    public List<V> getBySid( Collection<Long> sids , boolean ignoreFlag ) {
        List<E> entities = getShardService().getBySid( sids , ignoreFlag );
        return this.transToViewObject(entities);
    }
    
    @Override
    public int save( V viewObject ) {
        if( viewObject == null ) {
            return 0;
        }
        return getMasterShardService().save(
                this.transToEntity(viewObject));
    }
    
    @Override
    public int add( V viewObject ) {
        if( viewObject == null ) {
            return 0;
        }
        return getMasterShardService()
                .add(this.transToEntity(viewObject));
    }
    
    protected S getShardService() {
        if( shardServices == null || shardServices.isEmpty() ) {
            throw new RuntimeException( "!! cannot get shard service of type " 
                    + this.shardServiceClass.getName() );
        }
        return this.roundAccess(shardServices);
    }
    
    protected <SS extends GenericShardService> 
        SS getShardService( Class<SS> shardServiceClass ) {

        return getShardServiceByName( shardServicesMap, 
            getShardServiceName( shardServiceClass ), 
            false );
    }
    
    protected <SS extends GenericShardService> 
        SS getShardService( String shardServiceName ) {

        return getShardServiceByName( shardServicesMap, 
            shardServiceName, 
            false );
    }
    
    protected S getMasterShardService() {
        if( masterShardService == null ) {
            throw new RuntimeException( 
                    "!! cannot get master shard service of type " 
                        + this.shardServiceClass.getName() );
        }
        return masterShardService;
    }
    
    protected <SS extends GenericShardService> 
        SS getMasterShardService( Class<SS> shardServiceClass ) {

        return getShardServiceByName( masterShardServicesMap, 
            getShardServiceName( shardServiceClass ), 
            true );
    }
    
    protected <SS extends GenericShardService> 
        SS getMasterShardService( String shardServiceName ) {
            
        return getShardServiceByName(masterShardServicesMap,
                shardServiceName,
                true);
    }
    
    private <SS extends GenericShardService> 
        SS getShardServiceByName( Map shardServicesMap,
                String shardServiceName, 
                boolean beMaster ) {
            
        List<SS> services = ( List<SS> )shardServicesMap.get( shardServiceName );
        
        if( services == null ) {
            services = extractShardServicesByName( shardServiceName, beMaster );
            shardServicesMap.put( shardServiceName, services );
        }
        
        return roundAccess( services );
    }

    @PostConstruct
    public void intForgeService() {
        extractShardServices();
    }
    
    private void extractShardServices() {
        this.shardServices = new ArrayList<>();
        for( ApplicationContext ctx : shardContext.getShardContexts() ) {
            Object service = ctx.getBean( getShardServiceName() );
            if( service != null ) {
                this.shardServices.add( ( S )service );
                if( MASTER_CTX_ID.equalsIgnoreCase( ctx.getId() ) ) {
                    masterShardService = ( S )service;
                    shardServices.remove( masterShardService );
                }
            }
        }
    }
    
    private List extractShardServicesByName( String serviceName, 
            boolean beMaster ) {
        List services = new ArrayList<>();
        for( ApplicationContext ctx : shardContext.getShardContexts() ) {
            if( beMaster ) {
                if( MASTER_CTX_ID.equalsIgnoreCase( ctx.getId() )) {
                    Object service = ctx.getBean( serviceName );
                    if( service != null ) {
                        services.add( service );
                    }
                    if( services.size() > 0 ) { // if got one master
                        return services;
                    }
                }
            }else {
                Object service = ctx.getBean( serviceName );
                if( service != null
                    && ( ! MASTER_CTX_ID.equalsIgnoreCase( ctx.getId() ) ) ) {
                    services.add( service );
                }
            }
        }
        return services;
    }

    protected String getShardServiceName() {
        return getShardServiceName( this.shardServiceClass );
    }
    
    protected String getShardServiceName( 
            Class<? extends GenericShardService> shardServiceClass ) {
        
        Matcher m = P_SHARD_SERVICE.matcher(
                shardServiceClass.getSimpleName());
        if( m.matches() ) {
            String name = m.group( 1 );
            return name.substring( 0, 1 ).toLowerCase() + name.substring( 1 );
        }
        throw new RuntimeException( shardServiceClass.getName()
                + " is not a valid shard service declaration,"
                + " it must be end with \"ShardService\"" );
    }
    
    @Deprecated
    private void matchMethodSet( Matcher m, Method method ) throws Exception {
        String serviceName = m.group(1);
        List services = new ArrayList();
        for( ApplicationContext ctx : shardContext.getShardContexts() ) {
            Object service = ctx.getBean( serviceName );
            if( service != null ) {
                services.add( service );
            }
        }
        if( services.isEmpty() ) {
            throw new RuntimeException( 
                "!! cannot find service: " + serviceName );
        }
        method.invoke( this, services );
    }
    
    @Deprecated
    private void matchMethodSet2( Matcher m, Method method ) throws Exception {
        String ctxId = m.group(1);
        String serviceName = m.group(2);
        ApplicationContext ctx = shardContext.getShardContextById( 
                MASTER.equalsIgnoreCase( ctxId ) ? MASTER_CTX_ID : ctxId );
        Object service = ctx.getBean(serviceName);
        if( service == null ) {
            throw new RuntimeException( 
                "!! cannot find service \"" + serviceName 
                        + "\" in context \"" + ctxId + "\"" );
        }
        method.invoke(this, service);
    }
    
    protected <S> S roundAccess( final List<S> services ) {
        if( services == null || services.isEmpty() ) {
            throw new IllegalArgumentException( "services are empty" );
        }
        
        if( services.size() < 2 ) {
            return services.get( 0 );
        }
        
        int h = services.hashCode();
        
        Integer i = roundNumberMap.get( h );
        
        if( i == null ) {
            i = 0;
            roundNumberMap.put( h, i );
        }
        
        if( i >= services.size() ) {
            i = 0;
        }
        
        S s = services.get( i );
        
        roundNumberMap.put( h, ++i );
        
        return s;
    }
    
    protected Logger getLogger() {
        return BaseUtils.getLogger(this.forgeServiceImplClass);
    }
        
    protected V newViewObject() {
        V obj = null;
        try {
            obj = this.viewObjectClass.newInstance();
        }catch( Exception ex ) {
            getLogger().error( "newViewObject exception with viewObjectClass="
                    + viewObjectClass, 
                    ex );
        }
        return obj;
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
    
    protected E transToEntity( V object ) {
        return transToEntity( object, ( Object[] )null );
    }
    
    protected List<E> transToEntity( List<V> objects ) {
        return transToEntity( objects, ( Object[] )null );
    }
    
    protected List<E> transToEntity( List<V> objects, Object... options ) {
        List<E> entities = new ArrayList<>();
        if( objects != null ) {
            for( V obj : objects ) {
                entities.add( transToEntity( obj, options ) );
            }
        }
        return entities;
    }
    
    protected List<V> transToViewObject( List<E> entities ) {
        return transToViewObject( entities, ( Object[] )null );
    }
    
    protected List<V> transToViewObject( List<E> entities, Object... options ) {
        List<V> viewObjects = new ArrayList<V>();
        if( entities != null ) {
            for( E entity : entities ) {
                viewObjects.add( transToViewObject( entity, options ) );
            }
        }
        return viewObjects;
    }
    
    protected E transToEntity( V viewObject, Object... options ) {
        
        if( viewObject == null ) {
            return null;
        }
        
        E entity = this.newEntityObject();
        
        BaseUtils.copyProperties( entity, viewObject );
        
        return entity;
    }
    
    protected V transToViewObject( E entity, Object... options ) {
        if( entity == null ) {
            return null;
        }
        
        V viewObject = this.newViewObject();
        
        BaseUtils.copyProperties( viewObject, entity );
        
        return viewObject;
    }

    private void getParamURlSet(Object clazz, Set<String> set, Set<String> names)
            throws IllegalArgumentException, IllegalAccessException {
        Field[] fil = clazz.getClass().getDeclaredFields();
        for (Field fi : fil) {
            fi.setAccessible(true);
           
            Object obj = null;
            obj = fi.get(clazz);
            if (obj == null ) {
                continue;
            }
            String name = fi.getName();
            if (obj instanceof String) {
                if( StringUtils.equalsIgnoreCase(obj.toString(),"NULL")){
                    continue;
                }
                if (names.contains(name)) {
                    set.add(obj.toString());
                }
            } else if (obj instanceof Collection) {
                List lit = new ArrayList<>((Collection) obj);
                Iterator it = lit.iterator();
                while (it.hasNext()) {
                    Object ob = it.next();
                    getParamURlSet(ob, set, names);
                }
            }  else if (obj instanceof BaseVO || obj instanceof BaseVO) {
                getParamURlSet(obj, set, names);
            }
        }
    }

    private void setParamURL(Object clazz, Map<String, String> map,
            Set<String> names) throws IllegalArgumentException,
            IllegalAccessException {
        Field[] fil = clazz.getClass().getDeclaredFields();
        for (Field fi : fil) {
            fi.setAccessible(true);
            String name = fi.getName();
            Object obj = null;
            obj = fi.get(clazz);
            if(obj == null){
                continue;
            }
            if (obj instanceof String) {
                if (names.contains(name)) {
                   String str = map.get(obj);
                   fi.set(clazz, str);
                }
            } else if (obj instanceof Collection) {
                List lit = new ArrayList<>((Collection) obj);
                Iterator it = lit.iterator();
                while (it.hasNext()) {
                    Object ob = it.next();
                    setParamURL(ob, map, names);
                }
            } else if (obj instanceof BaseVO || obj instanceof BaseVO) {
                setParamURL(obj, map, names);
            } 
        }
    }
    
    protected <BE extends BaseEntity> BE lookupBySid( List<BE> entities, Long sid ) {
        if( entities == null || entities.isEmpty() ) {
            return null;
        }
        
        for( BE be : entities ) {
            if( sid.equals( be.getSid() ) ) {
                return be;
            }
        }
        
        return null;
    }
    
}