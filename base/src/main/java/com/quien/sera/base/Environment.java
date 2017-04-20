/** 
  *  Copyright 2016-2026 Software, Inc. All rights reserved.
  *  
  * @version  0.9, 03/17/17 
  * @author   limumu
  * @since    JDK1.8
  */
package com.quien.sera.base;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import com.quien.sera.common.util.BaseUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Environment {

    private static final Logger logger = org.slf4j.LoggerFactory
            .getLogger(Environment.class);

    private static final String LOGBACK_FILE_NAME = "logback.xml";

    public static final String ENV;
    public static final String LOG_ENV;
    public static final String VERIFY_SECRET;

    private static final String DEFAULT_FILE_NAME = "env.properties";
    private static final String DEFAULT_CCP_FILE_NAME = "default.properties";

    private static final String ENV_IP = "IP";

    private static Path ctxPath;

    private final static Properties properties;

    private final static Map<String, String> propConfigMap = new HashMap<String, String>();
    
    public static String LOCAL_IP ;

    static {
        // init context path
        contextPath();

        String config = retrieveEnvValue("SYS_ENV", "IP");

        if (StringUtils.equals(config, ENV_IP)) {
            List<String> ip = getLocalIP();
            ENV = getNetConfig(ip);
        } else {
            ENV = config;
        }
        logger.warn("Environment.ENV = " + ENV);

        LOG_ENV = retrieveEnvValue("LOG_ENV", ENV);
        logger.warn("Environment.LOG_ENV = " + LOG_ENV);

        VERIFY_SECRET = retrieveEnvValue("VERIFY_SECRET", "");

        // load properties
        properties = Environment.loadProperties(DEFAULT_CCP_FILE_NAME);

    }

    public static Path contextPath() {
        if (ctxPath == null) {
            String path = Thread.currentThread().getContextClassLoader()
                    .getResource("").getPath();
            String osName = System.getProperty("os.name");
            logger.warn("本机操作系统为:" + osName);
            if (StringUtils.startsWith(osName, "Windows")) {
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
            }
            ctxPath = Paths.get(path);
        }
        return ctxPath;
    }

    public static String loadContent(String name) throws IOException {
        if (StringUtils.isEmpty(name)) {
            return null;
        }

        InputStream ii = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(name);
        try {
            StringBuilder sbd = new StringBuilder();
            if (ii != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(ii));
                String line = null;
                while ((line = r.readLine()) != null) {
                    if (sbd.length() > 0) {
                        sbd.append("\n");
                    }
                    sbd.append(line);
                }
            }
            return sbd.toString();
        } finally {
            try {
                ii.close();
            } catch (Exception ex) {

            }
        }
    }

    public static Properties loadProperties(String name) {
        Properties p = new Properties();
        try {
            p.load(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(name));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load " + name, ex);
        }
        return p;
    }

    private static String retrieveEnvValue(String name, String defaultValue) {
        String env = null;

        // 1st: check in jvm app arguments
        env = System.getProperty(name);

        // 2nd: check in environment variables
        if (env == null || "".equals(env)) {
            env = System.getenv().get(name);
        }

        // 3rd: check in the according file
        if (env == null || "".equals(env)) {
            try {
                env = loadContent(name);
            } catch (Exception ex) {
                logger.error("!! Cannot get SYS_ENV's value", ex);
            }
        }

        // default:
        if (env == null || "".equals(env)) {

            env = defaultValue;
        }

        return env;
    }

    private static void resetLogContext() {
        LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory
                .getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);

            String path = Environment.LOG_ENV + "/" + LOGBACK_FILE_NAME;

            logger.warn("reset log context with file: " + path);

            context.reset();

            configurator.doConfigure(Thread.currentThread()
                    .getContextClassLoader().getResourceAsStream(path));

            logger.warn("reset log context completed!");

        } catch (Exception ex) {
            logger.error("reset log context exception occurred!!", ex);
            throw new RuntimeException(ex);
        }

        if (logger.isInfoEnabled()) {
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        }

    }

    private static String getNetConfig(List<String> ip) {
        String config = "";
        try {
            config = getEnvName(ip);
        } catch (Exception e) {
            logger.error("环境初始化失败");
        }
        return config;

    }

    private static List<String> getUnixLocalIp() throws SocketException {

        List<String> res = new ArrayList<String>();
        Enumeration<NetworkInterface> netInterfaces;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> nii = ni.getInetAddresses();
                while (nii.hasMoreElements()) {
                    ip = nii.nextElement();
                    if (ip.getHostAddress().indexOf(":") == -1) {
                        if (!StringUtils.equals(ip.getHostAddress(),
                                "127.0.0.1")
                                && !StringUtils.equals("192.168.0.1",
                                        ip.getHostAddress())) {
                            res.add(ip.getHostAddress());
                        }

                    }
                }
            }
        } catch (SocketException e) {
            logger.info("**********调用BaseEnvironment.getUnixLocalIp"
                    + "出错*********", e);
        }
        return res;
    }

    public static List<String> getLocalIP() {
        String osName = System.getProperty("os.name");
        logger.warn("本机操作系统为:" + osName);
        List<String> ipList = new ArrayList<String>();
        if (StringUtils.startsWith(osName, "Windows")) {
            try {
                InetAddress inet = InetAddress.getLocalHost();
                ipList.add(inet.getHostAddress());
            } catch (UnknownHostException e) {
                logger.info("**********调用BaseEnvironment.getLocalIP" + "出错*********", e);
            }
        } else {
            try {
                ipList.addAll(getUnixLocalIp());
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return ipList;
    }

    private static String getEnvName(List<String> ipList) throws Exception {
        logger.warn("Environment: ip = " + BaseUtils.toJson(ipList));
        String env = "";
        StringBuffer log = new StringBuffer();
        InputStream inStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(DEFAULT_FILE_NAME);
        Map<String, String> map = new LinkedHashMap<String, String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(inStream));
        String line = null;
        while ((line = r.readLine()) != null) {
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            String[] str = StringUtils.trim(line).split("=");
            map.put(str[0], str[1]);
        }
        for (String ip : ipList) {
            String name = ip.trim();
            env = System.getProperty(name);
            if (env == null || "".equals(env)) {
                env = System.getenv().get(name);
            }
            if (env == null || "".equals(env)) {

                for (String key : map.keySet()) {
                    log.append(key);
                    log.append(map.get(key) + "\r\n");
                    String[] pats = StringUtils.split(key, ".");
                    String[] ips = StringUtils.split(name, ".");
                    int count = 0;
                    for (int i = 0; i < pats.length; i++) {
                        String reg = pats[i];
                        if (StringUtils.equals(reg, ips[i])) {
                            count++;
                        } else {
                            reg = StringUtils.replace(reg, "*", "\\d+");
                            reg = StringUtils.replace(reg, "?", "\\d?");
                            Pattern pat = Pattern.compile("^" + reg + "$");
                            Matcher mat = pat.matcher(ips[i]);
                            if ( mat.matches() ) {
                                count++;
                            }
                        }

                    }
                    if (count == 4) {
                        env = map.get(key.toString());
                        LOCAL_IP = ip;
                        log.append("当前本机IP为:").append(ip).append(",匹配的配置文件项为:")
                                .append(key.toString()).append(",环境为:")
                                .append(env);
                        break;
                    }
                }

            }
        }
        logger.warn("*****************************************\r\n");
        logger.warn(log.toString());
        logger.warn("*****************************************\r\n");
        return env;
    }

    private Environment() {
    }

}