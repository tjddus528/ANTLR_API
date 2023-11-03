package com.example.antlrapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SqlComponent {
    int step;
    String keyword;
    String columns;
    String table;
    Character con;
}
