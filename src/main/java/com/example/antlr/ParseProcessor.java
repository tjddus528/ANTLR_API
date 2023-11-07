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
        MySqlLexer mySqlLexer = new MySqlLexer(charStream);  // 구조 자동으로 변경해 주는 노란선 어떡할까
        CommonTokenStream commonTokenStream = new CommonTokenStream(mySqlLexer);
        MySqlParser mySqlParser = new MySqlParser(commonTokenStream);
//        // count steps.. subquery의 개수 출력
//        MySqlParser.QuerySpecificationContext parseTree = mySqlParser.querySpecification();  // mysqlParser.시작룰(enterRule함수)
//        MySqlParser.SqlStatementsContext tree = mySqlParser.sqlStatements();
//        MySqlParser.SqlStatementContext tree = mySqlParser.sqlStatement();
//        MySqlParser.UnionStatementContext tree = mySqlParser.unionStatement();  // Union 쿼리문은 unionStatement로 startrule 함수 부분 설정하니까 워닝 안생김
//        MySqlParser.SelectStatementContext tree = mySqlParser.selectStatement();
        tree = mySqlParser.root();

        CountQueryListener listener = new CountQueryListener();

        walker.walk(listener, tree);
        int startRuleCount = listener.getStartRuleCount();  // 내가 전체 트리를 순회한다고 생각하면서 짠 코드가 트리 전체를 순회하는 코드가 아닌지 다시 확인
        System.out.println(startRuleCount);

        // 위에 요소에 접근하는 코드와 관련이 있는 것이었다 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!***
        // 단계별 요구 사항을 실행할 때마다 매번 파스 트리를 생성하는 과정을 거쳐야 하는가 > 그런가보다 (사실 더 효율적인 방법이 있을 거 같긴 함;)
    }

    public static void step2(){
        // 2. 쿼리 단계 별로 output string 만들기 (이거는 쿼리 2개인 경우부터 생각해 보는게 나을 듯)
        System.out.println("\n<<step 2>>");
        CharStream charStream1 = CharStreams.fromString("SELECT loan_number FROM borrower WHERE customer_name = (SELECT customer_name FROM depositor WHERE account_number = \"A-215\");");
        MySqlLexer mySqlLexer1 = new MySqlLexer(charStream1);
        CommonTokenStream commonTokenStream1 = new CommonTokenStream(mySqlLexer1);
        MySqlParser mySqlParser1= new MySqlParser(commonTokenStream1);

        //MySqlParser.RootContext tree2 = mySqlParser.root();  // **** startRule 정확하지 않을 수 있음 !!


        // 그냥 트리 상단부터 생짜 구현
//        MySqlParser.RootContext tree2 = mySqlParser1.root();
////        MySqlParser.QuerySpecificationContext tree2 = mySqlParser1.querySpecification();
//        ExtractQueryVisitor visitor = new ExtractQueryVisitor();
//        MySqlParser.ExpressionAtomContext tree_2 = tree2.getRuleContext(MySqlParser.ExpressionAtomContext.class, 0);
//        String st = visitor.visitExpressionAtomPredicate(tree_2);
////        String st = visitor.visitExpressionAtomPredicate(tree2);


        // ExtractQuery Visitor로 시도한 버전 (VisitExpressionAtom 시도한 버전.. StartRule 때문인지 뭐 떄문에 안되는 걸까 !!!!)
        // MySqlParser.ExpressionAtomContext tree3 = mySqlParser1.expressionAtom();   // [error] : line 1:7 mismatched input 'loan_number' expecting {'.', DOT_ID}
//        MySqlParser.QuerySpecificationContext tree3 = mySqlParser1.querySpecification();  // 윗줄의 에러는 해소됨, ; 출력 안됌
        //MySqlParser.RootContext tree3 = mySqlParser1.root();  // 출력 끝에 ; 랑 <EOF> 가 같이 출력
        //MySqlParser.RootContext tree3 = mySqlParser1.root();
//        MySqlParser.SelectStatementContext tree3 = mySqlParser1.selectStatement();

//        tree3.getRuleContext(MySqlParser.SubqueryExpressionAtomContext.class, 0);
        // mysqlParser.시작룰  *** 이걸 적당하게 인자로 가져다 줘야 하는데 . . . .
//        ExtractQueryVisitor visitor = eac.getR  // getParent해서 다시 내려오는 방법??
//        ExtractQueryVisitor visitor = new ExtractQueryVisitor();

//        ParseTree pt = tree3.getChild(1);
//        System.out.println(pt.getText());
//        ParseTree subtree = pt.getChild(1);
//        System.out.println(subtree.getText());

//        String str = visitor.visitSubqueryExpressionAtom(tree3.getRuleContext(MySqlParser.SubqueryExpressionAtomContext.class, 0));
//        System.out.println(str);



        // ExtractQuery Listener로 시도한 버전
        ExtractQueryListener listener1 = new ExtractQueryListener();
//        ParseTreeWalker walker1 = new ParseTreeWalker();
//        walker1.walk(listener1, tree2);
        walker.walk(listener1, tree);  // tree는 1단계에서 사용한 tree를 사용해도 됨(새로 만들면 오류 남 ;; 왜 그런건지는 모르겠음 ;;)// /  listener는 새로 만들기
//        listener1.giveCountNum(listener.startRuleCount);
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