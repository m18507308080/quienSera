/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.redis;

public final class RedisNameSpace {    

    /////////////////////////////////////////////
    // Name Space key enum defintion
    /////////////////////////////////////////////
    private static enum NS {

        // the root namespace
        QUIEN("quien")

        // the 1st level, tweemee namespace
        ,SERA( "sera" )

        // the 2nd level, business level
        ,USER("user")           /* 用户 */
        ,TEMP("temp")           /* 临时类 */
        ,LOCK("lock")           /* 锁 */
        ,CONFIG("config")       /* 配置 */
        ;
        
        private final String name;

        private NS( String name ) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
        
        //-- the 1st level ----------------------
        private static String _com_quien_sera( Object... objs ) {

            return _ns(QUIEN, SERA, objs );
        }

        //-- the 2nd level ----------------------
        private static String _com_quien_sera_user( Object... objs ) {

            return _com_quien_sera(USER, objs);
        }
        
        /////////////////////////////////////////////
        private static String _ns( Object... objs ) {
            StringBuilder sb = new StringBuilder();
            for( Object obj : objs ) {
                if( obj instanceof Object[] ) {
                    sb.append( _ns( (Object[])obj ) );
                }else {
                    sb.append( SEPARATOR ).append( obj.toString() );
                }
            }
            return sb.toString();
        }
    }
    
    ////////////////////////////////////////////////////////
    // Name Space Definition
    // Note: the ns definition must use the names 
    //       which defined in enum NS. 
    ////////////////////////////////////////////////////////
    
    public final static String SEPARATOR = "/";
    
    public final static String QUIEN_SERA_SID = "quien/sera/sid";

    public final static String QUIEN_SERA_NO = "quien/sera/no";
    
    public final static String QUIEN_SERA_TEMP = "quien/sera/temp";
    
    //-- user -----------------------------------------------
    public final static String QUIEN_SERA_USER = "quien/sera/user";

    //config
    public final static String QUIEN_SERA_REDIS_CONFIG = "quien/sera/config";

    //-- lock ---------------------------------------------
    public final static String LOCK = "quien/sera/lock";

    public final static String LOCK_GENERIC_INDEX = LOCK + "/generic_index_";

    public final static String LOCK_DEGREE_INSERT_UPDATE = LOCK + "/insert_update_";

    //redis服务器清单
    public final static String REDIS_SERVERS_CONFIG = QUIEN_SERA_REDIS_CONFIG  +"/redis_servers_config";

    // keep from initiation
    private RedisNameSpace(){}
    
}