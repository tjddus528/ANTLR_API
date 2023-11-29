package com.example.antlr;

import com.example.antlr.gen.MySqlParser;
import com.example.antlr.gen.MySqlParserBaseVisitor;
import com.example.antlrapi.dto.ColumnInfo;
import com.example.antlrapi.dto.StepSqlComponent;
import com.example.antlrapi.dto.TableInfo;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import java.util.*;

public class SqlStepVisitor extends MySqlParserBaseVisitor {

    Queue<String> sqlQueue = new LinkedList<>();
    Queue<String> sqlQueueForComponent = new LinkedList<>();
    Queue<StepSqlComponent> sqlComponentsQueue = new LinkedList<>();
    ArrayList<String> sqlList = new ArrayList<>();

    ArrayList<String> unionList = new ArrayList<>();



    public int extractFlag = 0;
    public int step = 0;
    static ArrayList<StepSqlComponent> stepSqlComponentsList = new ArrayList<>();
    StepSqlComponent stepSqlComponentForComponent = new StepSqlComponent();

    public void initSql() {
        step = 0;
        sqlQueue.clear();
        sqlList.clear();
        unionList.clear();
        stepSqlComponentsList.clear();

    }



    public void extractSelectComponent(MySqlParser.QuerySpecificationContext ctx, String sql) {
        extractFlag = 1;


//        System.out.println("extractSelectComponent");
        ArrayList<TableInfo> usedTables = new ArrayList<>();
        ArrayList<ColumnInfo> selectedColumns = new ArrayList<>();
        ArrayList<ColumnInfo> conditionColumns = new ArrayList<>();
        ArrayList<String> conditions = new ArrayList<>();

        Boolean joinExists = null;
        ArrayList<ColumnInfo> joinedColumns = new ArrayList<>();
        ArrayList<String> on = new ArrayList<>();


        // 1. step, keyword, Sql
        step++;
        StepSqlComponent stepSqlComponent = new StepSqlComponent(step, "SELECT", sql);

        // 2. 사용된 테이블
        int tableSourceBaseCnt = ctx.fromClause().tableSources().getChild(0).getChildCount();
        if (tableSourceBaseCnt == 2) {  //  Inner Join 인 경우

            ParseTree tableSource = ctx.fromClause().tableSources().getChild(0).getChild(0);
            if (tableSource != null) {
                int aliasCnt = tableSource.getChildCount();
                String tableName = tableSource.getChild(0).getText();
//                if (tableSource.getChild(0) instanceof MySqlParser.SubqueryTableItemContext) {
//                    // tableSource.getChild(0).getClass().getName() == "com.example.antlr.gen.MySqlParser$SubqueryTableItemContext"
//                    tableName = tableName.substring(1, tableName.length() - 1);
//                }
                String alias = "";

                if (aliasCnt == 3) {
                    alias = tableSource.getChild(0).getChild(2).getText();
                    usedTables.add(new TableInfo(tableName, alias));
                } else if (aliasCnt == 2) {
                    alias = tableSource.getChild(0).getChild(1).getText();
                    usedTables.add(new TableInfo(tableName, alias));
                } else {
                    usedTables.add(new TableInfo(tableName));
                }
            }

            tableSource = ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2);
            String tableSubQuery = "";
            if (tableSource instanceof MySqlParser.SubqueryTableItemContext){
//                tableSubQuery = visitParenthesisSelectContext((MySqlParser.ParenthesisSelectContext) tableSource.getChild(2));
            }
            else {
                if (tableSource != null) {
                    int aliasCnt = tableSource.getChildCount();
                    String tableName = tableSource.getChild(0).getText();
//                if (tableSource.getChild(0) instanceof MySqlParser.SubqueryTableItemContext) {
//                    // tableSource.getChild(0).getClass().getName() == "com.example.antlr.gen.MySqlParser$SubqueryTableItemContext"
//                    tableName = tableName.substring(1, tableName.length() - 1);
//                }
                    String alias = "";

                    if (aliasCnt == 3) {
                        alias = tableSource.getChild(0).getChild(2).getText();
                        usedTables.add(new TableInfo(tableName, alias));
                    } else if (aliasCnt == 2) {
                        alias = tableSource.getChild(0).getChild(1).getText();
                        usedTables.add(new TableInfo(tableName, alias));
                    } else {
                        usedTables.add(new TableInfo(tableName));
                    }
                }
            }



            tableSource = ctx.fromClause().tableSources().getChild(0).getChild(0);
            if (tableSource != null) {
                int aliasCnt = tableSource.getChildCount();
                String tableName = tableSource.getChild(0).getText();
                if (tableSource.getChild(0) instanceof MySqlParser.SubqueryTableItemContext) {
                    // tableSource.getChild(0).getClass().getName() == "com.example.antlr.gen.MySqlParser$SubqueryTableItemContext"
                    tableName = tableName.substring(1, tableName.length() - 1);
                }
                String alias = "";

                if (aliasCnt == 3) {
                    alias = tableSource.getChild(0).getChild(2).getText();
                    usedTables.add(new TableInfo(tableName, alias));
                } else if (aliasCnt == 2) {
                    alias = tableSource.getChild(0).getChild(1).getText();
                    usedTables.add(new TableInfo(tableName, alias));
                } else {
                    usedTables.add(new TableInfo(tableName));
                }
            }



            // join 테이블 저장
            TableInfo table = new TableInfo(ctx.fromClause().tableSources().getChild(0).getChild(0).getChild(0).getText(), ctx.fromClause().tableSources().getChild(0).getChild(0).getChild(2).getText());
            usedTables.add(table);
            // !!!!!!!!!!!!!!!!!InnerJoin예문, 서브쿼리 들어가는 곳 !!!!! !!!!!!!!!!!!!!!!!!!!!
//            System.out.println("여기를 보시오!! "+ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(0).getClass());
            tableSubQuery = "";
            if (ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(0) instanceof MySqlParser.ParenthesisSelectContext) {
//                System.out.println("여기 들어감?");
//                tableSubQuery = visitParenthesisSelectContext((MySqlParser.ParenthesisSelectContext) ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(0));
            }
//            System.out.println("tableSubQuery = " + tableSubQuery);
            table = new TableInfo(tableSubQuery, ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(2).getText());
            usedTables.add(table);

            // join 존재 여부
            joinExists = true;

            // joinSpec : 1. On 조건에 사용된 칼럼, 2. 전체 조건 Text
            ParseTree joinTreeCtx = ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(3); // joinSpec:1 노드

            // 1. On 조건에 사용된 칼럼
            // 1) 등호 왼쪽 칼럼들
            String tbName = ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).getText();
            //System.out.println("right before : " + tbName);
            String colName = "";
            if (ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(1) != null) {
                colName = ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(1).getText().substring(1);
                joinedColumns.add(new ColumnInfo(tbName, colName));
            }

            // 2) 등호 오른쪽 칼럼들
            tbName = ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).getText();
            if (ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(1) != null) {
                colName = ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(1).getText().substring(1);
                joinedColumns.add(new ColumnInfo(tbName, colName));
            }
            stepSqlComponent.setJoinedColumns(joinedColumns);

            // 2. On 전체 text
            String onExpression = "ON ";
            int childrenOn = joinTreeCtx.getChild(1).getChild(0).getChildCount();
//            System.out.println(childrenOn);
            ParseTree onTree = joinTreeCtx.getChild(1).getChild(0);
            for (int i = 0; i < childrenOn; i++) {
                if (i == childrenOn - 1) {
                    onExpression += onTree.getChild(i).getText();
                } else {
                    onExpression += onTree.getChild(i).getText();
                    onExpression += " ";
                }
            }
