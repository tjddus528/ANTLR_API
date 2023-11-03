package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseVisitor;

public class ExtractQueryVisitor extends MySqlParserBaseVisitor {
//    public String visitExpressionAtomPredicate (MySqlParser.ExpressionAtomContext ctx) {
//        if (ctx.getChild(0) != null) {
//            return visitSubqueryExpressionAtom(ctx);  // getRuleContext(MySqlParser.SubqueryExpressionAtomContext.class, 0)
//        }
//        // Handle other cases as needed
//        return null;
//    }

    @Override public String visitSimpleSelect(MySqlParser.SimpleSelectContext ctx) {
        return "";
    }
    public String visitSubqueryExpressionAtom(MySqlParser.SubqueryExpressionAtomContext ctx){
        String string = "";
        String[] word = new String[50];

        int idx = 0;
        word[idx++] = ctx.getChild(0).getText();
//        word[idx++] = ctx.querySpecification().selectElements().selectElement(0).getText();
//        word[idx++] = ctx.querySpecification().fromClause().FROM().getText();  // fromClause 안에 Where 절 조작 함수들 포함 되어 있음
//        word[idx++] = ctx.querySpecification().fromClause().tableSources().tableSource(0).getText(); // table 혹시 여러개면 tableSource(int i) 함수 말고, List<TableSourceContext> 반환하는 tableSource() 사용 ??
//        word[idx++] = ctx.querySpecification().fromClause().WHERE().getText();

        for(int i=0; i < idx; i++){
            string += word[i]+" ";
        }

        return string;
    }


//    public YourReturnType visitYourRootRule(YourGrammarNameParser.YourRootRuleContext ctx) {
//        // 만약 시작 규칙이 'YourRootRule'이라면, 이 메서드가 Visitor에서 시작 지점이 될 것입니다.
//        // 원하는 노드에 도달하려면 여기서 트리를 순회합니다.
//
//        // 예를 들어, 'YourRootRule' 안의 특정 노드를 찾고자 한다면:
//        for (ParseTree child : ctx.children) {
//            YourReturnType result = child.accept(this); // 자식 노드 방문
//            // 여기에 해당 노드에서 수행하고자 하는 작업을 추가할 수 있습니다.
//            // 예: 특정 조건을 만족하면 처리
//            if (child instanceof YourGrammarNameParser.YourDesiredNodeContext) {
//                // 특정 노드에 도달했을 때 수행할 작업
//                // YourDesiredNodeContext가 찾고자 하는 노드의 클래스명입니다.
//                // 작업 추가
//            }
//        }
//        return null;
//    }
}
