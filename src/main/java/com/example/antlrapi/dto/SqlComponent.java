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
    String keyword;
    ArrayList<String> columns;
    ArrayList<String> tables;
    Condition condition;

    public SqlComponent() {
        step = 0;
        keyword = null;
        columns = null;
        tables = null;
        condition = null;
    }

}