//            System.out.println("onExpression : " + onExpression);
            on.add(onExpression);
            stepSqlComponent.setOn(on);

        } else {  // Join 없이 From 뒤에 테이블 ,로 구분해서 여러개 오는 경우
            int tableCnt = ctx.fromClause().tableSources().getChildCount();
            for (int i = 0; i < tableCnt; i++) {
                int index = 0;
                ParseTree tableSource = ctx.fromClause().tableSources().getChild(i);
                if (i == 1)
                    continue;
//                    System.out.println("Join Part : " + tableSource.getText());
                else if (tableSource.getText() == ",") continue;
                else {  // , 가 아닌 경우 (1. grade AS g, 2. person p,  3. id)
//                if(tableSource.getChild(0) != null) {
//                    System.out.println("tablesource type : " + tableSource.getChild(0).getClass().getName());
//                }
                    if (tableSource.getChild(0) != null) {
                        int aliasCnt = tableSource.getChild(0).getChildCount();

                        String tableName = tableSource.getChild(0).getChild(0).getText();
                        if (tableSource.getChild(0) instanceof MySqlParser.SubqueryTableItemContext) {
                            // tableSource.getChild(0).getClass().getName() == "com.example.antlr.gen.MySqlParser$SubqueryTableItemContext"
                            tableName = tableName.substring(1, tableName.length() - 1);
                        }
                        String alias = "";

                        if (aliasCnt == 3) {
                            alias = tableSource.getChild(0).getChild(2).getText();
                            usedTables.add(new TableInfo(tableName, alias));
                        } else if (aliasCnt == 2) {
                            alias = tableSource.getChild(0).getChild(1).getText();
                            usedTables.add(new TableInfo(tableName, alias));
                        } else {
                            usedTables.add(new TableInfo(tableName));
                        }
                    }
                }
            }
        }


        for (int i = 0; i < usedTables.size(); i++) {
//            System.out.print("used table name : " + usedTables.get(i).getTableName());
//            System.out.println(" / used table alias : " + usedTables.get(i).getAlias());
        }
        stepSqlComponent.setTables(usedTables);


        // 3. 선택된 칼럼
        if (ctx.selectElements().getText().equals("*")) {
            // System.out.println("in * column");
            selectedColumns.add(new ColumnInfo(ctx.fromClause().tableSources().getText(), "*"));
//            System.out.println(ctx.fromClause().tableSources().getText());
        } else {
            int columnCnt = ctx.selectElements().getChildCount(); // selectElements 아래 분기 개수(, 포함)
            for (int i = 0; i < columnCnt; i++) {
                ParseTree columnSource = ctx.selectElements().getChild(i);
                if (columnSource.getText() == ",") continue;
                else {
                    if (columnSource.getChild(0) != null) {

                        String tableName = "";
                        String columnLable = "";
                        String alias = "";

                        int dotCnt = columnSource.getChildCount();
                        int tbCnt = columnSource.getChild(0).getChildCount();
                        if (dotCnt == 1) {  //  alias 없는 경우 (테이블 참조 유무 둘다 포함)
                            if (tbCnt == 1) {  // 테이블 참조 없이 칼럼 명만 있는 경우
                                columnLable = columnSource.getChild(0).getChild(0).getText();
//                                if (columnSource.getChild(0).getChild(0) instanceof MySqlParser.ExpressionAtomPredicateContext) {
//                                    columnLable = columnLable.substring(1, columnLable.length() - 1);
//                                }
                            } else {  //  테이블 참조 있는 칼럼인 경우 (ex. c.cake)
                                tableName = columnSource.getChild(0).getChild(0).getText();
                                columnLable = columnSource.getChild(0).getChild(1).getText().substring(1);
                            }
                        } else {  //  alias 있는 경우(AS 유무 둘 다 포함)
                            if (dotCnt == 2) { // AS 없는 경우
//                            int aliasCnt = columnSource.getChild(0).getChildCount();
                                if (tbCnt == 2) {  // table 참조 있는 경우
                                    tableName = columnSource.getChild(0).getChild(0).getText();
                                    columnLable = columnSource.getChild(0).getChild(1).getText().substring(1);
                                    alias = columnSource.getChild(1).getText();
                                } else {  // table 참조 없는 경우
                                    columnLable = columnSource.getChild(0).getText();
                                    if (columnSource.getChild(0).getChild(0) instanceof MySqlParser.ExpressionAtomPredicateContext) {
                                        columnLable = columnLable.substring(1, columnLable.length() - 1);
                                    }
                                    alias = columnSource.getChild(1).getText();
                                }
                            } else {  //  dotCnt == 3 // AS 있는 경우
                                if (tbCnt == 2) {  // 테이블 참조 있는 경우
                                    tableName = columnSource.getChild(0).getChild(0).getText();
                                    columnLable = columnSource.getChild(0).getChild(1).getText().substring(1);
                                    alias = columnSource.getChild(2).getText();
                                } else {  //  테이블 참조 없는 경우
                                    columnLable = columnSource.getChild(0).getChild(0).getText();
                                    if (columnSource.getChild(0).getChild(0) instanceof MySqlParser.ExpressionAtomPredicateContext) {
                                        columnLable = columnLable.substring(1, columnLable.length() - 1);
                                    }
                                    alias = columnSource.getChild(2).getText();
                                }
                            }
                        }

                        selectedColumns.add(new ColumnInfo(tableName, columnLable, alias));
                    }
                }
            }
        }

        for (int i = 0; i < selectedColumns.size(); i++) {
//            System.out.print("selectedColums Table Name : " + selectedColumns.get(i).getTableName());
//            System.out.print(" / selectedColums Column Label : " + selectedColumns.get(i).getColumnLabel());
//            System.out.println(" / selectedColums Alias  : " + selectedColumns.get(i).getAlias());
        }
        stepSqlComponent.setSelectedColumns(selectedColumns);


        // 4. 조건 칼럼
        if (ctx.fromClause().expression() != null) {
            ParseTree children = ctx.fromClause().expression().getChild(0);
            int childrenSize = children.getChildCount();
            for (int i = 0; i < childrenSize; i++) {
                String tableName = "unknown";
                String columnLable = "unknown";
                if(children.getChild(i) instanceof MySqlParser.ExpressionAtomPredicateContext){
//                    System.out.println("NNN : " + children.getChild(i).getChild(0).getChild(0).getClass().getName());
                    if( children.getChild(i).getChild(0).getChild(0) instanceof MySqlParser.FullColumnNameContext) {
                        if (children.getChild(i).getChild(0).getChild(0).getChildCount() == 2) {  //  테이블 참조 있는 경우
                            tableName = children.getChild(i).getChild(0).getChild(0).getChild(0).getText();
                            columnLable = children.getChild(i).getChild(0).getChild(0).getChild(1).getText().substring(1);
                        } else {  //  테이블 참조 없는 경우
                            columnLable = children.getChild(i).getChild(0).getChild(0).getChild(0).getText();
                        }
                        conditionColumns.add(new ColumnInfo(tableName, columnLable));
                    }
//                    System.out.println("table and column : " + tableName + " " + columnLable);
                }

            }


            for (int i = 0; i < conditionColumns.size(); i++) {
//                System.out.print("conditionColumn TableName : " + conditionColumns.get(i).getTableName());
//                System.out.println(" / conditionColumn ColumnLable : " + conditionColumns.get(i).getColumnLabel());
            }

            stepSqlComponent.setConditionColumns(conditionColumns);


            // 5. Where절 전체 저장
            if (ctx.fromClause().WHERE() != null) {
                String whereExpression = "WHERE ";
                ParseTree childern2 = ctx.fromClause().expression().getChild(0);
                // System.out.println(childern2.getText());
                int childrenSize2 = childern2.getChildCount();
                for (int i = 0; i < childrenSize2; i++) {
                    if (i == childrenSize2 - 1) {
                        whereExpression += childern2.getChild(i).getText();
                    } else {
                        whereExpression += childern2.getChild(i).getText();
                        whereExpression += " ";
                    }
                }
                conditions.add(whereExpression);
                stepSqlComponent.setConditions(conditions);

//                System.out.println("Where expression : " + whereExpression);
            }
        }
        // 2. 3. 4. 저장된 컴포넌트 저장
        stepSqlComponentsList.add(stepSqlComponent);
    }
    public void extractSelectComponentNointo(MySqlParser.QuerySpecificationNointoContext ctx, String sql) {
        extractFlag = 1;


//        System.out.println("extractSelectComponent");
        ArrayList<TableInfo> usedTables = new ArrayList<>();
        ArrayList<ColumnInfo> selectedColumns = new ArrayList<>();
        ArrayList<ColumnInfo> conditionColumns = new ArrayList<>();
        ArrayList<String> conditions = new ArrayList<>();

        Boolean joinExists = null;
        ArrayList<ColumnInfo> joinedColumns = new ArrayList<>();
        ArrayList<String> on = new ArrayList<>();


        // 1. step, keyword, Sql
        step++;
        StepSqlComponent stepSqlComponent = new StepSqlComponent(step, "SELECT", sql);

        // 2. 사용된 테이블
        int tableSourceBaseCnt = ctx.fromClause().tableSources().getChild(0).getChildCount();
        if (tableSourceBaseCnt == 2) {  //  Inner Join 인 경우

            // join 테이블 저장
            TableInfo table = new TableInfo(ctx.fromClause().tableSources().getChild(0).getChild(0).getChild(0).getText(), ctx.fromClause().tableSources().getChild(0).getChild(0).getChild(2).getText());
            usedTables.add(table);
            // !!!!!!!!!!!!!!!!!InnerJoin예문, 서브쿼리 들어가는 곳 !!!!! !!!!!!!!!!!!!!!!!!!!!
//            System.out.println("여기를 보시오!! "+ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(0).getClass());
            String tableSubQuery = "";
            if (ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(0) instanceof MySqlParser.ParenthesisSelectContext) {
//                System.out.println("여기 들어감?");
//                tableSubQuery = visitParenthesisSelectContext((MySqlParser.ParenthesisSelectContext) ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(0));
            }
//            System.out.println("tableSubQuery = " + tableSubQuery);
            table = new TableInfo(tableSubQuery, ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(2).getText());
            usedTables.add(table);

            // join 존재 여부
            joinExists = true;

            // joinSpec : 1. On 조건에 사용된 칼럼, 2. 전체 조건 Text
            ParseTree joinTreeCtx = ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(3); // joinSpec:1 노드

            // 1. On 조건에 사용된 칼럼
            // 1) 등호 왼쪽 칼럼들
            String tbName = ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).getText();
            //System.out.println("right before : " + tbName);
            String colName = "";
            if (ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(1) != null) {
                colName = ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(1).getText().substring(1);
                joinedColumns.add(new ColumnInfo(tbName, colName));
            }

            // 2) 등호 오른쪽 칼럼들
            tbName = ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(0).getText();
            if (ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(1) != null) {
                colName = ctx.getChild(1).getChild(0).getChild(0).getChild(0).getChild(0).getChild(1).getText().substring(1);
                joinedColumns.add(new ColumnInfo(tbName, colName));
            }
            stepSqlComponent.setJoinedColumns(joinedColumns);

            // 2. On 전체 text
            String onExpression = "ON ";
            int childrenOn = joinTreeCtx.getChild(1).getChild(0).getChildCount();
//            System.out.println(childrenOn);
            ParseTree onTree = joinTreeCtx.getChild(1).getChild(0);
            for (int i = 0; i < childrenOn; i++) {
                if (i == childrenOn - 1) {
                    onExpression += onTree.getChild(i).getText();
                } else {
                    onExpression += onTree.getChild(i).getText();
                    onExpression += " ";
                }
            }
//            System.out.println("onExpression : " + onExpression);
            on.add(onExpression);
            stepSqlComponent.setOn(on);

        } else {  // Join 없이 From 뒤에 테이블 ,로 구분해서 여러개 오는 경우
            int tableCnt = ctx.fromClause().tableSources().getChildCount();
            for (int i = 0; i < tableCnt; i++) {
                int index = 0;
                ParseTree tableSource = ctx.fromClause().tableSources().getChild(i);
                if (i == 1)
                    continue;
//                    System.out.println("Join Part : " + tableSource.getText());
                else if (tableSource.getText() == ",") continue;
                else {  // , 가 아닌 경우 (1. grade AS g, 2. person p,  3. id)
//                if(tableSource.getChild(0) != null) {
//                    System.out.println("tablesource type : " + tableSource.getChild(0).getClass().getName());
//                }
                    if (tableSource.getChild(0) != null) {
                        int aliasCnt = tableSource.getChild(0).getChildCount();

                        String tableName = tableSource.getChild(0).getChild(0).getText();
                        if (tableSource.getChild(0) instanceof MySqlParser.SubqueryTableItemContext) {
                            // tableSource.getChild(0).getClass().getName() == "com.example.antlr.gen.MySqlParser$SubqueryTableItemContext"
                            tableName = tableName.substring(1, tableName.length() - 1);
                        }
                        String alias = "";

                        if (aliasCnt == 3) {
                            alias = tableSource.getChild(0).getChild(2).getText();
                            usedTables.add(new TableInfo(tableName, alias));
                        } else if (aliasCnt == 2) {
                            alias = tableSource.getChild(0).getChild(1).getText();
                            usedTables.add(new TableInfo(tableName, alias));
                        } else {
                            usedTables.add(new TableInfo(tableName));
                        }
                    }
                }
            }
        }


        for (int i = 0; i < usedTables.size(); i++) {
//            System.out.print("used table name : " + usedTables.get(i).getTableName());
//            System.out.println(" / used table alias : " + usedTables.get(i).getAlias());
        }
        stepSqlComponent.setTables(usedTables);


        // 3. 선택된 칼럼
        if (ctx.selectElements().getText().equals("*")) {
            // System.out.println("in * column");
            selectedColumns.add(new ColumnInfo(ctx.fromClause().tableSources().getText(), "*"));
//            System.out.println(ctx.fromClause().tableSources().getText());
        } else {
            int columnCnt = ctx.selectElements().getChildCount(); // selectElements 아래 분기 개수(, 포함)
            for (int i = 0; i < columnCnt; i++) {
                ParseTree columnSource = ctx.selectElements().getChild(i);
                if (columnSource.getText() == ",") continue;
                else {
                    if (columnSource.getChild(0) != null) {

                        String tableName = "";
                        String columnLable = "";
                        String alias = "";

                        int dotCnt = columnSource.getChildCount();
                        int tbCnt = columnSource.getChild(0).getChildCount();
                        if (dotCnt == 1) {  //  alias 없는 경우 (테이블 참조 유무 둘다 포함)
                            if (tbCnt == 1) {  // 테이블 참조 없이 칼럼 명만 있는 경우
                                columnLable = columnSource.getChild(0).getChild(0).getText();
                                if (columnSource.getChild(0).getChild(0) instanceof MySqlParser.ExpressionAtomPredicateContext) {
                                    columnLable = columnLable.substring(1, columnLable.length() - 1);
                                }
                            } else {  //  테이블 참조 있는 칼럼인 경우 (ex. c.cake)
                                tableName = columnSource.getChild(0).getChild(0).getText();
                                columnLable = columnSource.getChild(0).getChild(1).getText().substring(1);
                            }
                        } else {  //  alias 있는 경우(AS 유무 둘 다 포함)
                            if (dotCnt == 2) { // AS 없는 경우
//                            int aliasCnt = columnSource.getChild(0).getChildCount();
                                if (tbCnt == 2) {  // table 참조 있는 경우
                                    tableName = columnSource.getChild(0).getChild(0).getText();
                                    columnLable = columnSource.getChild(0).getChild(1).getText().substring(1);
                                    alias = columnSource.getChild(1).getText();
                                } else {  // table 참조 없는 경우
                                    columnLable = columnSource.getChild(0).getText();
                                    if (columnSource.getChild(0).getChild(0) instanceof MySqlParser.ExpressionAtomPredicateContext) {
                                        columnLable = columnLable.substring(1, columnLable.length() - 1);
                                    }
                                    alias = columnSource.getChild(1).getText();
                                }
                            } else {  //  dotCnt == 3 // AS 있는 경우
                                if (tbCnt == 2) {  // 테이블 참조 있는 경우
                                    tableName = columnSource.getChild(0).getChild(0).getText();
                                    columnLable = columnSource.getChild(0).getChild(1).getText().substring(1);
                                    alias = columnSource.getChild(2).getText();
                                } else {  //  테이블 참조 없는 경우
                                    columnLable = columnSource.getChild(0).getChild(0).getText();
                                    if (columnSource.getChild(0).getChild(0) instanceof MySqlParser.ExpressionAtomPredicateContext) {
                                        columnLable = columnLable.substring(1, columnLable.length() - 1);
                                    }
                                    alias = columnSource.getChild(2).getText();
                                }
                            }
                        }

                        selectedColumns.add(new ColumnInfo(tableName, columnLable, alias));
                    }
                }
            }
        }

        for (int i = 0; i < selectedColumns.size(); i++) {
//            System.out.print("selectedColums Table Name : " + selectedColumns.get(i).getTableName());
//            System.out.print(" / selectedColums Column Label : " + selectedColumns.get(i).getColumnLabel());
//            System.out.println(" / selectedColums Alias  : " + selectedColumns.get(i).getAlias());
        }
        stepSqlComponent.setSelectedColumns(selectedColumns);


        // 4. 조건 칼럼
        if (ctx.fromClause().expression() != null) {
            ParseTree children = ctx.fromClause().expression().getChild(0);
            int childrenSize = children.getChildCount();
            for (int i = 0; i < childrenSize; i++) {
                String tableName = "unknown";
                String columnLable = "unknown";
                if(children.getChild(i) instanceof MySqlParser.ExpressionAtomPredicateContext){
//                    System.out.println("NNN : " + children.getChild(i).getChild(0).getChild(0).getClass().getName());
                    if( children.getChild(i).getChild(0).getChild(0) instanceof MySqlParser.FullColumnNameContext) {
                        if (children.getChild(i).getChild(0).getChild(0).getChildCount() == 2) {  //  테이블 참조 있는 경우
                            tableName = children.getChild(i).getChild(0).getChild(0).getChild(0).getText();
                            columnLable = children.getChild(i).getChild(0).getChild(0).getChild(1).getText().substring(1);
                        } else {  //  테이블 참조 없는 경우
                            columnLable = children.getChild(i).getChild(0).getChild(0).getChild(0).getText();
                        }
                        conditionColumns.add(new ColumnInfo(tableName, columnLable));
                    }
//                    System.out.println("table and column : " + tableName + " " + columnLable);
                }

            }


            for (int i = 0; i < conditionColumns.size(); i++) {
//                System.out.print("conditionColumn TableName : " + conditionColumns.get(i).getTableName());
//                System.out.println(" / conditionColumn ColumnLable : " + conditionColumns.get(i).getColumnLabel());
            }

            stepSqlComponent.setConditionColumns(conditionColumns);


            // 5. Where절 전체 저장
            if (ctx.fromClause().WHERE() != null) {
                String whereExpression = "WHERE ";
                ParseTree childern2 = ctx.fromClause().expression().getChild(0);
                // System.out.println(childern2.getText());
                int childrenSize2 = childern2.getChildCount();
                for (int i = 0; i < childrenSize2; i++) {
                    if (i == childrenSize2 - 1) {
                        whereExpression += childern2.getChild(i).getText();
                    } else {
                        whereExpression += childern2.getChild(i).getText();
                        whereExpression += " ";
                    }
                }
                conditions.add(whereExpression);
                stepSqlComponent.setConditions(conditions);

//                System.out.println("Where expression : " + whereExpression);
            }
        }
        // 2. 3. 4. 저장된 컴포넌트 저장
        stepSqlComponentsList.add(stepSqlComponent);
    }
    public void extractUnionComponent (MySqlParser.UnionStatementContext ctx, String sql) {
        step++;

        String unionQuery = "";
        String queryA = unionList.remove(0);
        String queryB = unionList.remove(0);
        unionQuery = queryA + " UNION " + queryB;

        StepSqlComponent stepSqlComponent = new StepSqlComponent(step, "UNION", sql);
        stepSqlComponent.setQueryA(queryA);
        stepSqlComponent.setQueryB(queryB);
        stepSqlComponentsList.add(stepSqlComponent);
    }


    @Override public RuleContext visitSelectExpressionElement(MySqlParser.SelectExpressionElementContext ctx) {
//        System.out.println("visitSelectExpressionElement");
//        System.out.println(ctx.getText());

        int cnt = ctx.getChildCount();
//        System.out.println(cnt);
        for(int i=0; i<cnt; i++) {
            if(!(ctx.getChild(i) instanceof TerminalNode)) {
                RuleContext queryContext = (RuleContext) ctx.getChild(i);
                if (queryContext instanceof MySqlParser.QuerySpecificationContext) {
//                    System.out.println("QuerySpecific!!!");
                    return queryContext;
                } else {
//                visitChildren(ctx);
                }
            }
        }
        return null;
    }

    public Object searchQuerySpecific(RuleContext node) {
//        System.out.println("searchQuerySpecific");

        RuleContext result = (RuleContext) defaultResult();
//        System.out.println("1!");

        int n = node.getChildCount();
        for (int i=0; i<n; i++) {
//            System.out.println("2!");
            if (!shouldVisitNextChild(node, result)) {
                break;
            }
//            System.out.println("3!");

            ParseTree c = node.getChild(i);
//            System.out.println("4!");

            RuleContext childResult = (RuleContext) c.accept(this);
//            System.out.println("5!");

            result = (RuleContext) aggregateResult(result, childResult);

        }
        return result;
    }

    public ColumnInfo visitFullColumnName(MySqlParser.FullColumnNameContext fullColumnName) {
        ColumnInfo columnInfo = new ColumnInfo();
//        System.out.println("FullColumnNameContext");
        int cnt = fullColumnName.getChildCount();

        // 컬럼명
        if(cnt == 1) {
            if(fullColumnName.getChild(0) instanceof MySqlParser.UidContext) {
//                System.out.println("FullColumnNameExpression Uid-> " + fullColumnName.getChild(0).getText());
                columnInfo.setColumnLabel(fullColumnName.getChild(0).getText());
            }
        }
        // 테이블명.컬럼명
        else if (cnt == 2) {
            if(fullColumnName.getChild(0) instanceof MySqlParser.UidContext) {
//                System.out.println("FullColumnNameExpression ??Uid-> " + fullColumnName.getChild(0).getText());
                columnInfo.setTableName(fullColumnName.getChild(0).getText());
            }
            if(fullColumnName.getChild(1) instanceof MySqlParser.DottedIdContext) {
                String str = fullColumnName.getChild(1).getText();
//                System.out.println("FullColumnNameExpression ??DottedIdContext-> " + fullColumnName.getChild(1).getText());
                columnInfo.setColumnLabel(str.substring(1));
//                System.out.println("------------columnLabel");
//                System.out.println(columnInfo.getColumnLabel());
            }
        }
        return columnInfo;
    }

    public ColumnInfo visitSelectColumnElement(MySqlParser.SelectColumnElementContext selectColumnElementContext) {
        String selectStatement = "";
        ColumnInfo columnInfo = new ColumnInfo();
//        System.out.println("SelectColumnElementContext");
        int cntOfSelectElement = selectColumnElementContext.getChildCount();
//        System.out.println("SELECT > ");

        for(int j=0; j<cntOfSelectElement; j++) {
            // 일반 컬럼 명
            if(selectColumnElementContext.getChild(j) instanceof MySqlParser.FullColumnNameContext) {
                ColumnInfo column = visitFullColumnName((MySqlParser.FullColumnNameContext) selectColumnElementContext.getChild(j));

                selectStatement += selectColumnElementContext.getChild(j).getText();
                columnInfo.setTableName(column.getTableName());
                columnInfo.setColumnLabel(column.getColumnLabel());
            }
            // ex) AS
            else if (selectColumnElementContext.getChild(j) instanceof TerminalNodeImpl) {
                selectStatement += " " + selectColumnElementContext.getChild(j).getText() + " ";
            }
            // Uid
            else if (selectColumnElementContext.getChild(j) instanceof MySqlParser.UidContext) {
                selectStatement += selectColumnElementContext.getChild(j).getText();
//                System.out.println("SELECT 컴포넌트 > selectedColumn ColumnName Alias");
                columnInfo.setAlias(selectColumnElementContext.getChild(j).getText());
            } else {
                selectStatement += selectColumnElementContext.getChild(j).getText();
            }
        }

        return columnInfo;
    }
    public ColumnInfo visitFullColumnNameExpressionContext(MySqlParser.FullColumnNameExpressionAtomContext ctx) {
        ColumnInfo columnInfo = new ColumnInfo();
//        System.out.println("FullColumnNameExpression");
        ParserRuleContext fullColumnName = (ParserRuleContext) ctx.getChild(0);

        int cnt = fullColumnName.getChildCount();

        // 컬럼명
        if(cnt == 1) {
            if(fullColumnName.getChild(0) instanceof MySqlParser.UidContext) {
//                System.out.println("FullColumnNameExpression Uid-> " + fullColumnName.getChild(0).getText());
                columnInfo.setColumnLabel(fullColumnName.getChild(0).getText());
            }
        }
        // 테이블명.컬럼명
        else if (cnt == 2) {
            if(fullColumnName.getChild(0) instanceof MySqlParser.UidContext) {
//                System.out.println("FullColumnNameExpression Uid-> " + fullColumnName.getChild(0).getText());
                columnInfo.setTableName(fullColumnName.getChild(0).getText());
            }
            if(fullColumnName.getChild(1) instanceof MySqlParser.DottedIdContext) {
                String str = fullColumnName.getChild(1).getText();
//                System.out.println("FullColumnNameExpression DottedIdContext-> " + fullColumnName.getChild(1).getText());
                columnInfo.setColumnLabel(str.substring(1));
            }
        }
        return columnInfo;
    }
    // where 절
    public StepSqlComponent visitSimpleSelectContext(MySqlParser.SimpleSelectContext ctx) {
        String conditionStatement = "";
        StepSqlComponent stepSqlComponent = new StepSqlComponent();

        searchQuerySpecific((RuleContext) ctx);
        if (!sqlQueue.isEmpty()) {
            String temp = sqlQueue.peek();
            sqlQueue.remove();
//                    System.out.println("temp : " + temp);
            conditionStatement += temp;
        }
        if(!sqlComponentsQueue.isEmpty()) {
            stepSqlComponent = sqlComponentsQueue.peek();
            sqlComponentsQueue.remove();
        }

        stepSqlComponent.setSqlStatement(conditionStatement);
        return stepSqlComponent;
    }
    @Override
    public StepSqlComponent visitSubqueryExpressionAtom(MySqlParser.SubqueryExpressionAtomContext ctx) {
//        System.out.println("visitSubqueryExpressionAtom");
        String conditionStatement = "";
        StepSqlComponent stepSqlComponent = new StepSqlComponent();
        ArrayList<ColumnInfo> conditionColumns = new ArrayList<>();

        int cnt = ctx.getChildCount();
//        System.out.println("cnt : "+cnt);
        for(int i=0; i<cnt; i++) {
//            System.out.println(ctx.getChild(i).getClass());
            if(ctx.getChild(i) instanceof MySqlParser.SimpleSelectContext) {
//                System.out.println("subquery -> simpleselect");

                StepSqlComponent result = visitSimpleSelectContext((MySqlParser.SimpleSelectContext) ctx.getChild(i));
                conditionStatement += result.getSqlStatement();
                if(result.getConditionColumns()!=null) {
                    for(int j=0; j< result.getConditionColumns().size();j++){
                        conditionColumns.add(result.getConditionColumns().get(j));
                    }
                }
            }
            // terminal node ex) 괄호
            else {
//                System.out.println("subquery -> 이외에 경우");
                String str = ctx.getChild(i).getText();
                if(str.equals("(")) conditionStatement += " " + str;
                else if(str.equals(")")) conditionStatement += str + " ";
                else conditionStatement += " " + str + " ";
            }
        }


        stepSqlComponent.setSqlStatement(conditionStatement);
        stepSqlComponent.setConditionColumns(conditionColumns);
        return stepSqlComponent;
    }
    @Override
    public StepSqlComponent visitExistsExpressionAtom(MySqlParser.ExistsExpressionAtomContext ctx) {
//        System.out.println("visitExistsExpressionAtom");
        String conditionStatement = "";
        StepSqlComponent stepSqlComponent = new StepSqlComponent();
        ArrayList<ColumnInfo> conditionColumns = new ArrayList<>();

        int cnt = ctx.getChildCount();
//        System.out.println("cnt : "+cnt);
        for(int i=0; i<cnt; i++) {
//            System.out.println(ctx.getChild(i).getClass());
            if(ctx.getChild(i) instanceof MySqlParser.SimpleSelectContext) {
//                System.out.println("exits -> simpleselect");
                StepSqlComponent result = visitSimpleSelectContext((MySqlParser.SimpleSelectContext) ctx.getChild(i));
                conditionStatement += result.getSqlStatement();
                if(result.getConditionColumns()!=null) {
                    for (int j = 0; j < result.getConditionColumns().size(); j++) {
                        conditionColumns.add(result.getConditionColumns().get(j));
                    }
                }
            }
            // terminal node ex) 괄호
            else {
//                System.out.println("subquery -> 이외에 경우");
                String str = ctx.getChild(i).getText();
                if(str.equals("(")) conditionStatement += " " + str;
                else if(str.equals(")")) conditionStatement += str + " ";
                else conditionStatement += " " + str + " ";
            }
        }

        stepSqlComponent.setSqlStatement(conditionStatement);
        stepSqlComponent.setConditionColumns(conditionColumns);
        return stepSqlComponent;
    }
    public StepSqlComponent visitExpressionAtomPredicate(MySqlParser.ExpressionAtomPredicateContext expressionAtomPredicateContext) {
//        System.out.println("visitExpressionAtomPredicate -> " + expressionAtomPredicateContext.getText());

        String conditionStatement = "";
        StepSqlComponent stepSqlComponent = new StepSqlComponent();
        ArrayList<ColumnInfo> conditionColumns = new ArrayList<>();

        int childOfExpressionAtom = expressionAtomPredicateContext.getChildCount();
//        System.out.println("childOfExpressionAtom : " + childOfExpressionAtom);
        for (int i = 0; i < childOfExpressionAtom; i++) {
//            System.out.println(expressionAtomPredicateContext.getChild(i).getClass());
            if(expressionAtomPredicateContext.getChild(i) instanceof MySqlParser.SimpleSelectContext) {
                StepSqlComponent conditionResult = visitSimpleSelectContext((MySqlParser.SimpleSelectContext) expressionAtomPredicateContext.getChild(i));
                conditionStatement += conditionResult.getSqlStatement();
                if(conditionResult.getConditionColumns()!=null) {
                    for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                        conditionColumns.add(conditionResult.getConditionColumns().get(j));
                    }
                }

            }
            else if (expressionAtomPredicateContext.getChild(i) instanceof MySqlParser.ExpressionAtomPredicateContext) {
                StepSqlComponent conditionResult = visitExpressionAtomPredicate((MySqlParser.ExpressionAtomPredicateContext) expressionAtomPredicateContext.getChild(i));
                conditionStatement += conditionResult.getSqlStatement();
                if(conditionResult.getConditionColumns()!=null) {
                    for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                        conditionColumns.add(conditionResult.getConditionColumns().get(j));
                    }
                }
            }
            else if (expressionAtomPredicateContext.getChild(i) instanceof MySqlParser.ExistsExpressionAtomContext) {
                StepSqlComponent conditionResult = visitExistsExpressionAtom((MySqlParser.ExistsExpressionAtomContext) expressionAtomPredicateContext.getChild(i));
                conditionStatement += conditionResult.getSqlStatement();
                if(conditionResult.getConditionColumns()!=null) {
                    for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                        conditionColumns.add(conditionResult.getConditionColumns().get(j));
                    }
                }
            }
            else if (expressionAtomPredicateContext.getChild(i) instanceof MySqlParser.SubqueryExpressionAtomContext) {
                StepSqlComponent conditionResult = visitSubqueryExpressionAtom((MySqlParser.SubqueryExpressionAtomContext) expressionAtomPredicateContext.getChild(i));
                conditionStatement += conditionResult.getSqlStatement();
                if(conditionResult.getConditionColumns()!=null) {
                    for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                        conditionColumns.add(conditionResult.getConditionColumns().get(j));
                    }
                }
            }
            else if (expressionAtomPredicateContext.getChild(i) instanceof MySqlParser.FullColumnNameExpressionAtomContext) {
//                System.out.println("FullColumnNameExpressionAtomContext");
                conditionStatement += expressionAtomPredicateContext.getChild(i).getText();


                ColumnInfo columnInfo = visitFullColumnNameExpressionContext((MySqlParser.FullColumnNameExpressionAtomContext) expressionAtomPredicateContext.getChild(i));
//                System.out.println("WHERE 컴포넌트 > column info ");
//                System.out.println("columnInfo");
//                System.out.println("getTableName : "+columnInfo.getTableName());
//                System.out.println("getColumnLabel : "+columnInfo.getColumnLabel());
                conditionColumns.add(columnInfo);
            }
            // terminal node
            else {
//                System.out.println("expressionAtom > simpleSelect 이외에");
                String str = expressionAtomPredicateContext.getChild(i).getText();
                if(str.equals("(")) conditionStatement += str + " ";
                else if(str.equals(")")) conditionStatement += " " + str;
                else conditionStatement += " " + str + " ";
            }
        }
