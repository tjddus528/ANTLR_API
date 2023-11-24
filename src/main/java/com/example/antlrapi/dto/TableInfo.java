package com.example.antlrapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TableInfo {
    String tableName;
    String alias;

    public TableInfo(){
        tableName = null;
        alias = null;
    }

    public TableInfo(String tableName){
        this.tableName = tableName;
        this.alias = null;
    }
}