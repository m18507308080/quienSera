/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.common.entity;

import com.quien.sera.common.constant.Constants;
import java.io.Serializable;
import java.sql.Timestamp;

public abstract class BaseEntity implements Serializable {
    
    private Long sid;                                   // 记录流水号
    private Integer version = 0;                            // 乐观锁版本号
    private Long operator = Constants.SYS_OPERATOR;     // 记录操作人
    private Timestamp createdDatetime;                  // 记录创建日期
    private Timestamp updatedDatetime;                  // 记录修改日期
    private Boolean actived = true;         // 记录是否启用、激活或有效（默认为true）
    private Boolean deleted = false;        // 记录是否已被删除（默认为false）

    public Long getSid() {
        return sid;
    }

    public void setSid(Long sid) {
        this.sid = sid;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getOperator() {
        return operator;
    }

    public void setOperator(Long operator) {
        this.operator = operator;
    }

    public Timestamp getCreatedDatetime() {
        return createdDatetime;
    }

    public void setCreatedDatetime(Timestamp createdDatetime) {
        this.createdDatetime = createdDatetime;
    }

    public Timestamp getUpdatedDatetime() {
        return updatedDatetime;
    }

    public void setUpdatedDatetime(Timestamp updatedDatetime) {
        this.updatedDatetime = updatedDatetime;
    }

    public Boolean getActived() {
        return null == actived ? true : actived;
    }

    public void setActived(Boolean actived) {
        this.actived = actived;
    }

    public Boolean getDeleted() {
        return null == deleted ? false : deleted ;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}