//        System.out.println("conditionStatement -> " + conditionStatement);
        stepSqlComponent.setSqlStatement(conditionStatement);
        stepSqlComponent.setConditionColumns(conditionColumns);

        return stepSqlComponent;

    }


    public StepSqlComponent visitBinaryComparisonPredicate(MySqlParser.BinaryComparisonPredicateContext binaryComparisonPredicateContext) {
//        System.out.println("visitBinaryComparisonPredicate -> " + binaryComparisonPredicateContext.getText());

        String compareStatement = "";
        StepSqlComponent stepSqlComponent = new StepSqlComponent();
        ArrayList<ColumnInfo> conditionColumns = new ArrayList<>();
        ColumnInfo columnInfo = new ColumnInfo();

        int childOfBinary = binaryComparisonPredicateContext.getChildCount();
//        System.out.println("childOfBinary : " + childOfBinary);
        for (int i = 0; i < childOfBinary; i++) {
//            System.out.println(binaryComparisonPredicateContext.getChild(i).getClass());
            // 1) InPredicate
            // A , B
            if ((binaryComparisonPredicateContext.getChild(i) instanceof MySqlParser.InPredicateContext)) {
                StepSqlComponent conditionResult = visitInPredicateContext((MySqlParser.InPredicateContext) binaryComparisonPredicateContext.getChild(i));
                compareStatement += conditionResult.getSqlStatement();
                if(conditionResult.getConditionColumns()!=null) {
                    for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                        conditionColumns.add(conditionResult.getConditionColumns().get(j));
                    }
                }
            }
            // 2) ExpressionAtomPredicate
            else if (binaryComparisonPredicateContext.getChild(i) instanceof MySqlParser.ExpressionAtomPredicateContext) {
                StepSqlComponent conditionResult = visitExpressionAtomPredicate((MySqlParser.ExpressionAtomPredicateContext) binaryComparisonPredicateContext.getChild(i));

//                System.out.println("ExpressionAtomPredicateContext 컴포넌트");


                compareStatement += conditionResult.getSqlStatement();
                if(conditionResult.getConditionColumns()!=null) {
                    for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                        conditionColumns.add(conditionResult.getConditionColumns().get(j));
                    }
                }

            }
            // Operator 연산자
            else if (binaryComparisonPredicateContext.getChild(i) instanceof MySqlParser.ComparisonOperatorContext) {
                compareStatement += " " + binaryComparisonPredicateContext.getChild(i).getText() + " ";
            }
            else {
                compareStatement += " " + binaryComparisonPredicateContext.getChild(i).getText();
            }
        }

