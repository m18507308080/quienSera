/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.api.user;

import com.quien.sera.api.BaseController;
import com.quien.sera.common.vo.ResultVO;
import com.quien.sera.forge.user.IdolForgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller("idolController")
@RequestMapping(value = "/user")
public class IdolController  extends BaseController<IdolController> {

    @Autowired
    private IdolForgeService idolForgeService ;

    @RequestMapping(value = "/{sid}",method = { RequestMethod.GET })
    @ResponseBody
    public ResultVO get( @PathVariable String sid ) {
        return ResultVO.success( idolForgeService.getBySid(Long.valueOf(sid)) ) ;
    }

}