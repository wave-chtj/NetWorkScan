package com.goldze.main.entity;

public class ModelTypeEntity {
    private String id;
    private String typeName;
    private String typeCommand;

    public ModelTypeEntity(String id, String typeName, String typeCommand) {
        this.id = id;
        this.typeName = typeName;
        this.typeCommand = typeCommand;
    }

    public ModelTypeEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeCommand() {
        return typeCommand;
    }

    public void setTypeCommand(String typeCommand) {
        this.typeCommand = typeCommand;
    }
}