//        System.out.println("comperStatement -> " + compareStatement);
        stepSqlComponent.setSqlStatement(compareStatement);
        stepSqlComponent.setConditionColumns(conditionColumns);
        return stepSqlComponent;
    }
    public StepSqlComponent visitInPredicateContext(MySqlParser.InPredicateContext inPredicateContext) {

        String conditionStatement = "";
        StepSqlComponent stepSqlComponent = new StepSqlComponent();
        ArrayList<ColumnInfo> conditionColumns = new ArrayList<>();
//        System.out.println("visitInPredicateContext : " + inPredicateContext.getText());

        int cntOfchildPredicate = inPredicateContext.getChildCount();
//        System.out.println("cntOfChildPredicate : "+cntOfchildPredicate);
        for (int i = 0; i < cntOfchildPredicate; i++) {
            // EXIST -> 바로 아래 자식이 simpleSelect
            if (inPredicateContext.getChild(i) instanceof MySqlParser.ExistsExpressionAtomContext) {
                StepSqlComponent conditionResult = visitExistsExpressionAtom((MySqlParser.ExistsExpressionAtomContext) inPredicateContext.getChild(i));
                conditionStatement += conditionResult.getSqlStatement();
                if(conditionResult.getConditionColumns()!=null) {
                    for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                        conditionColumns.add(conditionResult.getConditionColumns().get(j));
                    }
                }
            }
            // SUBQUERY -> 바로 아래 자식이 simpleSelect
            else if (inPredicateContext.getChild(i) instanceof MySqlParser.SubqueryExpressionAtomContext) {
                StepSqlComponent conditionResult = visitSubqueryExpressionAtom((MySqlParser.SubqueryExpressionAtomContext) inPredicateContext.getChild(i));
                conditionStatement += conditionResult.getSqlStatement();
                if(conditionResult.getConditionColumns()!=null) {
                    for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                        conditionColumns.add(conditionResult.getConditionColumns().get(j));
                    }
                }
            }
            // 바로 simpleSelect 인 경우 -> 서브쿼리 찾기
            else if (inPredicateContext.getChild(i) instanceof MySqlParser.SimpleSelectContext) {
//                System.out.println("SimpleSelectContext : " + inPredicateContext.getChild(j) .getText());
                StepSqlComponent conditionResult = visitSimpleSelectContext((MySqlParser.SimpleSelectContext) inPredicateContext.getChild(i));
                conditionStatement += conditionResult.getSqlStatement();
                if(conditionResult.getConditionColumns()!=null) {
                    for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                        conditionColumns.add(conditionResult.getConditionColumns().get(j));
                    }
                }
            }
            // fullColumnNameExpression OR TerminalNode
            else {
//                System.out.println(inPredicateContext.getChild(j).getClass() + " -> " + inPredicateContext.getChild(j).getText());
                String str = inPredicateContext.getChild(i).getText();
                if(str.equals("(")) conditionStatement += " " + str;
                else if(str.equals(")")) conditionStatement += str + " ";
                else conditionStatement += " " + inPredicateContext.getChild(i).getText() + " ";
            }
        }
        stepSqlComponent.setSqlStatement(conditionStatement);
        stepSqlComponent.setConditionColumns(conditionColumns);
        return stepSqlComponent;
    }
    public StepSqlComponent visitPredicateContext(MySqlParser.PredicateExpressionContext predicateExpressionContext) {
//        System.out.println("PredicateExpressionContext : " + predicateExpressionContext.getText());
        String conditionStatement = "";
        StepSqlComponent stepSqlComponent = new StepSqlComponent();
        ArrayList<ColumnInfo> conditionColumns = new ArrayList<>();

//        System.out.println(predicateExpressionContext.getChild(0).getClass());

        // 1. BinaryComparisonPredicate (A 연산자 B)
        if (predicateExpressionContext.getChild(0) instanceof MySqlParser.BinaryComparisonPredicateContext) {
//            System.out.println("BinaryComparisonPredicateContext");
            ParserRuleContext binaryComparisonPredicateContext = (ParserRuleContext) predicateExpressionContext.getChild(0);

            StepSqlComponent conditionResult = visitBinaryComparisonPredicate((MySqlParser.BinaryComparisonPredicateContext) binaryComparisonPredicateContext);

//            System.out.println("Binary 컴포넌트");
//            System.out.println(conditionResult.getConditionColumns().get(0).getTableName());
//            System.out.println(conditionResult.getConditionColumns().get(0).getColumnLabel());
            conditionStatement += conditionResult.getSqlStatement();
            if(conditionResult.getConditionColumns()!=null) {
                for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                    conditionColumns.add(conditionResult.getConditionColumns().get(j));
                }
            }
        }
        // 2. SQL연산자 - IN
        else if (predicateExpressionContext.getChild(0) instanceof MySqlParser.InPredicateContext) {
//            System.out.println("InPredicateContext");
            StepSqlComponent conditionResult = visitInPredicateContext((MySqlParser.InPredicateContext) predicateExpressionContext.getChild(0));
            conditionStatement += conditionResult.getSqlStatement();
            if(conditionResult.getConditionColumns()!=null) {
                for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                    conditionColumns.add(conditionResult.getConditionColumns().get(j));
                }
            }
        }
        //
        else if(predicateExpressionContext.getChild(0) instanceof MySqlParser.ExpressionAtomPredicateContext) {
//            System.out.println("PredicateExpressionContext");
            StepSqlComponent conditionResult = visitExpressionAtomPredicate((MySqlParser.ExpressionAtomPredicateContext) predicateExpressionContext.getChild(0));
            conditionStatement += conditionResult.getSqlStatement();
            if(conditionResult.getConditionColumns()!=null) {
                for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                    conditionColumns.add(conditionResult.getConditionColumns().get(j));
                }
            }
        }
        // 3. SQL연산자 - Like
        else if (predicateExpressionContext.getChild(0) instanceof MySqlParser.LikePredicateContext) {
//            System.out.println("LikePredi퍄cateContext : " + predicateExpressionContext.getChild(0).getText());
            ParserRuleContext ChildOflikePredicate = (ParserRuleContext) predicateExpressionContext.getChild(0);
            int cntOfchildLikePredicate = ChildOflikePredicate.getChildCount();
            for (int i = 0; i < cntOfchildLikePredicate; i++) {

                if (ChildOflikePredicate instanceof MySqlParser.InPredicateContext) {
                    StepSqlComponent conditionResult = visitInPredicateContext((MySqlParser.InPredicateContext) ChildOflikePredicate);
                    conditionStatement += conditionResult.getSqlStatement();
                    if(conditionResult.getConditionColumns()!=null) {
                        for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                            conditionColumns.add(conditionResult.getConditionColumns().get(j));
                        }
                    }
                }
                else {
                    conditionStatement += " " + ChildOflikePredicate.getText() + " ";
                }
            }
        }
        // 4. SQL연산자 - Between
        else if (predicateExpressionContext.getChild(0) instanceof MySqlParser.BetweenPredicateContext) {
//            System.out.println("BetweenPredicateContext : " + predicateExpressionContext.getChild(0).getText());
            int cntOfchildBetweenPredicate = predicateExpressionContext.getChild(0).getChildCount();
            for (int i = 0; i < cntOfchildBetweenPredicate; i++) {
                if (predicateExpressionContext.getChild(0).getChild(i) instanceof MySqlParser.InPredicateContext) {
                    ParserRuleContext ChildOfBetweenPredicate = (ParserRuleContext) predicateExpressionContext.getChild(0).getChild(i);
                    StepSqlComponent conditionResult = visitInPredicateContext((MySqlParser.InPredicateContext) ChildOfBetweenPredicate);
                    conditionStatement += conditionResult.getSqlStatement();
                    if(conditionResult.getConditionColumns()!=null) {
                        for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                            conditionColumns.add(conditionResult.getConditionColumns().get(j));
                            System.out.println("columnLabel");
                            System.out.println(conditionResult.getConditionColumns().get(j).getColumnLabel());
                        }
                    }
                }
                else {
                    conditionStatement += " " + predicateExpressionContext.getChild(0).getChild(i).getText() + " ";
                }
            }
        }
        // 5. SQL연산자 - Null
        else if (predicateExpressionContext.getChild(0) instanceof MySqlParser.IsNullPredicateContext) {
//            System.out.println("IsNullPredicateContext : " + predicateExpressionContext.getChild(0).getText());
            int cntOfchildNullPredicate = predicateExpressionContext.getChild(0).getChildCount();
            for (int i = 0; i < cntOfchildNullPredicate; i++) {
                ParserRuleContext ChildOfNullPredicate = (ParserRuleContext) predicateExpressionContext.getChild(0).getChild(i);
                if (ChildOfNullPredicate instanceof MySqlParser.InPredicateContext) {
                    StepSqlComponent conditionResult = visitInPredicateContext((MySqlParser.InPredicateContext) ChildOfNullPredicate);
                    conditionStatement += conditionResult.getSqlStatement();
                    if(conditionResult.getConditionColumns()!=null) {
                        for (int j = 0; j < conditionResult.getConditionColumns().size(); j++) {
                            conditionColumns.add(conditionResult.getConditionColumns().get(j));
                        }
                    }
                }
                else {
                    conditionStatement += " " + ChildOfNullPredicate.getText() + " ";
                }
            }

        }
        else {
//            System.out.println("이외에 경우가 있나? 일단 붙이기");
            conditionStatement += predicateExpressionContext.getChild(0).getText();
        }

        stepSqlComponent.setSqlStatement(conditionStatement);
        stepSqlComponent.setConditionColumns(conditionColumns);
