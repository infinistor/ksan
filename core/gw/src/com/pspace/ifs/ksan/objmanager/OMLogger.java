/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
