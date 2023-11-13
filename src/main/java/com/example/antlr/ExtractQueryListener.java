package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseListener;
import com.example.antlrapi.dto.SqlComponent;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;

public class ExtractComponentListener extends MySqlParserBaseListener {

    private SqlComponent sqlComponent = new SqlComponent();

    public void extractComponent(MySqlParser.QuerySpecificationContext ctx){

    }

    @Override
    public void enterQuerySpecification(MySqlParser.QuerySpecificationContext ctx) {
        extractComponent(ctx);
    }

    public SqlComponent returnComponent(){
        return sqlComponent;
    }

}