//        for(int i=0;i<conditionColumns.size();i++) {
//            System.out.println(conditionColumns.get(i).getTableName());
//            System.out.println(conditionColumns.get(i).getColumnLabel());
//
//        }

//        System.out.println("--> conditionStatement -> " + conditionStatement);
        return stepSqlComponent;
    }
    // from 절
    public StepSqlComponent visitSubqueryTableItem(MySqlParser.SubqueryTableItemContext subqueryTableItemContext) {
        int cntOfChildSubqueryTable = subqueryTableItemContext.getChildCount();
        String tableStatement = "";
        StepSqlComponent stepSqlComponent = new StepSqlComponent();
        ArrayList<TableInfo> usedTables = new ArrayList<>();
        TableInfo tableInfo = new TableInfo();

        for(int i=0; i<cntOfChildSubqueryTable; i++) {

            // (서브쿼리)
            if (subqueryTableItemContext.getChild(i) instanceof MySqlParser.ParenthesisSelectContext) {
                StepSqlComponent tableResult = visitParenthesisSelectContext((MySqlParser.ParenthesisSelectContext) subqueryTableItemContext.getChild(i));
//                System.out.println("visitSubqueryTableItem > ParenthesisSelectContext");

                tableStatement += "(" + tableResult.getSqlStatement() + ")";
                tableInfo.setTableName(tableResult.getSqlStatement());

            }
            // AS
            else if(subqueryTableItemContext.getChild(i) instanceof TerminalNodeImpl) {
                tableStatement += " " + subqueryTableItemContext.getChild(i).getText();
            }
            // alias
            else if (subqueryTableItemContext.getChild(i) instanceof MySqlParser.UidContext) {
                tableStatement += " " + subqueryTableItemContext.getChild(i).getText();
                tableInfo.setAlias(subqueryTableItemContext.getChild(i).getText());
            }
            else {
                tableStatement += " " + subqueryTableItemContext.getChild(i).getText();
            }
        }

        stepSqlComponent.setSqlStatement(tableStatement);
        usedTables.add(tableInfo);
        stepSqlComponent.setTables(usedTables);
        return stepSqlComponent;
    }

    public StepSqlComponent visitQueryExpression(MySqlParser.QueryExpressionContext query) {
        String Statement = "";
        StepSqlComponent stepSqlComponent = new StepSqlComponent();

//        System.out.println("visitSubqueryTableItem > ParenthesisSelectContext");
        searchQuerySpecific((RuleContext) query);
        if (!sqlQueue.isEmpty()) {
            String temp = sqlQueue.peek();
            sqlQueue.remove();
//                    System.out.println("temp : " + temp);
            Statement += temp;
        }
        else {
            Statement += " " + query.getChild(0).getText();
        }

        if(!sqlComponentsQueue.isEmpty()) {
            stepSqlComponent = sqlComponentsQueue.peek();
            sqlComponentsQueue.remove();
        }

        stepSqlComponent.setSqlStatement(Statement);
        return stepSqlComponent;
    }
    public StepSqlComponent visitParenthesisSelectContext(MySqlParser.ParenthesisSelectContext parenthesisSelect) {
        String tableStatement = "";
        StepSqlComponent stepSqlComponent = new StepSqlComponent();
        ParserRuleContext queryExpression = (ParserRuleContext) parenthesisSelect.getChild(0);

        searchQuerySpecific((RuleContext) queryExpression);
        if (!sqlQueue.isEmpty()) {
            String temp = sqlQueue.peek();
            sqlQueue.remove();
//                    System.out.println("temp : " + temp);
            tableStatement += temp;
        }
        else {
            tableStatement += " " + parenthesisSelect.getChild(0).getText();
        }

        if(!sqlComponentsQueue.isEmpty()) {
            stepSqlComponent = sqlComponentsQueue.peek();
            sqlComponentsQueue.remove();
        }

        stepSqlComponent.setSqlStatement(tableStatement);
        return stepSqlComponent;
    }


    public TableInfo visitAtomTableItem(MySqlParser.AtomTableItemContext ctx) {
        TableInfo tableInfo = new TableInfo();
        int cnt = ctx.getChildCount();

        // 테이블명
        if(cnt == 1) {
            tableInfo.setTableName(ctx.getChild(0).getText());
        }
        else if(cnt == 2) {
            tableInfo.setTableName(ctx.getChild(0).getText());
            tableInfo.setAlias(ctx.getChild(1).getText());
        }
        // 테이블명 AS alias
        else {
            tableInfo.setTableName(ctx.getChild(0).getText());
            tableInfo.setAlias(ctx.getChild(2).getText());
        }
        return tableInfo;
    }
    @Override public Object visitQuerySpecification(MySqlParser.QuerySpecificationContext ctx) {
//        System.out.println("visitQuerySpecification");
        String sql = ctx.getText();
//        System.out.println(sql);

        // sql문
        String sqlStatement = "";
        String fromStatement = "";
        String conditionStatement = "";
        String selectStatement = "";
        // 해당 sql문의 컴포넌트
        StepSqlComponent stepSqlComponent = new StepSqlComponent();
        ArrayList<TableInfo> usedTables = new ArrayList<>();
        ArrayList<ColumnInfo> selectedColumns = new ArrayList<>();
        ArrayList<ColumnInfo> conditionColumns = new ArrayList<>();
        ArrayList<String> conditions = new ArrayList<>();
        Boolean joinExists = null;
        ArrayList<ColumnInfo> joinedColumns = new ArrayList<>();
        ArrayList<String> on = new ArrayList<>();


        // 1. fromClause
        // FROM
        if(ctx.fromClause() !=null) {
            String from = ctx.fromClause().FROM().getText();
            fromStatement += " " + from + " ";

            // tableSources 요소 순회
            int tableCnt = ctx.fromClause().tableSources().getChildCount();
//        System.out.println("tableCnt : " + tableCnt);
            for (int i = 0; i < tableCnt; i++) {

                String str = ctx.fromClause().tableSources().getChild(i).getText();
//            System.out.println("tableSources = " + str);

                // tableSourceBase 요소일때
                // ex) 테이블 명 / 테이블 명 + alis -> AtomTableItem
                // ex) 서브쿼리 -> SubqueryTableItemContext
                // ex) 조인
                if (ctx.fromClause().tableSources().getChild(i) instanceof MySqlParser.TableSourceBaseContext) {
                    ParserRuleContext tableSourceBaseContext = (ParserRuleContext) ctx.fromClause().tableSources().getChild(i);
                    int childCountOfTableSourceBase = tableSourceBaseContext.getChildCount();
//                System.out.println("childCountOfTableSourceBase : " + childCountOfTableSourceBase);
                    // tableSourceBase에서 분기
                    for (int j = 0; j < childCountOfTableSourceBase; j++) {
                        // tableSourceBaseContext 의 childContext를 분석
                        ParserRuleContext childOfTableSourceBaseContext = (ParserRuleContext) tableSourceBaseContext.getChild(j);

                        // 말단 노드일때
                        if (tableSourceBaseContext.getChild(j) instanceof TerminalNodeImpl) {
//                        System.out.println("childOfTableSourceBaseContext.getChild(" + j + ") = TerminalNodeImpl");
                            fromStatement += childOfTableSourceBaseContext.getText() + " ";
                            continue;
                        }
                        // 일반 테이블명 -> tableName
                        // 일반 테이블명 + alias -> tableName alias
                        else if (tableSourceBaseContext.getChild(j) instanceof MySqlParser.AtomTableItemContext) {
                            TableInfo tableInfo = visitAtomTableItem((MySqlParser.AtomTableItemContext) tableSourceBaseContext.getChild(j));

//                        System.out.println("childOfTableSourceBaseContext.getChild(" + j + ") = AtomTableItemContext");
                            ParserRuleContext atomTableContext = (ParserRuleContext) childOfTableSourceBaseContext;
                            int cntTablebranch = atomTableContext.getChildCount();
//                        System.out.println("cntTablebranch " + cntTablebranch);

                            for (int h = 0; h < cntTablebranch; h++) {
                                // tableName
                                if (h == 0) {
                                    String tableName = childOfTableSourceBaseContext.getChild(h).getText();
                                    fromStatement += tableName;
                                } else if (h == 1) {
                                    String as = childOfTableSourceBaseContext.getChild(h).getText();
                                    fromStatement += " " + as + " ";
                                } else {
                                    String alias = childOfTableSourceBaseContext.getChild(h).getText();
                                    fromStatement += " " + alias + " ";
                                }
                            }
                            usedTables.add(tableInfo);

                        }
                        // 서브쿼리가 있는 경우
                        else if (childOfTableSourceBaseContext instanceof MySqlParser.SubqueryTableItemContext) {
                            StepSqlComponent fromResult = visitSubqueryTableItem((MySqlParser.SubqueryTableItemContext) childOfTableSourceBaseContext);
                            fromStatement += fromResult.getSqlStatement();
                            if (fromResult.getTables() != null) {
                                for (int k = 0; k < fromResult.getTables().size(); k++) {
                                    usedTables.add(fromResult.getTables().get(k));
                                }
                            }
                        }
                        // JOIN이 있는 경우
                        else if ((childOfTableSourceBaseContext instanceof MySqlParser.OuterJoinContext)
                                || (childOfTableSourceBaseContext instanceof MySqlParser.InnerJoinContext)
                                || (childOfTableSourceBaseContext instanceof MySqlParser.NaturalJoinContext)) {
                            stepSqlComponent.setJoinExists(true);
                            int cntOfJoin = childOfTableSourceBaseContext.getChildCount();
//                        System.out.println("cntOfJoin : " + cntOfJoin);
                            for (int k = 0; k < cntOfJoin; k++) {
//                            System.out.println(childOfTableSourceBaseContext.getChild(k).getClass());
                                // 말단 노드일 때 ex) JOIN, LEFT JOIN, RIGHT JOIN, CROSS JOIN 등
                                if (childOfTableSourceBaseContext.getChild(k) instanceof TerminalNodeImpl) {
                                    fromStatement += " " + childOfTableSourceBaseContext.getChild(k).getText() + " ";
                                    continue;
                                }

                                // 테이블 명
                                // 테이블 명 + alias
                                if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.AtomTableItemContext) {
                                    TableInfo tableInfo = visitAtomTableItem((MySqlParser.AtomTableItemContext) childOfTableSourceBaseContext.getChild(k));

                                    ParserRuleContext tableNameContext = (ParserRuleContext) childOfTableSourceBaseContext.getChild(k);
                                    int cntTablebranch = tableNameContext.getChildCount();
                                    for (int h = 0; h < cntTablebranch; h++) {
                                        if (h == 0) {
                                            fromStatement += tableNameContext.getChild(h).getText();
                                        } else {
                                            fromStatement += " " + tableNameContext.getChild(h).getText();
                                        }
                                    }
                                    usedTables.add(tableInfo);
                                } else if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.SubqueryTableItemContext) {
                                    StepSqlComponent fromResult = visitSubqueryTableItem((MySqlParser.SubqueryTableItemContext) childOfTableSourceBaseContext.getChild(k));
                                    fromStatement += fromResult.getSqlStatement();

                                    if (fromResult.getTables() != null) {
                                        for (int h = 0; h < fromResult.getTables().size(); h++) {
                                            usedTables.add(fromResult.getTables().get(h));
                                        }
                                    }
                                }
                                // JOIN 조건절
                                else if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.JoinSpecContext) {
                                    ParserRuleContext joinSpecContext = (ParserRuleContext) childOfTableSourceBaseContext.getChild(k);
                                    int cntJoinSpec = joinSpecContext.getChildCount();
                                    String onStatement = "";
                                    for (int h = 0; h < cntJoinSpec; h++) {
                                        // 말단일때 ex) ON
                                        if (joinSpecContext.getChild(h) instanceof TerminalNodeImpl) {
                                            onStatement += " " + joinSpecContext.getChild(h).getText() + " ";
                                        }
                                        // 조건절 내용
                                        else if (joinSpecContext.getChild(h) instanceof MySqlParser.PredicateExpressionContext) {
                                            StepSqlComponent fromResult = visitPredicateContext((MySqlParser.PredicateExpressionContext) joinSpecContext.getChild(h));
                                            onStatement += fromResult.getSqlStatement();
                                            if (fromResult.getConditionColumns() != null) {
                                                for (int l = 0; l < fromResult.getConditionColumns().size(); l++) {
                                                    joinedColumns.add(fromResult.getConditionColumns().get(l));
                                                }
                                            }

                                        } else
                                            onStatement += joinSpecContext.getChild(h).getText();
                                    }
                                    fromStatement += onStatement;

                                    // 연속된 공백 -> 하나의 공백으로
                                    onStatement = onStatement.replaceAll("\\s+", " ");
                                    // 앞 뒤 공백 제거
                                    onStatement = onStatement.strip();
                                    on.add(onStatement);

                                    stepSqlComponent.setJoinedColumns(joinedColumns);
                                    stepSqlComponent.setOn(on);
                                } else {
                                    fromStatement += childOfTableSourceBaseContext.getChild(k).getText();
                                }
                            }
                        }
                    }

                }
                // tableSourceBase 요소가 아닐때
                // ex) COMMA: ,
                else {

                    fromStatement += str + " ";
                }
