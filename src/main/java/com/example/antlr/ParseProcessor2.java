package com.example.antlr;

import com.example.antlr.gen.MySqlLexer;
import com.example.antlr.gen.MySqlParser;
import com.example.antlrapi.dto.StepSqlComponent;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;

public class ParseProcessor2 {
    public static ParseTreeWalker walker = new ParseTreeWalker();
    public static MySqlParser.RootContext tree;

    public static ArrayList<StepSqlComponent> divideSqlIntoStep(String sql) {

        // 파싱 준비
        CharStream charStream = CharStreams.fromString(sql);
        MySqlLexer mySqlLexer = new MySqlLexer(charStream);
        CommonTokenStream commonTokenStream = new CommonTokenStream(mySqlLexer);
        MySqlParser mySqlParser = new MySqlParser(commonTokenStream);

        // 트리 생성
        tree = mySqlParser.root(); // mysqlParser.시작룰(enterRule함수)

        // 과정 별 쿼리 순서를 저장하는 리스너 생성
        SqlStepListener listener = new SqlStepListener();

        listener.sqlStepVisitor.initSql();


        // 순회
        walker.walk(listener, tree);

        ArrayList<String> sqlList = listener.sqlStepVisitor.sqlList;
        for (String s : sqlList) {
            System.out.println(s);
        }

        ArrayList<StepSqlComponent> stepSqlComponentsListResult = listener.sqlStepVisitor.stepSqlComponentsList;
        return stepSqlComponentsListResult;


    }
}
