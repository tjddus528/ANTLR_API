package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseListener;
import com.example.antlrapi.dto.SqlComponent;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.Stack;

public class SqlStepListener extends MySqlParserBaseListener {

    int flagForQuerySpecification = 0;
    SqlStepVisitor sqlStepVisitor = new SqlStepVisitor();

    @Override public void enterQuerySpecificationNointo(MySqlParser.QuerySpecificationNointoContext ctx) {
//        System.out.println("enterQuerySpecificationNointo");
        String sql = ctx.getText();
//        System.out.println(sql);

        if(flagForQuerySpecification == 0) {
            sqlStepVisitor.visitQuerySpecificationNointo(ctx);
            flagForQuerySpecification = 1;
        }

//        System.out.println();
    }

    @Override public void enterQuerySpecification(MySqlParser.QuerySpecificationContext ctx) {
//        System.out.println("enterQuerySpecification");
        String sql = ctx.getText();
//        System.out.println(sql);

        if(flagForQuerySpecification == 0) {
            sqlStepVisitor.visitQuerySpecification(ctx);
            flagForQuerySpecification = 1;
        }
//        System.out.println();
    }

    @Override public void exitQuerySpecification(MySqlParser.QuerySpecificationContext ctx) {
//        System.out.println("exitQuerySpecification");
        String sql = ctx.getText();
//        System.out.println(sql);
        flagForQuerySpecification = 0;
//        System.out.println();

    }


    @Override public void exitQuerySpecificationNointo(MySqlParser.QuerySpecificationNointoContext ctx) {
//        System.out.println("exitQuerySpecificationNointo");
        String sql = ctx.getText();
//        System.out.println(sql);
        flagForQuerySpecification = 0;

//        System.out.println();
    }


    @Override public void enterUnionSelect(MySqlParser.UnionSelectContext ctx) {
//        System.out.println("enterUnionSelect");
        String sql = ctx.getText();
//        System.out.println(sql);
//        System.out.println();
    }

    @Override public void exitUnionSelect(MySqlParser.UnionSelectContext ctx) {
//        System.out.println("exitUnionSelect");
        String sql = ctx.getText();
//        System.out.println(sql);
//        System.out.println();
    }

    @Override public void enterUnionParenthesisSelect(MySqlParser.UnionParenthesisSelectContext ctx) {
//        System.out.println("enterUnionParenthesisSelect");
        String sql = ctx.getText();
//        System.out.println(sql);
    }

    @Override public void exitUnionParenthesisSelect(MySqlParser.UnionParenthesisSelectContext ctx) {
//        System.out.println("exitUnionParenthesisSelect");
        String sql = ctx.getText();
//        System.out.println(sql);
//        System.out.println();
    }
    @Override public void enterUnionParenthesis(MySqlParser.UnionParenthesisContext ctx) {
//        System.out.println("enterUnionParenthesis");
        String sql = ctx.getText();
//        System.out.println(sql);
//        System.out.println();
    }
    @Override public void exitUnionParenthesis(MySqlParser.UnionParenthesisContext ctx) {
//        System.out.println("exitUnionParenthesis");
        String sql = ctx.getText();
//        System.out.println(sql);
//        System.out.println();
    }
    @Override public void enterUnionStatement(MySqlParser.UnionStatementContext ctx) {
//        System.out.println("enterUnionStatement");
        String sql = ctx.getText();
//        System.out.println(sql);
        sqlStepVisitor.visitUnionStatement(ctx);

//        System.out.println();
    }
    @Override public void exitUnionStatement(MySqlParser.UnionStatementContext ctx) {
//        System.out.println("exitUnionStatement");
        String sql = ctx.getText();
//        System.out.println(sql);
        sqlStepVisitor.visitUnionStatement(ctx);

//        System.out.println();
    }


}
