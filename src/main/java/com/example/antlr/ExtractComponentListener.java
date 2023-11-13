package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseListener;
import com.example.antlrapi.dto.SqlComponent;

import java.util.ArrayList;

public class ExtractComponentListener extends MySqlParserBaseListener {

    private SqlComponent sqlComponent = new SqlComponent();
    int flag = 0;

    public void extractComponent(MySqlParser.QuerySpecificationContext ctx){
        String keyword = "";
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<String> tables = new ArrayList<>();

        keyword = ctx.SELECT().getText();

        int columnCnt = ctx.selectElements().getChildCount();
        for(int i=0; i < columnCnt; i++) {
            int index = 0;
            String str = ctx.selectElements().getChild(i).getText();
            if(str.equals(",")) continue;
            else {
                columns.add(str);
            }
        }

        int tableCnt = ctx.fromClause().tableSources().getChildCount();
        for(int i = 0; i < tableCnt; i++) {
            int index = 0;
            String str = ctx.fromClause().tableSources().getChild(i).getText();
            if(str.equals(",")) continue;
            else tables.add(str);
        }

        // From Clause
        //..

        sqlComponent = new SqlComponent(keyword, columns, tables);
    }

    @Override
    public void enterQuerySpecification(MySqlParser.QuerySpecificationContext ctx) {
        if(flag == 0) {
            extractComponent(ctx);
            flag = 1;
        }
    }

    public SqlComponent returnComponent(){
        return sqlComponent;
    }

}
