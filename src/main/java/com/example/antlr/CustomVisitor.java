package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseVisitor;
import org.antlr.v4.runtime.RuleContext;

import java.util.HashMap;
import java.util.Map;

public class CustomVisitor extends MySqlParserBaseVisitor { // 3단계 visitor
    private int plusCount = 0;

    @Override
    public Object visitRoot(MySqlParser.RootContext ctx){
        return visitChildren(ctx.parent);
    }

//    @Override
//    public Map<String, String> visitQuerySpecification(MySqlParser.QuerySpecificationContext ctx){
//        Map<String, String> keyValue = new HashMap<>();
//
//        String keyword = ctx.SELECT().getText();
//        String colname = ctx.selectElements().selectElement(0).getText();
//        String tablename = ctx.fromClause().tableSources().tableSource(0).getText();
//
//        keyValue.put("keyword", keyword);
//        keyValue.put("column", colname);
//        keyValue.put("table", tablename);
//
//        return keyValue;

//    }

    @Override
    public RuleContext visitQuerySpecification(MySqlParser.QuerySpecificationContext ctx) {
        System.out.println("ENTER QUERY SPECIFICATION");
        System.out.println("columns : " + ctx.selectElements().selectElement(0).getText());

        return (RuleContext) visitChildren(ctx);

    }
//    @Override
//    public Map<String, String> visitQuerySpecification(MySqlParser.QuerySpecificationContext ctx){
//        Map<String, String> keyValue = new HashMap<>();
//
//        String keyword = ctx.SELECT().getText();
//        String colname = ctx.selectElements().selectElement(0).getText();
//        String tablename = ctx.fromClause().tableSources().tableSource(0).getText();
//
//        keyValue.put("keyword", keyword);
//        keyValue.put("column", colname);
//        keyValue.put("table", tablename);
//
//        return keyValue;
//    }

    public int getPlusCount() {
        return plusCount;
    }
    public Integer visitSqlStatement(MySqlParser.SqlStatementsContext ctx){
        for(int i=0;;i++){
            if(ctx.sqlStatement(i) == null) break;
            plusCount++;
        }
        return plusCount;
    }
}
