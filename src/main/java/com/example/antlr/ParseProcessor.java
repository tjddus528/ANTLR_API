package com.example.antlr;

// (Youtube) Antlr Beginner Tutorial 2: Integrating Antrlr in Java Project

import com.example.antlrapi.dto.SqlComponent;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import com.example.antlr.gen.MySqlLexer;
import com.example.antlr.gen.MySqlParser;
import org.json.JSONObject;

import java.util.ArrayList;

public class ParseProcessor {

    public static ParseTreeWalker walker = new ParseTreeWalker();
    public static MySqlParser.RootContext tree;
    public static MySqlParser.QuerySpecificationContext queryTree;

    public static JSONObject[] component = null;
    public static ArrayList<SqlComponent> sqlComponents = new ArrayList<>();

    public static void step1(){
        // 1. 쿼리가 몇 개 인지 파악
        System.out.println("<<step 1>>");

        CharStream charStream = CharStreams.fromString("SELECT loan_number FROM borrower WHERE customer_name = (SELECT customer_name FROM depositor WHERE account_number = \"A-215\");");
        MySqlLexer mySqlLexer = new MySqlLexer(charStream);
        CommonTokenStream commonTokenStream = new CommonTokenStream(mySqlLexer);
        MySqlParser mySqlParser = new MySqlParser(commonTokenStream);

//      MySqlParser.UnionStatementContext tree = mySqlParser.unionStatement();
//      Union 쿼리문은 unionStatement로 startrule 함수 부분 설정하니까 워닝 안생김
//
        tree = mySqlParser.root(); // mysqlParser.시작룰(enterRule함수)

        CountQueryListener listener = new CountQueryListener();

        walker.walk(listener, tree);
        int startRuleCount = listener.getStartRuleCount();
        System.out.println(startRuleCount);

        // 단계별 요구 사항을 실행할 때마다 매번 파스 트리를 생성하는 과정을 거쳐야 하는가 > 그런가보다 (사실 더 효율적인 방법이 있을 거 같긴 함;)
    }

    public static void step2(){
        // 2. 쿼리 단계 별로 output string 만들기 (이거는 쿼리 2개인 경우부터 생각해 보는게 나을 듯)
        System.out.println("\n<<step 2>>");

        CharStream charStream1 = CharStreams.fromString("SELECT loan_number FROM borrower WHERE customer_name = (SELECT customer_name FROM depositor WHERE account_number = \"A-215\");");
        MySqlLexer mySqlLexer1 = new MySqlLexer(charStream1);
        CommonTokenStream commonTokenStream1 = new CommonTokenStream(mySqlLexer1);
        MySqlParser mySqlParser1 = new MySqlParser(commonTokenStream1);

        // ExtractQuery Listener로 시도한 버전
        ExtractQueryListener listener1 = new ExtractQueryListener();
        walker.walk(listener1, tree);  // tree는 1단계에서 사용한 tree를 사용해도 됨(새로 만들면 오류 남 ;; 왜 그런건지는 모르겠음 ;;), listener는 새로 만들기

        String[] query = listener1.returnQuery();

        for(int i=0;i<query.length;i++){
            if (query[i]!= null)
                System.out.println(query[i]);
        }
    }


    public static ArrayList<SqlComponent> step3(String sqlQuery){

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