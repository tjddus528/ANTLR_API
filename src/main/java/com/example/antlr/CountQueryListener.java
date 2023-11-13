package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseListener;

public class CountQueryListener extends MySqlParserBaseListener {
    public int startRuleCount = 0;
//    // CREATE
//    @Override public void enterColumnCreateTable(MySqlParser.ColumnCreateTableContext ctx){
//        startRuleCount++;
//    }
//    @Override public void enterCreateDatabase(MySqlParser.CreateDatabaseContext ctx) { startRuleCount++; }
//
//    //INSERT
//    @Override public void enterInsertStatement(MySqlParser.InsertStatementContext ctx) { startRuleCount++; }
//
//    // UPDATE
//    @Override public void enterUpdateStatement(MySqlParser.UpdateStatementContext ctx) { startRuleCount++; } // multiple single 추가 ??
//
//    // DELETE
//    @Override public void enterDeleteStatement(MySqlParser.DeleteStatementContext ctx) { startRuleCount++; } // multiple single 추가 ??
//
//    // DROP
//    @Override public void enterDropTable(MySqlParser.DropTableContext ctx) { startRuleCount++; }  // tablespace trigger view user database index
//    @Override public void enterDropDatabase(MySqlParser.DropDatabaseContext ctx) { startRuleCount++; }

    // SELECT (Simple Select 포함)
    @Override
    public void enterQuerySpecification(MySqlParser.QuerySpecificationContext ctx){
        startRuleCount++;
         // System.out.println("enterQuerySpecification");
    }
    @Override
    public void enterQuerySpecificationNointo(MySqlParser.QuerySpecificationNointoContext ctx){
        startRuleCount++;
        // System.out.println("enterQuerySpecificationNOINTO");
    }

    // Union
    @Override
    public void enterUnionStatement(MySqlParser.UnionStatementContext ctx){
        startRuleCount++;
        // System.out.println("enterUnionStatement");
    }

    public int getStartRuleCount(){
        return startRuleCount;
    }
}
