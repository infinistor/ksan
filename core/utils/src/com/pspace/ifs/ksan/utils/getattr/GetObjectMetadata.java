package com.pspace.ifs.ksan.utils.getattr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author legesse
 */
class CommandParser{
    private List <String> args;
    private HashMap<String, String> map;
    private Set<String> flags;
    private String separater;
    
    public CommandParser(String arguments[], String separaterChar){
        args = new ArrayList<>();
        map = new HashMap<>();
        flags = new HashSet<>();
        args = Arrays.asList(arguments);
        separater=separaterChar;
        map();
    }
    
    private void map(){
        for (String arg : args){
            if (arg.startsWith("--")){
                if ((args.indexOf(arg) == (args.size() - 1)) || (args.get(args.indexOf(arg)+1).startsWith("--")))
                    flags.add(arg.replace("--", ""));
                else{
                    map.put(arg.replace("--", ""), args.get(args.indexOf(arg) + 1));
                }
            }
        }
    }
    public String getArgumentValue(String argumentName)
    {
        if(map.containsKey(argumentName))
            return map.get(argumentName);
        return null;
    }
    
    public long getArgumentValueLong(String argumentName){
         if(map.containsKey(argumentName))
             return Long.valueOf(map.get(argumentName));
         return 0;
    }
    
    public boolean getFlag(String flagName){
        if(flags.contains(flagName))
            return true;
        return false;
    }
    
}

public class GetObjectMetadata {
    private final Metadata mt;
    private String bucketName;
    private String objPath;
    private String message;
    private boolean isFile;
    
    public GetObjectMetadata(String [] args) throws Exception{
        if (parseArgs(args) != 0)
            throw new Exception(message);
        
        mt = new Metadata(bucketName, objPath);
        
        if (getObjects() != 1)
           throw new Exception(message); 
    }
    
    private int getObjects(){
        Metadata mt1;
        
        try {
            ObjManagerUtil obmu = new ObjManagerUtil();
            mt1 = obmu.getObjectWithPath(mt.getBucket(), mt.getPath());
            message = mt1.toString();
            return 1;
        } catch (ResourceNotFoundException ex) {
            message = displayNoInformation();
            return 0;
        } catch (Exception ex) {
            message = dispalyError();
        }
        return -1;
    }
    
    private int parseArgs(String []args){
        String type;
        String arg;
        
        for(String arg1 :args){
            arg =arg1.toLowerCase();
            if (arg.startsWith("--bucketname"))
               bucketName = arg.split("=")[1];
            if (arg.startsWith("--path"))
               objPath = arg.split("=")[1];
            if (arg.startsWith("--type")){
               type = arg.split("=")[1];
               isFile = type.equalsIgnoreCase(GWConstants.OBJECT_TYPE_FILE) || type.equalsIgnoreCase("f");
            }
        }
        
        if (bucketName.isEmpty() || objPath.isEmpty()){
            message ="Either bucket name or object path is not provided!";
            return -1;
        }
        return 0;
    }
    
    private String dispalyError(){
        return String.format("There is error when try to get object for bucket : %s path : %s", mt.getBucket(), mt.getPath());
    }
    
    private String displayNoInformation(){
        return String.format(" There is no Information assocated with bucket : %s path : %s exist!\n", mt.getBucket(), mt.getPath());
    }
    
    public String getMessage(){
        return message;
    }
}
