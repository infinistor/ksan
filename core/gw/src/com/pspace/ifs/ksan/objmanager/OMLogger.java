/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.ifs.ksan.objmanager;

/*import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */

public class OMLogger {
    private final Logger logger;
    
    public OMLogger(String className) {
        logger = LoggerFactory.getLogger(className);
    }
    
    private void _log(OMLoggerLevel level, String msg, Throwable ex){
         if (null != level)
            switch (level) {
            case DEBUG:
                logger.debug(msg, ex);
                break;
            case ERROR:
                logger.error(msg, ex);
                break;
            case INFO:
                logger.info(msg, ex);
                break;
            case WARN:
                logger.warn(msg, ex);
                break;
            case TREACE:
                logger.trace(msg, ex);
                break;
            default:
                break;
        }
    }
    
    public void log(OMLoggerLevel level, String format, Object ...args){
        String msg = String.format(format, args);
        _log(level, msg, null);
    }
    
   public void log(OMLoggerLevel level, Throwable ex, String format, Object ...args){
        String msg = String.format(format, args);
        _log(level, msg, null);
    }
}
/*public class OMLogger {
    
    private Level logLevel;
    
    private OMLogger() {
    }
    
    public static OMLogger getInstance() {
        return OMLoggerHolder.INSTANCE;
    }
    
    private static class OMLoggerHolder {

        private static final OMLogger INSTANCE = new OMLogger();
    }
    
    private void setConsolProperties(Logger logger){
        try {
            logger.setLevel(logLevel);
            logger.addHandler(new ConsoleHandler());
            Handler fileHandler = new FileHandler("/var/log/infinistor/ObjManager/ObjManager.log", 2000, 5);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException ex) {
            Logger.getLogger(OMLogger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(OMLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void setXMLProperties(Logger logger){
        try {
            logger.setLevel(logLevel);
            logger.addHandler(new ConsoleHandler());
            Handler fileHandler = new FileHandler("/var/log/infinistor/ObjManager/ObjManager.conf", 2000, 5);
            fileHandler.setFormatter(new XMLFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException ex) {
            Logger.getLogger(OMLogger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(OMLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void setLogLevel(Level logLevel){
        this.logLevel = logLevel;
    }
    
    public void log(Object obj, Level level, String message, Throwable ex){
        String className = obj.getClass().getName();
        //if (logLevel.intValue() < level.intValue())
        //    return;
        Logger LOGGER = Logger.getLogger(className);
        LOGGER.setLevel(this.logLevel);
        LOGGER.log(level, message, ex);
    }
    
    public void log(Object obj, Level level, String message){
        String className = obj.getClass().getName();
        //if (logLevel.intValue() < level.intValue())
        //    return;
        Logger.getLogger(className).log(level, message);
    }
}*/
