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
package com.pspace.ifs.ksan.utility;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.bson.Transformer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author legesse
 */
public class UtilDataStorage {
    private String diskPoolPath;
    private String allListPath;
    private Document dskPoolDoc;
    private Document allDoc;
    
    private final String diskPoolTag="DISKPOOL";
    private final String serverTag="SERVER";
    private final String diskTag="DISK";
    private final String poolListTag="DISKPOOLLIST";
    private final String serverListTag="SERVERSLIST";
    
    public UtilDataStorage() throws ParserConfigurationException, SAXException{
    
        allListPath = "/usr/local/ksan/etc/serverList.xml";
        diskPoolPath = "/usr/local/ksan/etc/dskpool.xml";
        try{
            File dskPool = new File(diskPoolPath); 
            File allList = new File(allListPath);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
            
            DocumentBuilder alldb = dbf.newDocumentBuilder(); 
            allDoc = alldb.parse(allList);
            allDoc.getDocumentElement().normalize();
            
            DocumentBuilder dskPooldb = dbf.newDocumentBuilder(); 
            dskPoolDoc = dskPooldb.parse(dskPool);
            dskPoolDoc.getDocumentElement().normalize(); 
        } catch (IOException e){
            Element root;
            
            allDoc = new DocumentImpl(false);
            root = allDoc.createElement(serverListTag);
            allDoc.appendChild(root);
            saveFile(allDoc, allListPath);
            
            dskPoolDoc = new DocumentImpl(false);
            root = dskPoolDoc.createElement(poolListTag);
            dskPoolDoc.appendChild(root);
            saveFile(dskPoolDoc, diskPoolPath);
        }
        
    }
    
    private boolean isExist(Document list, String tag, String attr, String val){
        NodeList serverList;
        Element srv;
        Node node;
        String value;
        
        if (list == null)
            return false;
        
        serverList = list.getElementsByTagName(tag);
        for(int idx = 0; idx < serverList.getLength(); idx++){
            node = serverList.item(idx);  
            srv = (Element) node;  
            value = srv.getAttribute(attr);
            return value.equals(val);
        }
        return false;
    }
    
    private boolean isServerExist(String IPAddr){
        return isExist(allDoc, serverTag, "ip", IPAddr); 
    }
    
    private boolean isServerExistInPool(String IPAddr){
        return isExist(dskPoolDoc, serverTag, "ip", IPAddr); 
    }
    
    private boolean isDiskExist(String IPAddr){
        return isExist(allDoc, diskTag, "ip", IPAddr); 
    }
    
    private boolean isDiskExistInPool(String IPAddr){
        return isExist(dskPoolDoc, diskTag, "ip", IPAddr); 
    }
    
