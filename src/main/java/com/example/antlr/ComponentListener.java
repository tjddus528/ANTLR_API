package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseListener;
import com.example.antlrapi.dto.SqlComponent;
import org.json.JSONObject;

public class ComponentListener extends MySqlParserBaseListener {

    JSONObject[] component = new JSONObject[5];
    SqlComponent[] sqlComponent = new SqlComponent[5];

    private int order = 1;

    public void analyzeQuery(MySqlParser.QuerySpecificationContext ctx){
        String keyword = "";
        String columns = "";
        String table = "";
        Character con = 'N';

        keyword = ctx.SELECT().getText();
        columns = ctx.selectElements().selectElement(0).getText();
        table = ctx.fromClause().tableSources().tableSource(0).getText(); // table 혹시 여러개면 tableSource(int i) 함수 말고, List<TableSourceContext> 반환하는 tableSource() 사용 ??
        if (ctx.fromClause().WHERE().getText() != null){
            con = 'Y';
        }

        sqlComponent[1] = new SqlComponent(1, keyword, columns, table, con);

//        component[1] = new JSONObject();
//        component[1].put("step", 1);
//        component[1].put("keyword", keyword);
//        component[1].put("columns", columns);
//        component[1].put("tables", keyword);
//        component[1].put("condition", con);

    }

    public void analyzeSubquery(MySqlParser.QuerySpecificationContext ctx){
        String keyword = "";
        String columns = "";
        String table = "";
        Character con = 'N';

        keyword = ctx.SELECT().getText();
        columns = ctx.selectElements().selectElement(0).getText();
        table = ctx.fromClause().tableSources().tableSource(0).getText(); // table 혹시 여러개면 tableSource(int i) 함수 말고, List<TableSourceContext> 반환하는 tableSource() 사용 ??
        if (ctx.fromClause().WHERE().getText() != null){
            con = 'Y';
        }

        sqlComponent[0] = new SqlComponent(2, keyword, columns, table, con);


//        component[0] = new JSONObject();
//        component[0].put("step", 2);
//        component[0].put("keyword", keyword);
//        component[0].put("columns", columns);
//        component[0].put("tables", keyword);
//        component[0].put("condition", con);
    }


    // enterQuerySpecification 함수로 순회 순서 조정해서 뽑아낸 경우
    @Override
    public void enterQuerySpecification(MySqlParser.QuerySpecificationContext ctx) {
        //System.out.println("ENTER QUERY SPECIFICATION");

        if (order == 1) {
            analyzeQuery(ctx);
            order = 2;
        } else {  // if order == 2
            analyzeSubquery(ctx);
        }

    }

    public SqlComponent[] returnComponents(){
        return sqlComponent;
    }
}