//            System.out.println("middle fromStatement : " + fromStatement);

            }

            stepSqlComponent.setTables(usedTables);

            // 2. condition 절

            // WHERE절  있을 경우
            if (ctx.fromClause().WHERE() != null) {
//            System.out.println("WHERE!!!!!!!!!!!!!!!!!");

                conditionStatement += " " + ctx.fromClause().WHERE() + " ";
                // expression 있을 경우
                if (ctx.fromClause().expression() != null) {
//                System.out.println("expression!!!!!!!!!!!!!!!!!");
//                System.out.println(ctx.fromClause().expression().getClass());

                    int expressionCnt = ctx.fromClause().expression().getChildCount();

                    if (ctx.fromClause().expression() instanceof MySqlParser.LogicalExpressionContext) {
//                    System.out.println("LOGICAL");
//                    System.out.println("expressionCnt : " + expressionCnt);
                        for (int l = 0; l < expressionCnt; l++) {

//                        System.out.println(ctx.fromClause().expression().getChild(l).getClass());
                            if (ctx.fromClause().expression().getChild(l) instanceof MySqlParser.PredicateExpressionContext)
                            {
                                ParserRuleContext PredicateExpressionContext = (ParserRuleContext) ctx.fromClause().expression().getChild(l);
                                StepSqlComponent conditionComponent = visitPredicateContext((MySqlParser.PredicateExpressionContext) PredicateExpressionContext);

//                            System.out.println("predicate 컴포넌트 ");
                                conditionStatement += conditionComponent.getSqlStatement();
                                if(conditionComponent.getConditionColumns()!=null) {
                                    for (int j = 0; j < conditionComponent.getConditionColumns().size(); j++) {
                                        conditionColumns.add(conditionComponent.getConditionColumns().get(j));

                                    }
                                }
                            }
                            else {
//                            System.out.println("Logical Operator");
                                conditionStatement += " " + ctx.fromClause().expression().getChild(l).getText() + " ";
                            }
//                        System.out.println("middle conditionStatement : " + conditionStatement);

                        }
                    }
                    else {
//                    System.out.println("NOT LOGICAL");
//                    System.out.println("expressionCnt : " + expressionCnt);
//                    System.out.println(ctx.fromClause().expression().getClass());

                        if (ctx.fromClause().expression() instanceof MySqlParser.PredicateExpressionContext)
                        {
                            StepSqlComponent conditionComponent = visitPredicateContext((MySqlParser.PredicateExpressionContext) ctx.fromClause().expression());

//                        System.out.println("predicate 컴포넌트 ");
                            conditionStatement += conditionComponent.getSqlStatement();
                            if(conditionComponent.getConditionColumns()!=null) {
                                for (int j = 0; j < conditionComponent.getConditionColumns().size(); j++) {
                                    conditionColumns.add(conditionComponent.getConditionColumns().get(j));

//                            System.out.println(conditionComponent.getConditionColumns().get(j).getTableName());
//                            System.out.println(conditionComponent.getConditionColumns().get(j).getColumnLabel());
                                }
                            }
//                        System.out.println("middle conditionStatement -> "+ conditionStatement);

                        }
                        else {
//                        System.out.println("이건 뭐?");
                            conditionStatement += " " + ctx.fromClause().expression().getText() + " ";
                        }
//                    System.out.println("middle conditionStatement : " + conditionStatement);

                    }
//                System.out.println();
//                System.out.println(conditionStatement);
//                System.out.println();

                    // 연속된 공백 -> 하나의 공백으로
                    conditionStatement = conditionStatement.replaceAll("\\s+", " ");
                    // 앞 뒤 공백 제거
                    conditionStatement = conditionStatement.strip();
                    conditions.add(conditionStatement);
                    stepSqlComponent.setConditions(conditions);
                    stepSqlComponent.setConditionColumns(conditionColumns);

                }


            }

        }






        // 3. SELECT
        String selectKeyword = ctx.SELECT().getText();
        selectStatement += selectKeyword + " ";
        stepSqlComponent.setKeyword("SELECT");
        if(ctx.selectSpec().size() != 0) {
//            System.out.println("selectSpec");

            for(int i=0; i<ctx.selectSpec().size(); i++) {
//                System.out.println(ctx.selectSpec().get(i).getText());
                selectStatement += " " + ctx.selectSpec().get(i).getText() + " ";
            }
        }

        // selectElements
        if(ctx.selectElements() != null) {
            int columnCnt = ctx.selectElements().getChildCount();
//        System.out.println("columnCnt : " + columnCnt);
            for(int i=0; i < columnCnt; i++) {
//                System.out.println(ctx.selectElements().getChild(i).getClass());
//                System.out.println(ctx.selectElements().getChild(i).getText());
                String str = ctx.selectElements().getChild(i).getText();
                // selectElement요소에서
                // 서브쿼리 만났을 때!!!
                if(ctx.selectElements().getChild(i) instanceof MySqlParser.SelectExpressionElementContext) {
                    ParserRuleContext selectExpressionContext = (ParserRuleContext) ctx.selectElements().getChild(i);

                    // (서브쿼리) AS alias -> 3개의 child
                    // 1 : 서브쿼리 -> predicate Expression
                    // 2 : AS -> TerminalNodeImple
                    // 3 : alias -> uid (터미널 아님)
                    int cnt1 = selectExpressionContext.getChildCount();
//                    System.out.println("cnt 1 : " + cnt1);

                    for(int j=0; j< cnt1 ; j++) {
//                        System.out.println();
//                        System.out.println(selectExpressionContext.getChild(j).getClass());
//                        System.out.println();
                        ColumnInfo columnInfo = new ColumnInfo();
                        // 서브 쿼리
                        if (selectExpressionContext.getChild(j) instanceof MySqlParser.PredicateExpressionContext) {
                            StepSqlComponent selectResult = visitPredicateContext((MySqlParser.PredicateExpressionContext) selectExpressionContext.getChild(j));

                            selectStatement += selectResult.getSqlStatement();
                            columnInfo.setColumnLabel(selectResult.getSqlStatement());
                        }
                        // 말단 노드
                        // ex) AS
                        else if (selectExpressionContext.getChild(j) instanceof TerminalNodeImpl) {
                            selectStatement += " " + selectExpressionContext.getChild(j).getText() + " ";
                            continue;
                        }
                        // Alias
                        else if (selectExpressionContext.getChild(j) instanceof MySqlParser.UidContext) {
                            selectStatement += selectExpressionContext.getChild(j).getText() + " ";
                            columnInfo.setAlias(selectExpressionContext.getChild(j).getText());
                        }
                        else {
//                            System.out.println("else");
                            selectStatement += " " + selectExpressionContext.getChild(j).getText() + " ";
                            columnInfo.setColumnLabel(selectExpressionContext.getChild(j).getText());
                        }
                        selectedColumns.add(columnInfo);

                    }
                }
                // 일반 컬럼 정보
                else if (ctx.selectElements().getChild(i) instanceof MySqlParser.SelectColumnElementContext) {
                    ParserRuleContext selectColumnElementContext = (ParserRuleContext) ctx.selectElements().getChild(i);
//                ColumnInfo columnInfo = visitSelectColumnElement((MySqlParser.SelectColumnElementContext) selectColumnElementContext);

                    ColumnInfo columnInfo = new ColumnInfo();
                    int cntOfSelectElement = selectColumnElementContext.getChildCount();
//                System.out.println("SELECT > ");

                    for(int j=0; j<cntOfSelectElement; j++) {
                        // 일반 컬럼 명
                        if(selectColumnElementContext.getChild(j) instanceof MySqlParser.FullColumnNameContext) {
                            ColumnInfo column = visitFullColumnName((MySqlParser.FullColumnNameContext) selectColumnElementContext.getChild(j));

                            selectStatement += selectColumnElementContext.getChild(j).getText();
                            columnInfo.setTableName(column.getTableName());
                            columnInfo.setColumnLabel(column.getColumnLabel());
                        }
                        // ex) AS
                        else if (selectColumnElementContext.getChild(j) instanceof TerminalNodeImpl) {
                            selectStatement += " " + selectColumnElementContext.getChild(j).getText() + " ";
                        }
                        // Uid
                        else if (selectColumnElementContext.getChild(j) instanceof MySqlParser.UidContext) {
                            selectStatement += selectColumnElementContext.getChild(j).getText();
//                        System.out.println("SELECT 컴포넌트 > selectedColumn ColumnName Alias");
                            columnInfo.setAlias(selectColumnElementContext.getChild(j).getText());
                        } else {
                            selectStatement += selectColumnElementContext.getChild(j).getText();
                            columnInfo.setColumnLabel(selectColumnElementContext.getChild(j).getText());
                        }
                    }
                    selectedColumns.add(columnInfo);
                }
                // terminalNode?
                // ex) COMMA ,
                else if (ctx.selectElements().getChild(i) instanceof TerminalNodeImpl){
//                System.out.println("SELECT TERMINAL? -> " + ctx.selectElements().getChild(i).getText());
                    if(str.equals("*")) {
                        ColumnInfo columnInfo = new ColumnInfo();
                        columnInfo.setColumnLabel(str);
                        selectedColumns.add(columnInfo);
                    }
                    selectStatement += str + " ";
                }
                else if (ctx.selectElements().getChild(i) instanceof MySqlParser.SelectFunctionElementContext) {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setColumnLabel(ctx.selectElements().getChild(i).getText());
                    selectedColumns.add(columnInfo);
                    selectStatement += " " + str + " ";
                }
                // 기타
                else {
                    selectStatement += str + " ";
                }

//            System.out.println("middle selectStatement : " +selectStatement);
            }
            stepSqlComponent.setSelectedColumns(selectedColumns);
        }


        sqlStatement = selectStatement + fromStatement + " " + conditionStatement;
