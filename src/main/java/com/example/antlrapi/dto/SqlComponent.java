package com.example.antlrapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@AllArgsConstructor
public class SqlComponent {
    int step;
    String sql;
    String keyword;
    ArrayList<String> columns;
    ArrayList<String> tables;
    Condition condition;

    public SqlComponent(){
        step = 0;
        sql = null;
        keyword = null;
        columns = null;
        tables = null;
        condition = null;
    }

    // 단순한 쿼리문을 위한 생성자
    public SqlComponent(int step, String keyword, String sql){
        this.step = step;
        this.keyword = keyword;
        this.sql = sql;
        this.columns = null;
        this.tables = null;
        this.condition = null;
    }
}
