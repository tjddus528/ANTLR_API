package com.example.antlrapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ColumnInfo {
    String tableName;
    String columnLabel;
    String alias;

    public ColumnInfo(){
        tableName = null;
        columnLabel = null;
        alias = null;
    }
    public ColumnInfo(String tableName, String columnLable){
        this.tableName = tableName;
        this.columnLabel = columnLable;
    }
    public ColumnInfo(String columnLabel){
        this.columnLabel = columnLabel;
    }
}