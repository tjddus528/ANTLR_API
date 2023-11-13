package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseListener;
import com.example.antlrapi.dto.Condition;
import com.example.antlrapi.dto.SqlComponent;
import org.antlr.runtime.tree.ParseTree;
import org.antlr.v4.runtime.RuleContext;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;

public class ComponentListener extends MySqlParserBaseListener {

    SqlComponent sqlComponent = new SqlComponent();
    ArrayList<SqlComponent> resultSqlComponets = new ArrayList<>();

    private int order = 1;

    public void analyzeQuery(MySqlParser.QuerySpecificationContext ctx){
        String keyword = "";
        String sql = "";
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<String> tables = new ArrayList<>();
        Condition condition = new Condition();

        keyword = ctx.SELECT().getText();

        int columnCnt = ctx.selectElements().getChildCount();
        for(int i=0;i<columnCnt;i++) {
            int index = 0;
            String str = ctx.selectElements().getChild(i).getText();
            if(str.equals(",")) continue;
            else {
                columns.add(str);
            }
        }

        int tableCnt = ctx.fromClause().tableSources().getChildCount();
        for(int i=0;i<tableCnt;i++) {
            int index = 0;
            String str = ctx.fromClause().tableSources().getChild(i).getText();
            if(str.equals(",")) continue;
            else tables.add(str);
        }

        // table 혹시 여러개면 tableSource(int i) 함수 말고, List<TableSourceContext> 반환하는 tableSource() 사용 ??

        if (ctx.fromClause().WHERE() != null){
            condition.setHaveCondition(true);
            condition.setType(ctx.fromClause().WHERE().getText());
            for (org.antlr.v4.runtime.tree.ParseTree child : ctx.fromClause().expression().children) {

                condition.setSubject(child.getChild(0).getText());
                condition.setOperator(child.getChild(1).getText());
                condition.setObject(child.getChild(2).getText());

            }
        }

        resultSqlComponets.add(new SqlComponent(1, keyword, sql, columns, tables, condition));


    }

    public void analyzeSubquery(MySqlParser.QuerySpecificationContext ctx){
//        String keyword = "";
//        ArrayList<String> columns = new ArrayList<>();
//        ArrayList<String> tables = new ArrayList<>();
//        Condition condition = new Condition();;
//
//        keyword = ctx.SELECT().getText();
//
//        int columnCnt = ctx.selectElements().getChildCount();
//        for(int i=0;i<columnCnt;i++) {
//            int index = 0;
//            String str = ctx.selectElements().selectElement(i).getText();
//            if(str.equals(",")) continue;
//            else columns.add(str);
//        }
//
//        int tableCnt = ctx.fromClause().tableSources().getChildCount();
//        for(int i=0;i<tableCnt;i++) {
//            int index = 0;
//            String str = ctx.fromClause().tableSources().getChild(i).getText();
//            if(str.equals(",")) continue;
//            else tables.add(str);
//        }
//
//        // table 혹시 여러개면 tableSource(int i) 함수 말고, List<TableSourceContext> 반환하는 tableSource() 사용 ??
//
//        if (ctx.fromClause().WHERE().getText() != null){
//            condition.setHaveCondition(true);
//
//        }
//
//        resultSqlComponets.add(new SqlComponent(2, keyword, columns, tables, condition));
    }


    // enterQuerySpecification 함수로 순회 순서 조정해서 뽑아낸 경우
    @Override
    public void enterQuerySpecification(MySqlParser.QuerySpecificationContext ctx) {
        System.out.println("ENTER QUERY SPECIFICATION");
        System.out.println("columns : " + ctx.selectElements().selectElement(0).getText());
        System.out.println("tables : " + ctx.fromClause().tableSources().tableSource(0).getText());

        RuleContext fromCtx = ctx.fromClause();

        int number = fromCtx.getChildCount();
        System.out.println("number : " + number);
        for(int i=0;i<number;i++) {
            System.out.println(fromCtx.getChild(i).getText());
        }
        System.out.println();


        if (order == 1) {
            analyzeQuery(ctx);
            order = 2;
        } else {  // if order == 2
            analyzeSubquery(ctx);
        }

    }

    public ArrayList<SqlComponent> returnComponents(){
        return resultSqlComponets;
    }
}
