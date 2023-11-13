package com.example.antlr;

// (Youtube) Antlr Beginner Tutorial 2: Integrating Antrlr in Java Project

import com.example.antlrapi.dto.SqlComponent;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import com.example.antlr.gen.MySqlLexer;
import com.example.antlr.gen.MySqlParser;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseProcessor {

    public static ParseTreeWalker walker = new ParseTreeWalker();
    public static MySqlParser.RootContext tree;

    public static SqlComponent sqlComponent = new SqlComponent();

    public static String getCommand(String sqlQuery){
        String[] words = sqlQuery.split(" ");
        String command = words[0];
        return command;
    }

    public static ArrayList<String> pullSubquery(String sql){
        Pattern pattern = Pattern.compile("\\(\\s*SELECT[^)]*\\)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);

        ArrayList<String> subquery = new ArrayList<>();

        while(matcher.find()){
            String matchedSubstring = matcher.group();
            subquery.add(matchedSubstring);
        }

        return subquery;
    }

    public static int step1(String sqlQuery){
        // 파싱 준비
        CharStream charStream = CharStreams.fromString(sqlQuery);
        MySqlLexer mySqlLexer = new MySqlLexer(charStream);
        CommonTokenStream commonTokenStream = new CommonTokenStream(mySqlLexer);
        MySqlParser mySqlParser = new MySqlParser(commonTokenStream);

        // 트리 생성
        tree = mySqlParser.root(); // mysqlParser.시작룰(enterRule함수)

        // 쿼리 갯수 세어주는 리스너 생성
        CountQueryListener listener = new CountQueryListener();

        // 순회
        walker.walk(listener, tree);

        int startRuleCount = listener.getStartRuleCount();
        return startRuleCount;

        // 단계별 요구 사항을 실행할 때마다 매번 파스 트리를 생성하는 과정을 거쳐야 하는가 > 그런가보다
    }

    public static SqlComponent step2(String sqlQuery) {
        // 파싱 준비
        CharStream charStream1 = CharStreams.fromString(sqlQuery);
        MySqlLexer mySqlLexer1 = new MySqlLexer(charStream1);
        CommonTokenStream commonTokenStream1 = new CommonTokenStream(mySqlLexer1);
        MySqlParser mySqlParser1 = new MySqlParser(commonTokenStream1);

        ExtractComponentListener listener2 = new ExtractComponentListener();
        walker.walk(listener2, tree);  // tree는 1단계에서 사용한 tree를 사용해도 됨(새로 만들면 오류 남 ;; 왜 그런건지는 모르겠음 ;;), listener는 새로 만들기

        sqlComponent = listener2.returnComponent();

        return sqlComponent;
    }

        public static ArrayList<SqlComponent> step3 (String sqlQuery){

            // 파싱 준비 과정
            CharStream charStream = CharStreams.fromString(sqlQuery);
            MySqlLexer mySqlLexer = new MySqlLexer(charStream);
            CommonTokenStream commonTokenStream = new CommonTokenStream(mySqlLexer);
            MySqlParser mySqlParser = new MySqlParser(commonTokenStream);

            tree = mySqlParser.root();
//
//        queryTree = mySqlParser.querySpecification();
//        String str = queryTree.getText();
//        System.out.println(str);


            // 커스텀 리스너 생성
            ComponentListener listener = new ComponentListener();

            // 커스텀한 리스너를 통해 tree를 root부터 순회
            walker.walk(listener, tree);
//        walker.walk(listener, queryTree.getParent());

            ArrayList<SqlComponent> sqlComponents = listener.returnComponents();


            return sqlComponents;


        }

    }