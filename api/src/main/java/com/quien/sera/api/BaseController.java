/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.api;

import com.quien.sera.common.exception.BaseException;
import com.quien.sera.common.constant.ResultCode;
import com.quien.sera.common.util.BaseUtils;
import com.quien.sera.common.vo.ResultVO;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Enumeration;
import java.util.Locale;

public abstract class BaseController<T extends BaseController<T>> {

    private final Class<T> controllerClass;

    @Autowired
    private MessageSource messageSource;

    public BaseController() {

        Type[] types = ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments();
        this.controllerClass = (Class<T>) types[0];

    }

    public String getMessage(String code,
                             Object[] args,
                             String defaultMessage) {
        return messageSource.getMessage(code,
                args,
                defaultMessage,
                Locale.CHINA);
    }

    public String getMessage(String code) {
        return this.getMessage(code, null);
    }

    public String getMessage(String code, Object[] args) {
        return messageSource.getMessage(code, args, Locale.CHINA);
    }

    @ExceptionHandler
    public void exceptionHandler(Throwable ex, HttpServletRequest request, HttpServletResponse response) {

        if (ex instanceof IOException) {
            String causeMsg = ExceptionUtils.getRootCauseMessage(ex);
            if (StringUtils.containsIgnoreCase(causeMsg, "断开的管道")
                    || StringUtils.containsIgnoreCase(causeMsg, "Broken pipe")) {
                response(request, response, null);
                return;
            }
        }

        String code = ResultCode.ERROR;
        String msg = null;

        if (ex instanceof BaseException) {
            code = ((BaseException) ex).getResultCode();
            msg = getMessage(((BaseException) ex).getMsgCode(),
                    ((BaseException) ex).getMsgOptions());

        } else {
            String exClassName = ex.getClass().getName();
            msg = getMessage(exClassName);
            if (exClassName.equalsIgnoreCase(msg)
                    || StringUtils.isEmpty(msg)) {
                msg = getMessage("system.error");
                logRequest(request, ex);
            }
        }

        response(request, response, new ResultVO(code, msg));
    }

    protected void response(HttpServletRequest request, HttpServletResponse response, Object vo) {

        if (vo == null) {
            return;
        }

        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        PrintWriter w = null;
        try {
            w = response.getWriter();
            w.print(BaseUtils.toJson(vo));
            w.flush();
        } catch (Exception ex) {

        } finally {
            try {
                if (w != null) {
                    w.close();
                }
            } catch (Exception ex) {

            }
        }
    }

    @RequestMapping("**")
    public
    @ResponseBody
    Object notFoundHandler(HttpServletRequest request) {

        getLogger().warn("== request not supported =============="
                + getRequestInfo(request));

        return new ResultVO(ResultCode.REQUEST_NOT_SUPPORTED,
                getMessage("system.request.not.supported"));

    }

    private String getErrorFieldName(ObjectError objError) {
        String code = objError.getCodes()[0];
        int idx = code.lastIndexOf(".");
        return idx < 0 ? code : code.substring(idx + 1);
    }

    private String getErrorMessage(ObjectError objError) {
        if (objError.getCodes() != null) {
            for (String code : objError.getCodes()) {
                String msg = getMessage(code, objError.getArguments());
                if (StringUtils.isNotEmpty(msg)
                        && !msg.equals(code)) {
                    return msg;
                }
            }
        }
        return objError.getDefaultMessage();
    }

    protected Logger getLogger() {
        return BaseUtils.getLogger(this.controllerClass);
    }

    protected void logRequest(HttpServletRequest request) {
        getLogger().warn(getRequestInfo(request));
    }

    protected void logRequest(HttpServletRequest request, Throwable ex) {
        getLogger().error(getRequestInfo(request), ex);
        if (ex.getCause() != null) {
            getLogger().error("caused by >>>", ex.getCause());
        }
    }

    @SuppressWarnings("rawtypes")
    protected String getRequestInfo(HttpServletRequest request) {

        StringBuilder headers = new StringBuilder();

        Enumeration headerNames = request.getHeaderNames();
        if (headerNames.hasMoreElements()) {
            headers.append("\r\nheaders: {");
            while (headerNames.hasMoreElements()) {
                String name = (String) headerNames.nextElement();
                headers.append("\r\n")
                        .append(name)
                        .append("=")
                        .append(request.getHeader(name));
            }
            headers.append("\r\n}");
        }

        StringBuilder parameters = new StringBuilder();
        Enumeration parameterNames = request.getParameterNames();
        if (parameterNames.hasMoreElements()) {
            parameters.append("\r\nparameters: {");
            while (parameterNames.hasMoreElements()) {
                String parameter = (String) parameterNames.nextElement();
                parameters.append("\r\n")
                        .append(parameter)
                        .append("=")
                        .append(request.getParameter(parameter));
            }
            parameters.append("\r\n}");
        }

        StringBuilder content = new StringBuilder();
        if (!"multipart/form-data".equalsIgnoreCase(
                request.getContentType())) {
            try {
                BufferedReader r = request.getReader();
                String line = null;
                while ((line = r.readLine()) != null) {
                    content.append(line);
                }
            } catch (Exception ex) {

            }
        }


        return "\r\n>>> request >>>>>>"
                + headers.toString()
                + "\nmethod: " + request.getMethod()
                + "\ncontentType: " + request.getContentType()
                + "\npathInfo: " + request.getPathInfo()
                + "\npathTranslated: " + request.getPathTranslated()
                + "\nremoteAddr: " + request.getRemoteAddr()
                + "\nremoteHost: " + request.getRemoteHost()
                + "\nrequestURI: " + request.getRequestURI()
                + "\nservletPath: " + request.getServletPath()
                + parameters.toString()
                + "\ncontent ---\n"
                + content.toString()
                + "\n-----\n";
    }


    protected boolean isHttps(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = "http";
        }
        return StringUtils.startsWithIgnoreCase(scheme, "https");
    }

}