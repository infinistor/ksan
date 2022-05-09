package com.pspace.ifs.ksan.gw.utils.disk;

public class Disk {
    private String id;
    private String mode;    // "ReadOnly", "ReadWrite"
    private String path;
    private String status;  // "Bad", "Disable", "Stop", "Weak", "Good"

    public Disk() {
        id = "";
        mode = "";
        path = "";
        status = "";
    }

    public Disk(String id, String mode, String path, String status) {
        this.id = id;
        this.mode = mode;
        this.path = path;
        this.status = status;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
