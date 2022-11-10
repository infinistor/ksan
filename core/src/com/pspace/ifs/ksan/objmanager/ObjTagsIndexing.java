package com.pspace.ifs.ksan.objmanager;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author legesse
 */
public class ObjTagsIndexing {
    private DataRepository dbm;
    private final Objects objectMGT;
    private final BucketManager bucketMGT;
 
    public ObjTagsIndexing(BucketManager bucketMGT,  Objects objectMGT){
        this.bucketMGT = bucketMGT;
        this.objectMGT = objectMGT;
    }
    
    public void enableIndexing(String bucketName) throws SQLException, ResourceNotFoundException{
       bucketMGT.updateBucketTagsIndexing(bucketName, true);
    }
    
    public void disableIndexing(String bucketName) throws SQLException, ResourceNotFoundException{
        bucketMGT.updateBucketTagsIndexing(bucketName, false);
    }
    
    public boolean isIndexingEnabled(String bucketName) throws ResourceNotFoundException, SQLException{
        return bucketMGT.getBucket(bucketName).isObjectTagIndexEnabled();
    }
    
    public List<Metadata> getObjectWithTags(String bucketName, String tagList, int maxObjects) throws SQLException{
        return objectMGT.listObjectWithTags(bucketName, tagList, maxObjects);
    }
}