//        System.out.println();
//        System.out.println("sqlStatement : " + sqlStatement);
//        System.out.println();

        // 연속된 공백 -> 하나의 공백으로
        sqlStatement = sqlStatement.replaceAll("\\s+", " ");
        // 앞 뒤 공백 제거
        sqlStatement = sqlStatement.strip();

        sqlQueue.add(sqlStatement);
        sqlQueueForComponent.add(sqlStatement);
        sqlComponentsQueue.add(stepSqlComponent);


        if(extractFlag == 0) {
            sqlList.add(sqlStatement);
            step++;
            stepSqlComponent.setStep(step);
            stepSqlComponent.setSqlStatement(sqlStatement);
            stepSqlComponentsList.add(stepSqlComponent);
//            extractSelectComponent(ctx, sqlStatement);
            extractFlag = 0;
        }

        return ctx;
    }

    @Override public Object visitQuerySpecificationNointo(MySqlParser.QuerySpecificationNointoContext ctx) {
//        System.out.println("visitQuerySpecification");
        String sql = ctx.getText();
//        System.out.println(sql);

        // sql문
        String sqlStatement = "";
        // 해당 sql문의 컴포넌트
        StepSqlComponent stepSqlComponent = new StepSqlComponent();
        ArrayList<TableInfo> usedTables = new ArrayList<>();
        ArrayList<ColumnInfo> selectedColumns = new ArrayList<>();
        ArrayList<ColumnInfo> conditionColumns = new ArrayList<>();
        ArrayList<String> conditions = new ArrayList<>();
        Boolean joinExists = null;
        ArrayList<ColumnInfo> joinedColumns = new ArrayList<>();
        ArrayList<String> on = new ArrayList<>();


        // 1. fromClause
        String fromStatement = "";

        // FROM
        String from = ctx.fromClause().FROM().getText();
        fromStatement += " " + from + " ";

        // tableSources 요소 순회
        int tableCnt = ctx.fromClause().tableSources().getChildCount();
//        System.out.println("tableCnt : " + tableCnt);
        for (int i = 0; i < tableCnt; i++) {

            String str = ctx.fromClause().tableSources().getChild(i).getText();
//            System.out.println("tableSources = " + str);

            // tableSourceBase 요소일때
            // ex) 테이블 명 / 테이블 명 + alis -> AtomTableItem
            // ex) 서브쿼리 -> SubqueryTableItemContext
            // ex) 조인
            if (ctx.fromClause().tableSources().getChild(i) instanceof MySqlParser.TableSourceBaseContext) {
                ParserRuleContext tableSourceBaseContext = (ParserRuleContext) ctx.fromClause().tableSources().getChild(i);
                int childCountOfTableSourceBase = tableSourceBaseContext.getChildCount();
//                System.out.println("childCountOfTableSourceBase : " + childCountOfTableSourceBase);
                // tableSourceBase에서 분기
                for (int j = 0; j < childCountOfTableSourceBase; j++) {
                    // tableSourceBaseContext 의 childContext를 분석
                    ParserRuleContext childOfTableSourceBaseContext = (ParserRuleContext) tableSourceBaseContext.getChild(j);

                    // 말단 노드일때
                    if (tableSourceBaseContext.getChild(j) instanceof TerminalNodeImpl) {
//                        System.out.println("childOfTableSourceBaseContext.getChild(" + j + ") = TerminalNodeImpl");
                        fromStatement += childOfTableSourceBaseContext.getText() + " ";
                        continue;
                    }
                    // 일반 테이블명 -> tableName
                    // 일반 테이블명 + alias -> tableName alias
                    else if (tableSourceBaseContext.getChild(j) instanceof MySqlParser.AtomTableItemContext) {
                        TableInfo tableInfo = visitAtomTableItem((MySqlParser.AtomTableItemContext) tableSourceBaseContext.getChild(j));

//                        System.out.println("childOfTableSourceBaseContext.getChild(" + j + ") = AtomTableItemContext");
                        ParserRuleContext atomTableContext = (ParserRuleContext) childOfTableSourceBaseContext;
                        int cntTablebranch = atomTableContext.getChildCount();
//                        System.out.println("cntTablebranch " + cntTablebranch);

                        for (int h = 0; h < cntTablebranch; h++) {
                            // tableName
                            if (h == 0) {
                                String tableName = childOfTableSourceBaseContext.getChild(h).getText();
                                fromStatement += tableName;
                            } else if (h==1) {
                                String as = childOfTableSourceBaseContext.getChild(h).getText();
                                fromStatement += " " + as + " ";
                            } else {
                                String alias = childOfTableSourceBaseContext.getChild(h).getText();
                                fromStatement += " " + alias + " ";
                            }
                        }
                        usedTables.add(tableInfo);

                    }
                    // 서브쿼리가 있는 경우
                    else if (childOfTableSourceBaseContext instanceof MySqlParser.SubqueryTableItemContext) {
                        StepSqlComponent fromResult = visitSubqueryTableItem((MySqlParser.SubqueryTableItemContext) childOfTableSourceBaseContext);
                        fromStatement += fromResult.getSqlStatement();
                        if(fromResult.getTables()!=null) {
                            for (int k = 0; k < fromResult.getTables().size(); k++) {
                                usedTables.add(fromResult.getTables().get(k));
                            }
                        }
                    }
                    // JOIN이 있는 경우
                    else if ((childOfTableSourceBaseContext instanceof MySqlParser.OuterJoinContext)
                            || (childOfTableSourceBaseContext instanceof MySqlParser.InnerJoinContext)
                            || (childOfTableSourceBaseContext instanceof MySqlParser.NaturalJoinContext)) {
                        stepSqlComponent.setJoinExists(true);
                        int cntOfJoin = childOfTableSourceBaseContext.getChildCount();
//                        System.out.println("cntOfJoin : " + cntOfJoin);
                        for (int k = 0; k < cntOfJoin; k++) {
//                            System.out.println(childOfTableSourceBaseContext.getChild(k).getClass());
                            // 말단 노드일 때 ex) JOIN, LEFT JOIN, RIGHT JOIN, CROSS JOIN 등
                            if (childOfTableSourceBaseContext.getChild(k) instanceof TerminalNodeImpl) {
                                fromStatement += " " + childOfTableSourceBaseContext.getChild(k).getText() + " ";
                                continue;
                            }

                            // 테이블 명
                            // 테이블 명 + alias
                            if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.AtomTableItemContext) {
                                TableInfo tableInfo = visitAtomTableItem((MySqlParser.AtomTableItemContext) tableSourceBaseContext.getChild(j));

                                ParserRuleContext tableNameContext = (ParserRuleContext) childOfTableSourceBaseContext.getChild(k);
                                int cntTablebranch = tableNameContext.getChildCount();
                                for (int h = 0; h < cntTablebranch; h++) {
                                    if (h == 0) {
                                        fromStatement += tableNameContext.getChild(h).getText();
                                    } else {
                                        fromStatement += " " + tableNameContext.getChild(h).getText();
                                    }
                                }
                                usedTables.add(tableInfo);
                            }
                            else if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.SubqueryTableItemContext) {
                                StepSqlComponent fromResult = visitSubqueryTableItem((MySqlParser.SubqueryTableItemContext) childOfTableSourceBaseContext.getChild(k));
                                fromStatement += fromResult.getSqlStatement();

                                if(fromResult.getTables()!=null) {
                                    for (int h = 0; h < fromResult.getTables().size(); h++) {
                                        usedTables.add(fromResult.getTables().get(h));
                                    }
                                }
                            }
                            // JOIN 조건절
                            else if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.JoinSpecContext) {
                                ParserRuleContext joinSpecContext = (ParserRuleContext) childOfTableSourceBaseContext.getChild(k);
                                int cntJoinSpec = joinSpecContext.getChildCount();
                                String onStatement = "";
                                for (int h = 0; h < cntJoinSpec; h++) {
                                    // 말단일때 ex) ON
                                    if (joinSpecContext.getChild(h) instanceof TerminalNodeImpl) {
                                        onStatement += " " + joinSpecContext.getChild(h).getText() + " ";
                                    }
                                    // 조건절 내용
                                    else if (joinSpecContext.getChild(h) instanceof MySqlParser.PredicateExpressionContext) {
                                        StepSqlComponent fromResult = visitPredicateContext((MySqlParser.PredicateExpressionContext) joinSpecContext.getChild(h));
                                        onStatement += fromResult.getSqlStatement();
                                        if(fromResult.getConditionColumns()!=null) {
                                            for (int l = 0; l < fromResult.getConditionColumns().size(); l++) {
                                                joinedColumns.add(fromResult.getConditionColumns().get(l));
                                            }
                                        }

                                    }
                                    else
                                        onStatement += joinSpecContext.getChild(h).getText();
                                }
                                fromStatement += onStatement;

                                // 연속된 공백 -> 하나의 공백으로
                                onStatement = onStatement.replaceAll("\\s+", " ");
                                // 앞 뒤 공백 제거
                                onStatement = onStatement.strip();
                                on.add(onStatement);

                                stepSqlComponent.setJoinedColumns(joinedColumns);
                                stepSqlComponent.setOn(on);
                            }
                            else {
                                fromStatement += childOfTableSourceBaseContext.getChild(k).getText();
                            }
                        }
                    }
                }

            }
            // tableSourceBase 요소가 아닐때
            // ex) COMMA: ,
            else {

                fromStatement += str + " ";
            }