    private int saveFile(Document list, String xmlPath){
        try {
            //list.normalizeDocument();
            list.adoptNode(list.createTextNode("\n"));
            Source source = new DOMSource(list);
            File xmlFile = new File(xmlPath);
            StreamResult result = new StreamResult(new OutputStreamWriter(new FileOutputStream(xmlFile), "ISO-8859-1"));
            javax.xml.transform.Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
        } catch(Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }
    
    private boolean idExist(Document doc, String tag, String Id){
       NodeList nodeList;
        Element elm;
        Node node;
        String value;
        
        if (doc == null)
            return false;
        
        nodeList = doc.getElementsByTagName(tag);
        for(int idx = 0; idx < nodeList.getLength(); idx++){
            node = nodeList.item(idx);  
            elm = (Element) node;  
            value = elm.getAttribute("id");
            return value.equals(Id);
        }
        return false; 
    }
    
    private String getNewId(Document doc, String tag){
        int retry =100;
        int leftLimit = 48; // letter '0'
        int rightLimit = 122; // letter 'z'
        String id;
        
        Random rand = new Random();
        do {
            id = rand.ints(leftLimit, rightLimit)
                    .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                    .limit(15)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
            
            if (--retry < 0){
                id = "";
                break;    
            }
        } 
        while(idExist(doc, tag, id));
        
        return id;
    }
    
    public int addServer(String IPaddr, String status){
        String Id;
        Element root;
        
        if (isServerExist(IPaddr)){
            
            return -1;
        }
        
        root = allDoc.getDocumentElement();//getElementsByTagName(serverListTag); 
        if (root == null){
            return -1;
        }
        
        Id = getNewId(allDoc, serverTag);
        if (Id.isEmpty())
            return -1;
        
        
        Element svr = allDoc.createElement(serverTag);
        svr.setAttribute("id", Id);
        svr.setAttribute("ip", IPaddr);
        svr.setAttribute("status", status);
        root.appendChild(allDoc.createTextNode("\n"));
        root.appendChild(svr);
        root.appendChild(allDoc.createTextNode("\n"));
        //allDoc.appendChild(root);
        return saveFile(allDoc, allListPath);
    }
    
    public Element getServer(String ip){
        Element svr;
        Node node;
        String IPaddr;
        
        NodeList nList = allDoc.getElementsByTagName(serverTag);
        for(int idx = 0; idx < nList.getLength(); idx++){
            node = nList.item(idx);
            svr = (Element)node;
            IPaddr = svr.getAttribute("ip");
            if (IPaddr.equals(ip))
                return svr;
        }
        return null;
    }
    
    public int removeServer(String ip){
        Element root;
        Element svr;
        int ret;
         
        svr = getServer(ip);
        if (svr == null)
            return -1;
        
        root = allDoc.getDocumentElement(); 
        if (root == null)
            return -1;
            
        root.removeChild(svr);
        
        ret = saveFile(allDoc, allListPath);
        if (ret == 0)
            removeServerFromDiskPool(ip);
        return ret; 
    }
    
    private boolean isDiskExist(Element svr, String mpath){
        NodeList nList;
        Element dsk;
        Node node;
        
        if (svr == null)
            return false;
        
        nList = svr.getElementsByTagNameNS(serverTag, diskTag);//getElementsByTagName(diskTag);
        for(int idx =0; idx < nList.getLength(); idx++){
           node = nList.item(idx);
           if (((Element)node).getElementsByTagName("path").item(0).getTextContent().equals(mpath))
               return true;
        }
        return false;
    }
    
    public int addDisk(String IP, String mpath, String status){
        Element svr;
        String Id;
        
        svr = getServer(IP);
        if (svr == null)
            return -1;
        
        if (isDiskExist(svr, mpath))
            return -1;
        
        Id = getNewId(allDoc, diskTag);
        if (Id.isEmpty())
            return -1;
        
        Element dsk = allDoc.createElement(diskTag);
        dsk.setAttribute("id", Id);
        dsk.setAttribute("path", mpath);
        dsk.setAttribute("mode", "READWRITE");
        dsk.setAttribute("status", status);
        svr.appendChild(allDoc.createTextNode("\n"));
        svr.appendChild(dsk);
        svr.appendChild(allDoc.createTextNode("\n"));
        return saveFile(allDoc, allListPath);
    }
    
    public Element getDisk(String diskId){
        NodeList nList;
        Element dsk;
        Node node;
        
        nList = allDoc.getElementsByTagName(diskTag);
        for(int idx =0; idx < nList.getLength(); idx++){
           node = nList.item(idx);
           dsk = (Element)node;
           if (dsk.getAttribute("id").equals(diskId))
               return dsk;
        }
        return null;
    }
    
    public int removeDisk(String diskId){
        Element dsk;
        int ret;
        
        dsk = getDisk(diskId);
        if (dsk == null)
            return -1;
        
        allDoc.removeChild(dsk);
        removeDiskFromDiskPoolByDiskId(diskId);
        return 0;
    }
    
    public Element getDiskPool(String dPname, String dskPoolId){
        NodeList nList;
        Element dskPool;
        Node node;
        
        if (dskPoolDoc == null)
            return null;
        
        nList = dskPoolDoc.getElementsByTagName(diskPoolTag);
        for(int idx =0; idx < nList.getLength(); idx++){
           node = nList.item(idx);
           dskPool = (Element)node;
           if (!dPname.isEmpty()){
               if (dskPool.getAttribute("name").equals(dPname))
                   return dskPool;
           }
           
           if (!dskPoolId.isEmpty()){
              if (dskPool.getAttribute("id").equals(dskPoolId))
                   return dskPool; 
           }
        }
        return null;
    }
    
    public String createDiskPool(String dPname){
        Element dskPool;
        Element root;
        String id;
        int ret;
         
        dskPool = getDiskPool(dPname, "");
        if (dskPool != null)
            return null;
        
        root = dskPoolDoc.getDocumentElement();
        if (root == null)
            return null;
       
        id = getNewId(dskPoolDoc, diskPoolTag);
        if (id.isEmpty())
            return null;
        
        dskPool = dskPoolDoc.createElement(diskPoolTag);
        dskPool.setAttribute("name", dPname);
        dskPool.setAttribute("id", id);
        //root.appendChild(dskPoolDoc.createTextNode("\n"));
        root.appendChild(dskPool);
        root.appendChild(dskPoolDoc.createTextNode("\n"));
  
        ret = saveFile(dskPoolDoc, diskPoolPath);
        if (ret == 0)
            return id;
        
        return null;
    }
    
    public int removeDiskPool(String diskPoolId){
        Element root;
        Element dskPool;
         
        root = dskPoolDoc.getDocumentElement();
        if (root == null)
            return -1;
        
        dskPool = getDiskPool("", diskPoolId);
        if (dskPool == null)
            return -1;
        
        root.removeChild(dskPool);
        return saveFile(dskPoolDoc, diskPoolPath);
    }
    
    public boolean isDiskExistInPool(String diskId, String dskPoolId){
        Element dskPool;
        Element dsk;
        NodeList nList;
        Node node;
        
        dskPool = getDiskPool("", dskPoolId);
        if (dskPool == null)
            return false;
        
        nList = dskPoolDoc.getElementsByTagName(diskTag);
        for(int idx =0; idx < nList.getLength(); idx++){
           node = nList.item(idx);
           dsk = (Element)node;
           if (dsk.getAttribute("id").equals(diskId))
               return true;
        }
        return false;
    }
    
    private Element getServerFromDiskPool(String IP){
        NodeList nList;
        Node node;
        Element svr;
        
        nList = dskPoolDoc.getElementsByTagName(serverTag);
        for(int idx =0; idx < nList.getLength(); idx++){
            node = nList.item(idx);
            svr = (Element)node;
            if (svr.getAttribute("ip").equals(IP))
               return svr;
        }
        return null;
    }
    
    private Element getDiskFromDiskPoolWithDiskId(Element svr, String diskId){
        NodeList nListDSK;
        Element dsk;
        
        nListDSK = svr.getElementsByTagName(diskTag);
        for(int idx = 0; idx < nListDSK.getLength(); idx++){
            dsk = (Element)nListDSK.item(idx);
            if (dsk.getAttribute("id").equals(diskId))
                return dsk;
        }
        return null;
    }
    
    private Element getServerFromDiskPoolWithDiskId(String diskId){
        NodeList nList;
        Node node;
        Element svr;
        
        nList = dskPoolDoc.getElementsByTagName(serverTag);
        for(int idx =0; idx < nList.getLength(); idx++){
            node = nList.item(idx);
            svr = (Element)node;
            if (getDiskFromDiskPoolWithDiskId(svr, diskId) != null)
                return svr;
        }
        return null;
    }
    
    public int addDiskToDiskPool(String diskId, String dskPoolId){
        Element dskPool;
        Element dsk;
        Element svr;
        Element root;
        Element elm;
        
        dskPool = getDiskPool("", dskPoolId);
        if (dskPool == null)
            return -1;
        
        dsk = getDisk(diskId);
        if (dsk == null)
            return -2;
        
        svr = (Element)dsk.getParentNode();
        if (svr == null)
            return -22;
        
        root = getServerFromDiskPool(svr.getAttribute("ip"));
        if (root == null){
            root = dskPoolDoc.createElement(serverTag);
            root.setAttribute("id", svr.getAttribute("id"));
            root.setAttribute("ip", svr.getAttribute("ip"));
            root.setAttribute("status", svr.getAttribute("status"));
            root.appendChild(dskPoolDoc.createTextNode("\n"));
            //dskPoolDoc.appendChild(root);
        }
        
        elm = dskPoolDoc.createElement(diskTag);
        elm.setAttribute("id", dsk.getAttribute("id"));
        elm.setAttribute("path", dsk.getAttribute("path"));
        elm.setAttribute("mode", dsk.getAttribute("mode"));
        elm.setAttribute("status", dsk.getAttribute("status"));
        root.appendChild(elm);
        dskPool.appendChild(dskPoolDoc.createTextNode("\n"));
        dskPool.appendChild(root);
        dskPool.appendChild(dskPoolDoc.createTextNode("\n"));
        //dskPoolDoc.appendChild(root);
        return saveFile(dskPoolDoc, diskPoolPath);
    }
    
    public int removeDiskFromPool(String diskId, String dskPoolId){
        Element dskPool;
        Element dsk;
        Element svr;
        
        if (!isDiskExistInPool(diskId, dskPoolId))
            return -1;
        
        dsk = getDisk(diskId);
        if (dsk == null)
            return -2;
        
        dskPool = getDiskPool("", dskPoolId);
        if (dskPool == null){
            return -1;
        }
        
        svr = getServerFromDiskPoolWithDiskId(diskId);
        if (svr == null){
            return -1;
        }
        
        dsk = getDiskFromDiskPoolWithDiskId(svr, diskId);
        if (dsk == null)
            return -1;
        
        svr.removeChild(dsk);
        return saveFile(dskPoolDoc, diskPoolPath);
    }
    
    private int removeDiskFromDiskPoolByDiskId(String diskId){
        Element dsk;
        Element svr;
        
        svr = getServerFromDiskPoolWithDiskId(diskId);
        if (svr == null){
            return -1;
        }
        
        dsk = getDiskFromDiskPoolWithDiskId(svr, diskId);
        if (dsk == null)
            return -1;
        
        svr.removeChild(dsk);
        return saveFile(dskPoolDoc, diskPoolPath);
    }
    
    private int removeServerFromDiskPool(String Ip){
        Element dskPool;
        Element svr;
        
        svr = getServerFromDiskPool(Ip);
        if (svr == null){
            return -1;
        }
        
        dskPool = (Element)svr.getParentNode();
        if (dskPool == null)
            return -1;
        
        dskPool.removeChild(svr);
        return saveFile(dskPoolDoc, diskPoolPath);
    }
    
    public int updateDiskStatus(String diskId, String status){
        Element dsk;
        Element dskp;
        Element svr;
        int ret;
        
        svr = getServerFromDiskPoolWithDiskId(diskId);
        if (svr == null){
            return -1;
        }
        
        dsk = getDisk(diskId);
        if (dsk == null)
            return -2;
        
        dskp = this.getDiskFromDiskPoolWithDiskId(svr, diskId);
        if (dskp == null)
            return -2;
        
        dsk.setAttribute("status", status);
        dskp.setAttribute("status", status);
        ret = saveFile(dskPoolDoc, diskPoolPath);
        if (ret == 0)
            saveFile(allDoc, allListPath);
        return ret;
    }
    
    public int updateServerStatus(String Ip, String status){
        Element dsk;
        Element svrp;
        Element svr;
        int ret;
        
        svr = this.getServer(Ip);
        if (svr == null){
            return -1;
        }
        
        svrp = this.getServerFromDiskPool(Ip);
        if (svrp == null)
            return -2;
        
        svr.setAttribute("status", status);
        svrp.setAttribute("status", status);
        ret = saveFile(dskPoolDoc, diskPoolPath);
        if (ret == 0)
            saveFile(allDoc, allListPath);
        return ret;
    }
    
    private int listPoolDisk(Element dpool){
        NodeList nList;
        Element elm;
        
        nList = dpool.getElementsByTagName(diskTag);
        if (nList.getLength() == 0)
            return 0;
        
        System.out.format("----------------------------------------------------------\n");
        System.out.format("   ID              Path              Status\n");
        for(int idx =0; idx < nList.getLength(); idx++){
           elm = (Element)nList.item(idx); 
           System.out.format("%s   %s            %s\n", elm.getAttribute("id"), elm.getAttribute("path"), elm.getAttribute("status"));
        }
        System.out.format("----------------------------------------------------------\n");
        return 0;
        
    }
    
    public int list(String tag){
        NodeList nList;
        Element elm;
        
        if (tag.equals(diskPoolTag)){
            nList = dskPoolDoc.getElementsByTagName(diskPoolTag);
            System.out.format("   ID              name \n");
        } else{
            nList = allDoc.getElementsByTagName(tag);
            System.out.format("   ID              %s              Status\n", tag.equals(serverTag) ? "IP" : "Path");
        }
        
        System.out.println(">> dskPoolDoc " + dskPoolDoc.getElementsByTagName("DISKPOOL").getLength());
        System.out.println(">> nList " + nList.toString());
        for(int idx =0; idx < nList.getLength(); idx++){
           elm = (Element)nList.item(idx);
           if (tag.equals(serverTag))
               System.out.format("%s   %s    %s\n", elm.getAttribute("id"), elm.getAttribute("ip"), elm.getAttribute("status"));
           else if (tag.equals(diskTag))
               System.out.format("%s   %s             %s\n", elm.getAttribute("id"), elm.getAttribute("path"), elm.getAttribute("status"));
           else if (tag.equals(diskPoolTag)){
               System.out.format("%s   %s \n", elm.getAttribute("id"), elm.getAttribute("name"));
                listPoolDisk(elm);
           }
        }
        return 0;
    }
}
