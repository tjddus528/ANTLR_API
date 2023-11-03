package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseListener;

public class CountQueryListener extends MySqlParserBaseListener {
    public int startRuleCount = 0;
    @Override
    public void enterQuerySpecification(MySqlParser.QuerySpecificationContext ctx){
        startRuleCount++;
        System.out.println("enterQuerySpecification");
    }
    @Override
    public void enterQuerySpecificationNointo(MySqlParser.QuerySpecificationNointoContext ctx){
        startRuleCount++;
        System.out.println("enterQuerySpecificationNOINTO");
    }

    @Override
    public void enterUnionStatement(MySqlParser.UnionStatementContext ctx){
        startRuleCount++;
        System.out.println("enterUnionStatement");
    }

    public int getStartRuleCount(){
        return startRuleCount;
    }
}