//            System.out.println("middle fromStatement : " + fromStatement);

        }

        stepSqlComponent.setTables(usedTables);



        // 2. condition 절
        String conditionStatement = "";
        // WHERE절  있을 경우
        if (ctx.fromClause().WHERE() != null) {
//            System.out.println("WHERE!!!!!!!!!!!!!!!!!");

            conditionStatement += " " + ctx.fromClause().WHERE() + " ";
            // expression 있을 경우
            if (ctx.fromClause().expression() != null) {
//                System.out.println("expression!!!!!!!!!!!!!!!!!");
//                System.out.println(ctx.fromClause().expression().getClass());

                int expressionCnt = ctx.fromClause().expression().getChildCount();

                if (ctx.fromClause().expression() instanceof MySqlParser.LogicalExpressionContext) {
//                    System.out.println("LOGICAL");
//                    System.out.println("expressionCnt : " + expressionCnt);
                    for (int l = 0; l < expressionCnt; l++) {

//                        System.out.println(ctx.fromClause().expression().getChild(l).getClass());
                        if (ctx.fromClause().expression().getChild(l) instanceof MySqlParser.PredicateExpressionContext)
                        {
                            ParserRuleContext PredicateExpressionContext = (ParserRuleContext) ctx.fromClause().expression().getChild(l);
                            StepSqlComponent conditionComponent = visitPredicateContext((MySqlParser.PredicateExpressionContext) PredicateExpressionContext);

//                            System.out.println("predicate 컴포넌트 ");
                            conditionStatement += conditionComponent.getSqlStatement();
                            if(conditionComponent.getConditionColumns()!=null) {
                                for (int j = 0; j < conditionComponent.getConditionColumns().size(); j++) {
                                    conditionColumns.add(conditionComponent.getConditionColumns().get(j));

                                }
                            }
                        }
                        else {
//                            System.out.println("Logical Operator");
                            conditionStatement += " " + ctx.fromClause().expression().getChild(l).getText() + " ";
                        }
//                        System.out.println("middle conditionStatement : " + conditionStatement);

                    }
                }
                else {
//                    System.out.println("NOT LOGICAL");
//                    System.out.println("expressionCnt : " + expressionCnt);
//                    System.out.println(ctx.fromClause().expression().getClass());

                    if (ctx.fromClause().expression() instanceof MySqlParser.PredicateExpressionContext)
                    {
                        StepSqlComponent conditionComponent = visitPredicateContext((MySqlParser.PredicateExpressionContext) ctx.fromClause().expression());

//                        System.out.println("predicate 컴포넌트 ");
                        conditionStatement += conditionComponent.getSqlStatement();
                        if(conditionComponent.getConditionColumns()!=null) {
                            for (int j = 0; j < conditionComponent.getConditionColumns().size(); j++) {
                                conditionColumns.add(conditionComponent.getConditionColumns().get(j));

//                            System.out.println(conditionComponent.getConditionColumns().get(j).getTableName());
//                            System.out.println(conditionComponent.getConditionColumns().get(j).getColumnLabel());
                            }
                        }
//                        System.out.println("middle conditionStatement -> "+ conditionStatement);

                    }
                    else {
//                        System.out.println("이건 뭐?");
                        conditionStatement += " " + ctx.fromClause().expression().getText() + " ";
                    }
//                    System.out.println("middle conditionStatement : " + conditionStatement);

                }
//                System.out.println();
//                System.out.println(conditionStatement);
//                System.out.println();

                // 연속된 공백 -> 하나의 공백으로
                conditionStatement = conditionStatement.replaceAll("\\s+", " ");
                // 앞 뒤 공백 제거
                conditionStatement = conditionStatement.strip();
                conditions.add(conditionStatement);
                stepSqlComponent.setConditions(conditions);
                stepSqlComponent.setConditionColumns(conditionColumns);

            }


        }




        // 3. SELECT
        String selectStatement = "";
        String selectKeyword = ctx.SELECT().getText();
        selectStatement += selectKeyword + " ";
        stepSqlComponent.setKeyword("SELECT");
        // selectElements
        int columnCnt = ctx.selectElements().getChildCount();
//        System.out.println("columnCnt : " + columnCnt);
        for(int i=0; i < columnCnt; i++) {
//            System.out.println(ctx.selectElements().getChild(i).getClass());
            String str = ctx.selectElements().getChild(i).getText();
            // selectElement요소에서
            // 서브쿼리 만났을 때!!!
            if(ctx.selectElements().getChild(i) instanceof MySqlParser.SelectExpressionElementContext) {
                ParserRuleContext selectExpressionContext = (ParserRuleContext) ctx.selectElements().getChild(i);

                // (서브쿼리) AS alias -> 3개의 child
                // 1 : 서브쿼리 -> predicate Expression
                // 2 : AS -> TerminalNodeImple
                // 3 : alias -> uid (터미널 아님)
                int cnt1 = selectExpressionContext.getChildCount();
//                System.out.println("cnt 1 : " + cnt1);

                for(int j=0; j< cnt1 ; j++) {
                    ColumnInfo columnInfo = new ColumnInfo();
                    // 말단 노드
                    // ex) AS
                    if(selectExpressionContext.getChild(j) instanceof TerminalNodeImpl) {
                        selectStatement += " " + selectExpressionContext.getChild(j).getText() + " ";
                        continue;
                    }
                    // 서브 쿼리
                    else if (selectExpressionContext.getChild(j) instanceof MySqlParser.PredicateExpressionContext) {
                        selectStatement += visitPredicateContext((MySqlParser.PredicateExpressionContext) selectExpressionContext.getChild(j));
                        columnInfo.setColumnLabel(selectStatement);
                    }
                    // uid
                    else {
                        selectStatement += selectExpressionContext.getChild(j).getText() + " ";
                        columnInfo.setAlias(selectExpressionContext.getChild(j).getText());
                    }
                    selectedColumns.add(columnInfo);

                }
            }
            // 일반 컬럼 정보
            else if (ctx.selectElements().getChild(i) instanceof MySqlParser.SelectColumnElementContext) {
                ParserRuleContext selectColumnElementContext = (ParserRuleContext) ctx.selectElements().getChild(i);
//                ColumnInfo columnInfo = visitSelectColumnElement((MySqlParser.SelectColumnElementContext) selectColumnElementContext);

                ColumnInfo columnInfo = new ColumnInfo();
                int cntOfSelectElement = selectColumnElementContext.getChildCount();
//                System.out.println("SELECT > ");

                for(int j=0; j<cntOfSelectElement; j++) {
                    // 일반 컬럼 명
                    if(selectColumnElementContext.getChild(j) instanceof MySqlParser.FullColumnNameContext) {
                        ColumnInfo column = visitFullColumnName((MySqlParser.FullColumnNameContext) selectColumnElementContext.getChild(j));

                        selectStatement += selectColumnElementContext.getChild(j).getText();
                        columnInfo.setTableName(column.getTableName());
                        columnInfo.setColumnLabel(column.getColumnLabel());
                    }
                    // ex) AS
                    else if (selectColumnElementContext.getChild(j) instanceof TerminalNodeImpl) {
                        selectStatement += " " + selectColumnElementContext.getChild(j).getText() + " ";
                    }
                    // Uid
                    else if (selectColumnElementContext.getChild(j) instanceof MySqlParser.UidContext) {
                        selectStatement += selectColumnElementContext.getChild(j).getText();
//                        System.out.println("SELECT 컴포넌트 > selectedColumn ColumnName Alias");
                        columnInfo.setAlias(selectColumnElementContext.getChild(j).getText());
                    } else {
                        selectStatement += selectColumnElementContext.getChild(j).getText();
                    }
                }
                selectedColumns.add(columnInfo);
            }
            // terminalNode?
            // ex) COMMA ,
            else if (ctx.selectElements().getChild(i) instanceof TerminalNodeImpl){
//                System.out.println("SELECT TERMINAL? -> " + ctx.selectElements().getChild(i).getText());
                if(str.equals("*")) {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setColumnLabel(str);
                    selectedColumns.add(columnInfo);
                }
                selectStatement += str + " ";
            }
            else if (ctx.selectElements().getChild(i) instanceof MySqlParser.SelectFunctionElementContext) {
                ColumnInfo columnInfo = new ColumnInfo();
                columnInfo.setColumnLabel(ctx.selectElements().getChild(i).getText());
                selectedColumns.add(columnInfo);
                selectStatement += " " + str + " ";
            }
            // 기타
            else {
                selectStatement += str + " ";
            }

//            System.out.println("middle selectStatement : " +selectStatement);
        }
        stepSqlComponent.setSelectedColumns(selectedColumns);

        sqlStatement = selectStatement + fromStatement + " " + conditionStatement;
//        System.out.println();
//        System.out.println("sqlStatement : " + sqlStatement);
//        System.out.println();

        // 연속된 공백 -> 하나의 공백으로
        sqlStatement = sqlStatement.replaceAll("\\s+", " ");
        // 앞 뒤 공백 제거
        sqlStatement = sqlStatement.strip();

        sqlQueue.add(sqlStatement);
        sqlQueueForComponent.add(sqlStatement);
        sqlComponentsQueue.add(stepSqlComponent);


        if(extractFlag == 0) {
            sqlList.add(sqlStatement);
            step++;
            stepSqlComponent.setStep(step);
            stepSqlComponent.setSqlStatement(sqlStatement);
            stepSqlComponentsList.add(stepSqlComponent);
//            extractSelectComponent(ctx, sqlStatement);
            extractFlag = 0;
        }

        return ctx;
    }

    @Override public Object visitUnionSelect(MySqlParser.UnionSelectContext ctx) {
//        System.out.println("visitUnionSelect");
        String sql = ctx.getText();
        return visitChildren(ctx);
    }

    @Override public Object visitUnionParenthesisSelect(MySqlParser.UnionParenthesisSelectContext ctx) {
//        System.out.println("visitUnionParenthesisSelect");
        String sql = ctx.getText();
        return visitChildren(ctx);
    }

    @Override public Object visitUnionParenthesis(MySqlParser.UnionParenthesisContext ctx) {
//        System.out.println("visitUnionParenthesis");
        String sql = ctx.getText();
        return visitChildren(ctx);
    }

    @Override public Object visitUnionStatement(MySqlParser.UnionStatementContext ctx) {
//        System.out.println("visitUnionStatement");
        String sql = ctx.getText();

        unionList.add(sqlList.get(sqlList.size()-1));
//        System.out.println(sqlList.get(sqlList.size()-1));

        if(unionList.size() >= 2) {


            // queryA
            String queryA = unionList.get(0);
//            System.out.println(queryA);

            // union 키워드 -> union / union distinct / union all
            String unionKeyword = "UNION";
            if(ctx.ALL() != null) {
                unionKeyword += " ALL";
            }
            if(ctx.DISTINCT() != null) {
                unionKeyword += " DISTINCT";
            }

            // queryB
            String queryB = unionList.get(1);
//            System.out.println(queryB);

            String unionSql = queryA + " " + unionKeyword + " " + queryB;
            sqlList.add(unionSql);


//            System.out.println("extract Union Component");
            extractUnionComponent(ctx, unionSql);
        }



        return sql;
    }

}
