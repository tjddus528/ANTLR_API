package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseListener;

public class ExtractQueryListener extends MySqlParserBaseListener {

    private String[] query = new String[10];
    private int order = 1;
    public int count = 0;
    public int qid = 1;
//    MySqlParser.QuerySpecificationContext ctx = null;


    public void makeQuery(MySqlParser.QuerySpecificationContext ctx){
        String string = "";
        String[] word = new String[50];

        int idx = 0;
        word[idx++] = ctx.SELECT().getText();
        word[idx++] = ctx.selectElements().selectElement(0).getText();
        word[idx++] = ctx.fromClause().FROM().getText();  // fromClause 안에 Where 절 조작 함수들 포함 되어 있음
        word[idx++] = ctx.fromClause().tableSources().tableSource(0).getText(); // table 혹시 여러개면 tableSource(int i) 함수 말고, List<TableSourceContext> 반환하는 tableSource() 사용 ??
        word[idx++] = ctx.fromClause().WHERE().getText();
//        word[idx++] = ctx.fromClause().getChild(3).getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).getText();
        word[idx++] = ctx.fromClause().expression().getChild(0).getChild(0).getText();
        word[idx++] = ctx.fromClause().expression().getChild(0).getChild(1).getText();
        if (ctx.fromClause().expression().getChild(0).getChild(1) != null){
            word[idx++] = "(@)";  // 서브 쿼리 삽입 지점
        }

        for(int i=0; i < idx; i++){
            if (i == idx-1){
                string += word[i];
            }
            else{
                string += word[i]+" ";
            }
        }

        query[qid++] = string;

    }
    public void makeSubquery(MySqlParser.QuerySpecificationContext ctx){
        String string = "";
        String[] word = new String[50];

        int idx = 0;
        word[idx++] = ctx.SELECT().getText();
        word[idx++] = ctx.selectElements().selectElement(0).getText();
        word[idx++] = ctx.fromClause().FROM().getText();  // fromClause 안에 Where 절 조작 함수들 포함 되어 있음
        word[idx++] = ctx.fromClause().tableSources().tableSource(0).getText(); // table 혹시 여러개면 tableSource(int i) 함수 말고, List<TableSourceContext> 반환하는 tableSource() 사용 ??
        word[idx++] = ctx.fromClause().WHERE().getText();
//        word[idx++] = ctx.fromClause().getChild(3).getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).getText();
        word[idx++] = ctx.fromClause().expression().getChild(0).getChild(0).getText();
        word[idx++] = ctx.fromClause().expression().getChild(0).getChild(1).getText();
        word[idx++] = ctx.fromClause().expression().getChild(0).getChild(2).getText();

        for(int i=0; i < idx; i++){
            if (i == idx-1){
                string += word[i];
            }
            else{
                string += word[i]+" ";
            }

        }

        query[qid++] = string;
    }

    public void attachQuery(){
        StringBuilder stringBuilder = new StringBuilder(query[1]);
        query[0] = new String(query[1]);
        query[0]= query[0].replace("@", query[2]);
//        query[0] = String.valueOf(stringBuilder.insert(query[1].indexOf("@"), query[2]));
    }

    @Override
    public void enterQuerySpecification(MySqlParser.QuerySpecificationContext ctx) {
        //System.out.println("ENTER QUERY SPECIFICATION");

        if (order == 1) {
            makeQuery(ctx);
            order = 2;
        } else {  // if order == 2
            makeSubquery(ctx);

            attachQuery();
        }

    }

        // 여기서 쿼리문의 종류에 따라 분기 ?? (지금은 in predicate 인 경우) > 분기가 가능한가 > 일단은 이후 쿼리를 앞 쿼리에 붙이는 방식으로 진행 ..(한정적임)
        //System.out.printf("size of children : " + String.valueOf(ctx.fromClause().expression().children.size()));
//        for(int i = 0;i<pt.size();i++){
//            System.out.printf(pt.get(i).getText());
//            System.out.printf("one word\n");
//        }

    public String[] returnQuery(){
        return query;
    }

}
