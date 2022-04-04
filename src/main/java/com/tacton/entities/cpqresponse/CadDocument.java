package com.tacton.entities.cpqresponse;

import javax.persistence.Transient;

public class CadDocument {

    private String type;
    private String file;
    @Transient
    private String name;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String fileUrl) {
        this.file = fileUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CadDocument{" +
                "type='" + type + '\'' +
                ", fileUrl='" + file + '\'' +
                '}';
    }
}
