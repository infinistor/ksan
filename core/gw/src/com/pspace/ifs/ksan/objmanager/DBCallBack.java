/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.ObjManger;

import java.time.LocalDate;

/**
 *
 * @author legesse
 */
public interface DBCallBack {
    public void call(String key, String etag, String lastModified, long size, String versionId, String pdiskid, String rdiskid, boolean lastVersion);
}
