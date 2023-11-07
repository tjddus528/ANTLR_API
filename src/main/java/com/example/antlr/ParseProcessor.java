package com.example.antlr;

// (Youtube) Antlr Beginner Tutorial 2: Integrating Antrlr in Java Project

import com.example.antlrapi.dto.SqlComponent;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import com.example.antlr.gen.MySqlLexer;
import com.example.antlr.gen.MySqlParser;
import org.json.JSONObject;

public class ParseProcessor {

    public static ParseTreeWalker walker = new ParseTreeWalker();
    public static MySqlParser.RootContext tree;

    public static JSONObject[] component = null;
    public static SqlComponent[] sqlComponent;

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

    public static SqlComponent[] step3(String sqlQuery){
        // 3. 쿼리의 구성 요소 key value 형식으로 뽑아내기
        CharStream charStream2 = CharStreams.fromString(sqlQuery);
        MySqlLexer mySqlLexer2 = new MySqlLexer(charStream2);
        CommonTokenStream commonTokenStream2 = new CommonTokenStream(mySqlLexer2);
        MySqlParser mySqlParser2 = new MySqlParser(commonTokenStream2);

        ComponentListener listener2 = new ComponentListener();

        tree = mySqlParser2.root();
        walker.walk(listener2, tree);

        sqlComponent = listener2.returnComponents();

        return sqlComponent;

    }
}