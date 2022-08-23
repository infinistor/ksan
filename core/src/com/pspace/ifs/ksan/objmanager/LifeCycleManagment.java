package com.pspace.ifs.ksan.objmanager;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author legesse
 */
public class LifeCycleManagment {
    private DataRepository dbm;
    private static Logger logger;
    
    public LifeCycleManagment(DataRepository dbm){
        this.dbm = dbm;
        logger =  LoggerFactory.getLogger(ObjManagerUtil.class);
    }
    
    public void putLifeCycleEvent(LifeCycle lifecycle) throws SQLException{ 
        dbm.insertLifeCycle(lifecycle);
    }
    
    public void putLifeCycleEvents(List<LifeCycle> lifecycleList) throws SQLException{
        for (LifeCycle lc : lifecycleList){
            putLifeCycleEvent(lc);
        }
    }
    
    public int removeLifeCycleEvent(LifeCycle lifecycle) throws SQLException{
        return dbm.deleteLifeCycle(lifecycle);
    }
    
    // get life cycle with bucketname, key and versionId
    public LifeCycle getLifeCycleEvent(LifeCycle lifecycle) throws SQLException{
        return dbm.selectLifeCycle(lifecycle);
    }
    
    // get life cycle with uploadid
    public LifeCycle getLifecyCleEventByUploadId(LifeCycle lifecycle) throws SQLException{
        return dbm.selectLifeCycle(lifecycle);
    }
    
    public List<LifeCycle> getLifeCycleEventList() throws SQLException{
        return dbm.selectAllLifeCycle();
    }
}
