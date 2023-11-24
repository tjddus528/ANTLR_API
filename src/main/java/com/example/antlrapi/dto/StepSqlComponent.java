package com.example.antlrapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@AllArgsConstructor
public class StepSqlComponent {
    int step;
    String keyword;
    String sqlStatement;
    ArrayList<TableInfo> tables;
    ArrayList<ColumnInfo> selectedColumns;
    ArrayList<ColumnInfo> conditionColumns;
    ArrayList<String> conditions;

    Boolean joinExists;
    ArrayList<ColumnInfo> joinedColumns;
    ArrayList<String> on;

    String queryA;
    String queryB;

    public StepSqlComponent() {
        step = 0;
        keyword = null;
        sqlStatement = null;
        tables = null;
        selectedColumns = null;
        conditionColumns = null;
        conditions = null;

        joinExists = null;
        joinedColumns = null;
        on = null;

        queryA = null;
        queryB = null;
    }
    public StepSqlComponent(int step, String keyword, String sqlStatement){
        this.step = step;
        this.keyword = keyword;
        this.sqlStatement = sqlStatement;
        this.tables = null;
        this.selectedColumns = null;
        this.conditionColumns = null;
        this.conditions = null;

        this.joinExists = null;
        this.joinedColumns = null;
        this.on = null;

        this.queryA = null;
        this.queryB = null;
    }
}