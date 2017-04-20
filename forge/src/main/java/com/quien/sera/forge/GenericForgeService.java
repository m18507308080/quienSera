/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.forge;

import com.quien.sera.common.entity.BaseEntity;
import com.quien.sera.common.vo.BaseVO;
import java.util.Collection;
import java.util.List;

/**
 * ForgeService基类
 * 
 * @param <V> BaseViewObject子视图类型
 * @param <E> BaseEntity子实体类型
 *
 * @author zengwei
 * @version 1.0
 */
public interface GenericForgeService<V extends BaseVO,
                                    E extends BaseEntity> {
    
    V getBySid(Long sid);
    
    List<V> getBySid(Collection<Long> sids);
    
    List<V> getBySid(Collection<Long> sids, boolean ignoreFlag);
    
    /**
     * insert or update the view object base on the field 'sid' has value or not.
     * 
     * @param viewObject 视图类
     * @return 
     */
    int save(V viewObject);
    
    int add(V viewObject);

}