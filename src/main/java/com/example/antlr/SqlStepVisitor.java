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
    ArrayList<String> sqlList = new ArrayList<>();

    ArrayList<String> unionList = new ArrayList<>();



    public int step = 0;
    static ArrayList<StepSqlComponent> stepSqlComponentsList = new ArrayList<>();

    public void initSql() {
        step = 0;
        sqlQueue.clear();
        sqlList.clear();
        unionList.clear();
        stepSqlComponentsList.clear();
    }



    public void extractSelectComponent(MySqlParser.QuerySpecificationContext ctx, String sql) {
        System.out.println("extractSelectComponent");
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
            System.out.println("여기를 보시오!! "+ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(0).getClass());
            String tableSubQuery = "";
            if (ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(0) instanceof MySqlParser.ParenthesisSelectContext) {
                System.out.println("여기 들어감?");
                tableSubQuery = visitParenthesisSelectContext((MySqlParser.ParenthesisSelectContext) ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(0));
            }
            System.out.println("tableSubQuery = " + tableSubQuery);
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
            System.out.println(childrenOn);
            ParseTree onTree = joinTreeCtx.getChild(1).getChild(0);
            for (int i = 0; i < childrenOn; i++) {
                if (i == childrenOn - 1) {
                    onExpression += onTree.getChild(i).getText();
                } else {
                    onExpression += onTree.getChild(i).getText();
                    onExpression += " ";
                }
            }
            System.out.println("onExpression : " + onExpression);
            on.add(onExpression);
            stepSqlComponent.setOn(on);

        } else {  // Join 없이 From 뒤에 테이블 ,로 구분해서 여러개 오는 경우
            int tableCnt = ctx.fromClause().tableSources().getChildCount();
            for (int i = 0; i < tableCnt; i++) {
                int index = 0;
                ParseTree tableSource = ctx.fromClause().tableSources().getChild(i);
                if (i == 1) System.out.println("Join Part : " + tableSource.getText());
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
            System.out.print("used table name : " + usedTables.get(i).getTableName());
            System.out.println(" / used table alias : " + usedTables.get(i).getAlias());
        }
        stepSqlComponent.setTables(usedTables);


        // 3. 선택된 칼럼
        if (ctx.selectElements().getText().equals("*")) {
            // System.out.println("in * column");
            selectedColumns.add(new ColumnInfo(ctx.fromClause().tableSources().getText(), "*"));
            System.out.println(ctx.fromClause().tableSources().getText());
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
            System.out.print("selectedColums Table Name : " + selectedColumns.get(i).getTableName());
            System.out.print(" / selectedColums Column Label : " + selectedColumns.get(i).getColumnLabel());
            System.out.println(" / selectedColums Alias  : " + selectedColumns.get(i).getAlias());
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
                    System.out.println("table and column : " + tableName + " " + columnLable);
                }

            }


            for (int i = 0; i < conditionColumns.size(); i++) {
                System.out.print("conditionColumn TableName : " + conditionColumns.get(i).getTableName());
                System.out.println(" / conditionColumn ColumnLable : " + conditionColumns.get(i).getColumnLabel());
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

                System.out.println("Where expression : " + whereExpression);
            }
        }
        // 2. 3. 4. 저장된 컴포넌트 저장
        stepSqlComponentsList.add(stepSqlComponent);
    }
    public void extractSelectComponentNointo(MySqlParser.QuerySpecificationNointoContext ctx, String sql) {
        System.out.println("extractSelectComponentNointo");

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
            table = new TableInfo(ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(0).getText(), ctx.fromClause().tableSources().getChild(0).getChild(1).getChild(2).getChild(2).getText());
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

            // 2. On 전체 text
            String onExpression = "ON ";
            int childrenOn = joinTreeCtx.getChild(1).getChild(0).getChildCount();
            System.out.println(childrenOn);
            ParseTree onTree = joinTreeCtx.getChild(1).getChild(0);
            for (int i = 0; i < childrenOn; i++) {
                if (i == childrenOn - 1) {
                    onExpression += onTree.getChild(i).getText();
                } else {
                    onExpression += onTree.getChild(i).getText();
                    onExpression += " ";
                }
            }
            System.out.println("onExpression : " + onExpression);
            on.add(onExpression);
        } else {  // Join 없이 From 뒤에 테이블 ,로 구분해서 여러개 오는 경우
            int tableCnt = ctx.fromClause().tableSources().getChildCount();
            for (int i = 0; i < tableCnt; i++) {
                int index = 0;
                ParseTree tableSource = ctx.fromClause().tableSources().getChild(i);
                if (i == 1) System.out.println("Join Part : " + tableSource.getText());
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
            System.out.print("used table name : " + usedTables.get(i).getTableName());
            System.out.println(" / used table alias : " + usedTables.get(i).getAlias());
        }
        stepSqlComponent.setTables(usedTables);


        // 3. 선택된 칼럼
        if (ctx.selectElements().getText().equals("*")) {
            // System.out.println("in * column");
            selectedColumns.add(new ColumnInfo(ctx.fromClause().tableSources().getText(), "*"));
            System.out.println(ctx.fromClause().tableSources().getText());
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
            System.out.print("selectedColums Table Name : " + selectedColumns.get(i).getTableName());
            System.out.print(" / selectedColums Column Label : " + selectedColumns.get(i).getColumnLabel());
            System.out.println(" / selectedColums Alias  : " + selectedColumns.get(i).getAlias());
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
                    System.out.println("table and column : " + tableName + " " + columnLable);
                }

            }


            for (int i = 0; i < conditionColumns.size(); i++) {
                System.out.print("conditionColumn TableName : " + conditionColumns.get(i).getTableName());
                System.out.println(" / conditionColumn ColumnLable : " + conditionColumns.get(i).getColumnLabel());
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

                System.out.println("Where expression : " + whereExpression);
            }



            // 2. 3. 4. 저장된 컴포넌트 저장
            stepSqlComponentsList.add(stepSqlComponent);
        }
    }
    public void extractUnionComponent (MySqlParser.UnionStatementContext ctx, String sql) {
        step++;

        String unionQuery = "";
        String queryA = unionList.remove(0);
        String queryB = unionList.remove(0);
        unionQuery = queryA + " UNION " + queryB;

        StepSqlComponent stepSqlComponent = new StepSqlComponent(step, "UNION", unionQuery);
        stepSqlComponent.setQueryA(queryA);
        stepSqlComponent.setQueryB(queryB);
        stepSqlComponentsList.add(stepSqlComponent);
    }


    @Override public RuleContext visitSelectExpressionElement(MySqlParser.SelectExpressionElementContext ctx) {
        System.out.println("visitSelectExpressionElement");
        System.out.println(ctx.getText());

        int cnt = ctx.getChildCount();
        System.out.println(cnt);
        for(int i=0; i<cnt; i++) {
            if(!(ctx.getChild(i) instanceof TerminalNode)) {
                RuleContext queryContext = (RuleContext) ctx.getChild(i);
                if (queryContext instanceof MySqlParser.QuerySpecificationContext) {
                    System.out.println("QuerySpecific!!!");
                    return queryContext;
                } else {
//                visitChildren(ctx);
                }
            }
        }
        return null;
    }

    public Object searchQuerySpecific(RuleContext node) {
        System.out.println("searchQuerySpecific");

        RuleContext result = (RuleContext) defaultResult();
        System.out.println("1!");

        int n = node.getChildCount();
        for (int i=0; i<n; i++) {
            System.out.println("2!");
            if (!shouldVisitNextChild(node, result)) {
                break;
            }
            System.out.println("3!");

            ParseTree c = node.getChild(i);
            System.out.println("4!");

            RuleContext childResult = (RuleContext) c.accept(this);
            System.out.println("5!");

            result = (RuleContext) aggregateResult(result, childResult);

        }
        return result;
    }

    public String visitInPredicateContext(MySqlParser.InPredicateContext inPredicateContext) {

        String conditionStatement = "";
        System.out.println("visitInPredicateContext : " + inPredicateContext.getText());

        int cntOfchildPredicate = inPredicateContext.getChildCount();
        System.out.println("cntOfChildPredicate : "+cntOfchildPredicate);
        for (int j = 0; j < cntOfchildPredicate; j++) {
            // EXIST or SUBQUERY -> 바로 아래 자식이 simpleSelect
            if ((inPredicateContext.getChild(j) instanceof MySqlParser.ExistsExpressionAtomContext)
                    || (inPredicateContext.getChild(j) instanceof MySqlParser.SubqueryExpressionAtomContext)) {
                System.out.println("Exist Or Subquery ExpressionAtomContext : " + inPredicateContext.getChild(j).getText());

                ParserRuleContext existOrSubQueryPredicate = (ParserRuleContext) inPredicateContext.getChild(j);

                int cntChildOfExistOrSubQuery = existOrSubQueryPredicate.getChildCount();
                System.out.println("cntChildOfExistOrSubQuery : " + cntChildOfExistOrSubQuery);
                for (int k = 0; k < cntChildOfExistOrSubQuery; k++) {
                    System.out.println(existOrSubQueryPredicate.getChild(k).getText());
                    // 말단일경우 ex) ( )
                    if (existOrSubQueryPredicate.getChild(k) instanceof TerminalNodeImpl) {
                        String str = existOrSubQueryPredicate.getChild(k).getText();
                        if (str.equals("(") || str.equals(")"))
                            continue;
                        else
                            conditionStatement += str;
                    }

                    // simpleSelect -> 서브쿼리 찾기
                    ParserRuleContext simpleSelect = (ParserRuleContext) existOrSubQueryPredicate.getChild(k);
                    if ((simpleSelect instanceof MySqlParser.SimpleSelectContext) ||
                            (simpleSelect instanceof MySqlParser.ExpressionAtomPredicateContext)) {
                        System.out.println("EXITS > SimpleSelectContext : " + simpleSelect.getChild(0).getText());
                        System.out.println(simpleSelect.getChild(0).getClass());

                        searchQuerySpecific((RuleContext) simpleSelect);
                        if (!sqlQueue.isEmpty()) {
                            String temp = sqlQueue.peek();
                            sqlQueue.remove();
                            System.out.println("temp : " + temp);
                            conditionStatement += "(" + temp + ")";
                        } else {
                            conditionStatement += simpleSelect.getText();
                        }

                    } else {
                        System.out.println("이건 뭐지 ? " + simpleSelect.getClass());
                        conditionStatement += " " + simpleSelect.getText() + " ";
                    }
                }

            }
            // 바로 simpleSelect 인 경우 -> 서브쿼리 찾기
            else if (inPredicateContext.getChild(j) instanceof MySqlParser.SimpleSelectContext) {
                System.out.println("SimpleSelectContext : " + inPredicateContext.getChild(j) .getText());
                searchQuerySpecific((RuleContext) inPredicateContext.getChild(j));
                if (!sqlQueue.isEmpty()) {
                    String temp = sqlQueue.peek();
                    sqlQueue.remove();
                    System.out.println("temp : " + temp);
                    conditionStatement += "(" + temp + ")";
                }
            }
            // fullColumnNameExpression
            else {
                System.out.println(inPredicateContext.getChild(j).getClass() + " -> " + inPredicateContext.getChild(j).getText());
                conditionStatement += " " + inPredicateContext.getChild(j).getText() + " ";
            }
        }
        return conditionStatement;
    }
    public String visitPredicateContext(MySqlParser.PredicateExpressionContext predicateExpressionContext) {
        System.out.println("PredicateExpressionContext : " + predicateExpressionContext.getText());
        String conditionStatement = "";
        // 1. BinaryComparisonPredicate (A 연산자 B)
        if (predicateExpressionContext.getChild(0) instanceof MySqlParser.BinaryComparisonPredicateContext) {
            ParserRuleContext binaryComparisonPredicateContext = (ParserRuleContext) predicateExpressionContext.getChild(0);
            conditionStatement += visitBinaryComparisonPredicate((MySqlParser.BinaryComparisonPredicateContext) binaryComparisonPredicateContext);
        }
        // 2. SQL연산자 - IN
        else if (predicateExpressionContext.getChild(0) instanceof MySqlParser.InPredicateContext) {
            conditionStatement += visitInPredicateContext((MySqlParser.InPredicateContext) predicateExpressionContext.getChild(0));
        }
        // 3. SQL연산자 - Like
        else if (predicateExpressionContext.getChild(0) instanceof MySqlParser.LikePredicateContext) {
            System.out.println("LikePredicateContext : " + predicateExpressionContext.getChild(0).getText());
            ParserRuleContext ChildOflikePredicate = (ParserRuleContext) predicateExpressionContext.getChild(0);
            int cntOfchildLikePredicate = ChildOflikePredicate.getChildCount();
            for (int i = 0; i < cntOfchildLikePredicate; i++) {

                if (ChildOflikePredicate instanceof MySqlParser.InPredicateContext) {
                    conditionStatement += visitInPredicateContext((MySqlParser.InPredicateContext) ChildOflikePredicate);
                }
                else {
                    conditionStatement += " " + ChildOflikePredicate.getText() + " ";
                }
            }
        }
        // 4. SQL연산자 - Between
        else if (predicateExpressionContext.getChild(0) instanceof MySqlParser.BetweenPredicateContext) {
            System.out.println("BetweenPredicateContext : " + predicateExpressionContext.getChild(0).getText());
            int cntOfchildBetweenPredicate = predicateExpressionContext.getChild(0).getChildCount();
            for (int i = 0; i < cntOfchildBetweenPredicate; i++) {
                ParserRuleContext ChildOfBetweenPredicate = (ParserRuleContext) predicateExpressionContext.getChild(0).getChild(i);
                if (ChildOfBetweenPredicate instanceof MySqlParser.InPredicateContext) {
                    conditionStatement += visitInPredicateContext((MySqlParser.InPredicateContext) ChildOfBetweenPredicate);
                }
                else {
                    conditionStatement += " " + ChildOfBetweenPredicate.getText() + " ";
                }
            }
        }
        // 5. SQL연산자 - Null
        else if (predicateExpressionContext.getChild(0) instanceof MySqlParser.IsNullPredicateContext) {
            System.out.println("IsNullPredicateContext : " + predicateExpressionContext.getChild(0).getText());
            int cntOfchildNullPredicate = predicateExpressionContext.getChild(0).getChildCount();
            for (int i = 0; i < cntOfchildNullPredicate; i++) {
                ParserRuleContext ChildOfNullPredicate = (ParserRuleContext) predicateExpressionContext.getChild(0).getChild(i);
                if (ChildOfNullPredicate instanceof MySqlParser.InPredicateContext) {
                    conditionStatement += visitInPredicateContext((MySqlParser.InPredicateContext) ChildOfNullPredicate);
                }
                else {
                    conditionStatement += " " + ChildOfNullPredicate.getText() + " ";
                }
            }

        }
        else {
            System.out.println("이외에 경우가 있나? 일단 붙이기");
            conditionStatement += predicateExpressionContext.getChild(0).getText();
        }
        return conditionStatement;
    }
    public String visitBinaryComparisonPredicate(MySqlParser.BinaryComparisonPredicateContext binaryComparisonPredicateContext) {
        System.out.println("visitBinaryComparisonPredicate -> " + binaryComparisonPredicateContext.getText());

        String compareStatement = "";

        int childOfBinary = binaryComparisonPredicateContext.getChildCount();
        System.out.println("childOfBinary : " + childOfBinary);
        for (int i = 0; i < childOfBinary; i++) {
            System.out.println(binaryComparisonPredicateContext.getChild(i).getClass());
            // 1) InPredicate
            // A , B
            if ((binaryComparisonPredicateContext.getChild(i) instanceof MySqlParser.InPredicateContext)) {
                compareStatement += visitInPredicateContext((MySqlParser.InPredicateContext) binaryComparisonPredicateContext.getChild(i));
            }
            // Operator 연산자
            else if (binaryComparisonPredicateContext.getChild(i) instanceof MySqlParser.ComparisonOperatorContext) {
                compareStatement += " " + binaryComparisonPredicateContext.getChild(i).getText() + " ";
            }
            else {
                compareStatement += " " + binaryComparisonPredicateContext.getChild(i).getText();
            }
        }
        return compareStatement;
    }
    public String visitSubqueryTableItem(MySqlParser.SubqueryTableItemContext subqueryTableItemContext) {
        int cntOfChildSubqueryTable = subqueryTableItemContext.getChildCount();
        String tableStatement = "";
        for(int i=0; i<cntOfChildSubqueryTable; i++) {
            if(subqueryTableItemContext.getChild(i) instanceof TerminalNodeImpl) {
                tableStatement += " " + subqueryTableItemContext.getChild(i).getText();
                continue;
            }
            else if (subqueryTableItemContext.getChild(i) instanceof MySqlParser.ParenthesisSelectContext) {
                System.out.println("visitSubqueryTableItem > ParenthesisSelectContext");
                searchQuerySpecific((RuleContext) subqueryTableItemContext.getChild(i).getChild(0));
                if (!sqlQueue.isEmpty()) {
                    String temp = sqlQueue.peek();
                    sqlQueue.remove();
                    System.out.println("temp : " + temp);
                    tableStatement += "(" + temp + ")";
                }
                else {
                    tableStatement += " " + subqueryTableItemContext.getChild(i).getText();
                }
                continue;
            }
            else if (subqueryTableItemContext.getChild(i) instanceof MySqlParser.UidContext) {
                tableStatement += " " + subqueryTableItemContext.getChild(i).getText();
                continue;
            }
            else {
                tableStatement += " " + subqueryTableItemContext.getChild(i).getText();
                continue;
            }
        }
        return tableStatement;
    }
    public String visitParenthesisSelectContext(MySqlParser.ParenthesisSelectContext parenthesisSelect) {
        String str = "";

        System.out.println("visitSubqueryTableItem > ParenthesisSelectContext");
        searchQuerySpecific((RuleContext) parenthesisSelect.getChild(0));
        if (!sqlQueueForComponent.isEmpty()) {
            String temp = sqlQueueForComponent.peek();
            sqlQueueForComponent.remove();
            System.out.println("temp : " + temp);
            str += temp;
        }
        else {
            str += " " + parenthesisSelect.getChild(0).getText();
        }

        return str;
    }


    @Override public Object visitQuerySpecification(MySqlParser.QuerySpecificationContext ctx) {
        System.out.println("visitQuerySpecification");
        String sql = ctx.getText();
        System.out.println(sql);

        String sqlStatement = "";


        // 1. fromClause
        String fromStatement = "";

        // FROM
        String from = ctx.fromClause().FROM().getText();
        fromStatement += " " + from + " ";

        // tableSources 요소 순회
        int tableCnt = ctx.fromClause().tableSources().getChildCount();
        System.out.println("tableCnt : " + tableCnt);
        for (int i = 0; i < tableCnt; i++) {

            String str = ctx.fromClause().tableSources().getChild(i).getText();
            System.out.println("tableSources = " + str);

            // tableSourceBase 요소일때
            // ex) 테이블 명 / 테이블 명 + alis -> AtomTableItem
            // ex) 서브쿼리 -> SubqueryTableItemContext
            // ex) 조인
            if (ctx.fromClause().tableSources().getChild(i) instanceof MySqlParser.TableSourceBaseContext) {
                ParserRuleContext tableSourceBaseContext = (ParserRuleContext) ctx.fromClause().tableSources().getChild(i);
                System.out.println(tableSourceBaseContext.getClass());

                int childCountOfTableSourceBase = tableSourceBaseContext.getChildCount();
                System.out.println("childCountOfTableSourceBase : " + childCountOfTableSourceBase);

                // tableSourceBase에서 분기
                for (int j = 0; j < childCountOfTableSourceBase; j++) {
                    System.out.println(tableSourceBaseContext.getChild(j).getClass());
                    // tableSourceBaseContext 의 childContext를 분석
                    ParserRuleContext childOfTableSourceBaseContext = (ParserRuleContext) tableSourceBaseContext.getChild(j);

                    // 말단 노드일때
                    if (tableSourceBaseContext.getChild(j) instanceof TerminalNodeImpl) {
                        System.out.println("childOfTableSourceBaseContext.getChild(" + j + ") = TerminalNodeImpl");
                        fromStatement += childOfTableSourceBaseContext.getText() + " ";
                        continue;
                    }

                    // 일반 테이블명 -> tableName
                    // 일반 테이블명 + alias -> tableName alias
                    if (childOfTableSourceBaseContext instanceof MySqlParser.AtomTableItemContext) {
                        System.out.println("childOfTableSourceBaseContext.getChild(" + j + ") = AtomTableItemContext");


                        ParserRuleContext atomTableContext = (ParserRuleContext) childOfTableSourceBaseContext;
                        int cntTablebranch = atomTableContext.getChildCount();
                        System.out.println("cntTablebranch " + cntTablebranch);
                        for (int h = 0; h < cntTablebranch; h++) {
                            if (h == 0) {
                                fromStatement += childOfTableSourceBaseContext.getChild(h).getText();
                            } else {
                                fromStatement += " " + childOfTableSourceBaseContext.getChild(h).getText();
                            }

                        }
                    }
                    // 서브쿼리가 있는 경우
                    else if (childOfTableSourceBaseContext instanceof MySqlParser.SubqueryTableItemContext) {
                        fromStatement += visitSubqueryTableItem((MySqlParser.SubqueryTableItemContext) childOfTableSourceBaseContext);
                    }
                    // JOIN이 있는 경우
                    else if ((childOfTableSourceBaseContext instanceof MySqlParser.OuterJoinContext)
                            || (childOfTableSourceBaseContext instanceof MySqlParser.InnerJoinContext)
                            || (childOfTableSourceBaseContext instanceof MySqlParser.NaturalJoinContext)) {
                        int cntOfJoin = childOfTableSourceBaseContext.getChildCount();
                        System.out.println("cntOfJoin : " + cntOfJoin);
                        for (int k = 0; k < cntOfJoin; k++) {
                            System.out.println(childOfTableSourceBaseContext.getChild(k).getClass());
                            // 말단 노드일 때 ex) JOIN, LEFT JOIN, RIGHT JOIN, CROSS JOIN 등
                            if (childOfTableSourceBaseContext.getChild(k) instanceof TerminalNodeImpl) {
                                fromStatement += " " + childOfTableSourceBaseContext.getChild(k).getText() + " ";
                                continue;
                            }

                            // 테이블 명
                            // 테이블 명 + alias
                            if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.AtomTableItemContext) {
                                ParserRuleContext tableNameContext = (ParserRuleContext) childOfTableSourceBaseContext.getChild(k);
                                int cntTablebranch = tableNameContext.getChildCount();
                                for (int h = 0; h < cntTablebranch; h++) {
                                    if (h == 0) {
                                        fromStatement += tableNameContext.getChild(h).getText();
                                    } else {
                                        fromStatement += " " + tableNameContext.getChild(h).getText();
                                    }
                                }
                            }
                            else if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.SubqueryTableItemContext) {
                                fromStatement += visitSubqueryTableItem((MySqlParser.SubqueryTableItemContext) childOfTableSourceBaseContext.getChild(k));
                            }
                            // JOIN 조건절
                            else if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.JoinSpecContext) {
                                ParserRuleContext joinSpecContext = (ParserRuleContext) childOfTableSourceBaseContext.getChild(k);
                                int cntJoinSpec = joinSpecContext.getChildCount();
                                for (int h = 0; h < cntJoinSpec; h++) {
                                    // 말단일때 ex) ON
                                    if (joinSpecContext.getChild(h) instanceof TerminalNodeImpl) {
                                        fromStatement += " " + joinSpecContext.getChild(h).getText() + " ";
                                    }
                                    // 조건절 내용
                                    else if (joinSpecContext.getChild(h) instanceof MySqlParser.PredicateExpressionContext) {
                                        fromStatement += visitPredicateContext((MySqlParser.PredicateExpressionContext) joinSpecContext.getChild(h));
                                    }
                                    else
                                        fromStatement += joinSpecContext.getChild(h).getText();
                                }
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
            System.out.println("middle fromStatement : " + fromStatement);

        }


        // 2. condition 절
        String conditionStatement = "";
        // WHERE절  있을 경우
        if (ctx.fromClause().WHERE() != null) {
            conditionStatement += " " + ctx.fromClause().WHERE() + " ";
            // expression 있을 경우
            if (ctx.fromClause().expression() != null) {
                System.out.println(ctx.fromClause().expression().getClass());

                int expressionCnt = ctx.fromClause().expression().getChildCount();

                if (ctx.fromClause().expression() instanceof MySqlParser.LogicalExpressionContext) {
                    System.out.println("LOGICAL");
                    System.out.println("expressionCnt : " + expressionCnt);
                    for (int l = 0; l < expressionCnt; l++) {

                        System.out.println(ctx.fromClause().expression().getChild(l).getClass());


                        if (ctx.fromClause().expression().getChild(l) instanceof MySqlParser.PredicateExpressionContext)
                        {
                            ParserRuleContext PredicateExpressionContext = (ParserRuleContext) ctx.fromClause().expression().getChild(l);
                            conditionStatement += visitPredicateContext((MySqlParser.PredicateExpressionContext) PredicateExpressionContext);

                        }
                        else {
                            System.out.println("Logical Operator");
                            conditionStatement += " " + ctx.fromClause().expression().getChild(l).getText() + " ";
                        }
                        System.out.println("middle conditionStatement : " + conditionStatement);

                    }
                }
                else {
                    System.out.println("NOT LOGICAL");
                    System.out.println("expressionCnt : " + expressionCnt);

                    if (ctx.fromClause().expression() instanceof MySqlParser.PredicateExpressionContext)
                    {
                        ParserRuleContext PredicateExpressionContext = (ParserRuleContext) ctx.fromClause().expression();
                        conditionStatement += visitPredicateContext((MySqlParser.PredicateExpressionContext) PredicateExpressionContext);
                    }
                    else {
                        System.out.println("이건 뭐?");
                        conditionStatement += " " + ctx.fromClause().expression().getText() + " ";
                    }
                    System.out.println("middle conditionStatement : " + conditionStatement);

                }

            }
        }



        // 3. SELECT
        String selectStatement = "";
        String selectKeyword = ctx.SELECT().getText();
        selectStatement += selectKeyword + " ";
        // selectElements
        int columnCnt = ctx.selectElements().getChildCount();
        System.out.println("columnCnt : " + columnCnt);
        for(int i=0; i < columnCnt; i++) {
            System.out.println(ctx.selectElements().getChild(i).getClass());
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
                System.out.println("cnt 1 : " + cnt1);
                for(int j=0; j< cnt1 ; j++) {
                    // 말단 노드
                    // ex) AS
                    if(selectExpressionContext.getChild(j) instanceof TerminalNodeImpl) {
                        selectStatement += " " + selectExpressionContext.getChild(j).getText() + " ";
                        continue;
                    }
                    // 서브 쿼리
                    else if (selectExpressionContext.getChild(j) instanceof MySqlParser.PredicateExpressionContext) {
                        selectStatement += visitPredicateContext((MySqlParser.PredicateExpressionContext) selectExpressionContext.getChild(j));
                    }
                    // uid
                    else {
                        selectStatement += selectExpressionContext.getChild(j).getText() + " ";
                    }

                }
            }
            // 일반 컬럼 정보
            else if (ctx.selectElements().getChild(i) instanceof MySqlParser.SelectColumnElementContext) {
                ParserRuleContext selectColumnElementContext = (ParserRuleContext) ctx.selectElements().getChild(i);
                int cntOfSelectElement = selectColumnElementContext.getChildCount();
                for(int j=0; j<cntOfSelectElement; j++) {
                    // 일반 컬럼 명
                    if(selectColumnElementContext.getChild(j) instanceof MySqlParser.FullColumnNameContext) {
                        selectStatement += selectColumnElementContext.getChild(j).getText();
                    }
                    // ex) AS
                    else if (selectColumnElementContext.getChild(j) instanceof TerminalNodeImpl) {
                        selectStatement += " " + selectColumnElementContext.getChild(j).getText() + " ";
                    }
                    // Uid
                    else if (selectColumnElementContext.getChild(j) instanceof MySqlParser.UidContext) {
                        selectStatement += selectColumnElementContext.getChild(j).getText();
                    } else {
                        selectStatement += selectColumnElementContext.getChild(j).getText();
                    }
                }
            }
            // terminalNode?
            // ex) COMMA ,
            else if (ctx.selectElements().getChild(i) instanceof TerminalNodeImpl){
                System.out.println("SELECT TERMINAL? -> " + ctx.selectElements().getChild(i).getText());
                selectStatement += str + " ";
            }
            // 기타
            else {
                selectStatement += str + " ";
            }

            System.out.println("middle selectStatement : " +selectStatement);
        }

        sqlStatement = selectStatement + fromStatement + conditionStatement;
        System.out.println();
        System.out.println("sqlStatement : " + sqlStatement);
        System.out.println();

        // 연속된 공백 -> 하나의 공백으로
        sqlStatement = sqlStatement.replaceAll("\\s+", " ");
        sqlStatement = sqlStatement.strip();

        sqlQueue.add(sqlStatement);
        sqlQueueForComponent.add(sqlStatement);
        sqlList.add(sqlStatement);

        extractSelectComponent(ctx, sqlStatement);
        return ctx;
    }

    @Override public Object visitQuerySpecificationNointo(MySqlParser.QuerySpecificationNointoContext ctx) {
        System.out.println("visitQuerySpecificationNointo");
        String sql = ctx.getText();
        System.out.println(sql);

        String sqlStatement = "";


        // 1. fromClause
        String fromStatement = "";

        // FROM
        String from = ctx.fromClause().FROM().getText();
        fromStatement += " " + from + " ";

        // tableSources 요소 순회
        int tableCnt = ctx.fromClause().tableSources().getChildCount();
        System.out.println("tableCnt : " + tableCnt);
        for (int i = 0; i < tableCnt; i++) {

            String str = ctx.fromClause().tableSources().getChild(i).getText();
            System.out.println("tableSources = " + str);

            // tableSourceBase 요소일때
            // ex) 테이블 명 / 테이블 명 + alis -> AtomTableItem
            // ex) 서브쿼리 -> SubqueryTableItemContext
            // ex) 조인
            if (ctx.fromClause().tableSources().getChild(i) instanceof MySqlParser.TableSourceBaseContext) {
                ParserRuleContext tableSourceBaseContext = (ParserRuleContext) ctx.fromClause().tableSources().getChild(i);
                System.out.println(tableSourceBaseContext.getClass());

                int childCountOfTableSourceBase = tableSourceBaseContext.getChildCount();
                System.out.println("childCountOfTableSourceBase : " + childCountOfTableSourceBase);

                // tableSourceBase에서 분기
                for (int j = 0; j < childCountOfTableSourceBase; j++) {
                    System.out.println(tableSourceBaseContext.getChild(j).getClass());
                    // tableSourceBaseContext 의 childContext를 분석
                    ParserRuleContext childOfTableSourceBaseContext = (ParserRuleContext) tableSourceBaseContext.getChild(j);

                    // 말단 노드일때
                    if (tableSourceBaseContext.getChild(j) instanceof TerminalNodeImpl) {
                        System.out.println("childOfTableSourceBaseContext.getChild(" + j + ") = TerminalNodeImpl");
                        fromStatement += childOfTableSourceBaseContext.getText() + " ";
                        continue;
                    }

                    // 일반 테이블명 -> tableName
                    // 일반 테이블명 + alias -> tableName alias
                    if (childOfTableSourceBaseContext instanceof MySqlParser.AtomTableItemContext) {
                        System.out.println("childOfTableSourceBaseContext.getChild(" + j + ") = AtomTableItemContext");


                        ParserRuleContext atomTableContext = (ParserRuleContext) childOfTableSourceBaseContext;
                        int cntTablebranch = atomTableContext.getChildCount();
                        System.out.println("cntTablebranch " + cntTablebranch);
                        for (int h = 0; h < cntTablebranch; h++) {
                            if (h == 0) {
                                fromStatement += childOfTableSourceBaseContext.getChild(h).getText();
                            } else {
                                fromStatement += " " + childOfTableSourceBaseContext.getChild(h).getText();
                            }

                        }
                    }
                    // 서브쿼리가 있는 경우
                    else if (childOfTableSourceBaseContext instanceof MySqlParser.SubqueryTableItemContext) {
                        fromStatement += visitSubqueryTableItem((MySqlParser.SubqueryTableItemContext) childOfTableSourceBaseContext);
                    }
                    // JOIN이 있는 경우
                    else if ((childOfTableSourceBaseContext instanceof MySqlParser.OuterJoinContext)
                            || (childOfTableSourceBaseContext instanceof MySqlParser.InnerJoinContext)
                            || (childOfTableSourceBaseContext instanceof MySqlParser.NaturalJoinContext)) {
                        int cntOfJoin = childOfTableSourceBaseContext.getChildCount();
                        System.out.println("cntOfJoin : " + cntOfJoin);
                        for (int k = 0; k < cntOfJoin; k++) {
                            System.out.println(childOfTableSourceBaseContext.getChild(k).getClass());
                            // 말단 노드일 때 ex) JOIN, LEFT JOIN, RIGHT JOIN, CROSS JOIN 등
                            if (childOfTableSourceBaseContext.getChild(k) instanceof TerminalNodeImpl) {
                                fromStatement += " " + childOfTableSourceBaseContext.getChild(k).getText() + " ";
                                continue;
                            }

                            // 테이블 명
                            // 테이블 명 + alias
                            if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.AtomTableItemContext) {
                                ParserRuleContext tableNameContext = (ParserRuleContext) childOfTableSourceBaseContext.getChild(k);
                                int cntTablebranch = tableNameContext.getChildCount();
                                for (int h = 0; h < cntTablebranch; h++) {
                                    if (h == 0) {
                                        fromStatement += tableNameContext.getChild(h).getText();
                                    } else {
                                        fromStatement += " " + tableNameContext.getChild(h).getText();
                                    }
                                }
                            }
                            else if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.SubqueryTableItemContext) {
                                fromStatement += visitSubqueryTableItem((MySqlParser.SubqueryTableItemContext) childOfTableSourceBaseContext.getChild(k));
                            }
                            // JOIN 조건절
                            else if (childOfTableSourceBaseContext.getChild(k) instanceof MySqlParser.JoinSpecContext) {
                                ParserRuleContext joinSpecContext = (ParserRuleContext) childOfTableSourceBaseContext.getChild(k);
                                int cntJoinSpec = joinSpecContext.getChildCount();
                                for (int h = 0; h < cntJoinSpec; h++) {
                                    // 말단일때 ex) ON
                                    if (joinSpecContext.getChild(h) instanceof TerminalNodeImpl) {
                                        fromStatement += " " + joinSpecContext.getChild(h).getText() + " ";
                                    }
                                    // 조건절 내용
                                    else if (joinSpecContext.getChild(h) instanceof MySqlParser.PredicateExpressionContext) {
                                        fromStatement += visitPredicateContext((MySqlParser.PredicateExpressionContext) joinSpecContext.getChild(h));
                                    }
                                    else
                                        fromStatement += joinSpecContext.getChild(h).getText();
                                }
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
            System.out.println("middle fromStatement : " + fromStatement);

        }


        // 2. condition 절
        String conditionStatement = "";
        // WHERE절  있을 경우
        if (ctx.fromClause().WHERE() != null) {
            conditionStatement += " " + ctx.fromClause().WHERE() + " ";
            // expression 있을 경우
            if (ctx.fromClause().expression() != null) {
                System.out.println(ctx.fromClause().expression().getClass());

                int expressionCnt = ctx.fromClause().expression().getChildCount();

                if (ctx.fromClause().expression() instanceof MySqlParser.LogicalExpressionContext) {
                    System.out.println("LOGICAL");
                    System.out.println("expressionCnt : " + expressionCnt);
                    for (int l = 0; l < expressionCnt; l++) {

                        System.out.println(ctx.fromClause().expression().getChild(l).getClass());


                        if (ctx.fromClause().expression().getChild(l) instanceof MySqlParser.PredicateExpressionContext)
                        {
                            ParserRuleContext PredicateExpressionContext = (ParserRuleContext) ctx.fromClause().expression().getChild(l);
                            conditionStatement += visitPredicateContext((MySqlParser.PredicateExpressionContext) PredicateExpressionContext);

                        }
                        else {
                            System.out.println("Logical Operator");
                            conditionStatement += " " + ctx.fromClause().expression().getChild(l).getText() + " ";
                        }
                        System.out.println("middle conditionStatement : " + conditionStatement);

                    }
                }
                else {
                    System.out.println("NOT LOGICAL");
                    System.out.println("expressionCnt : " + expressionCnt);

                    if (ctx.fromClause().expression() instanceof MySqlParser.PredicateExpressionContext)
                    {
                        ParserRuleContext PredicateExpressionContext = (ParserRuleContext) ctx.fromClause().expression();
                        conditionStatement += visitPredicateContext((MySqlParser.PredicateExpressionContext) PredicateExpressionContext);
                    }
                    else {
                        System.out.println("이건 뭐?");
                        conditionStatement += " " + ctx.fromClause().expression().getText() + " ";
                    }
                    System.out.println("middle conditionStatement : " + conditionStatement);

                }

            }
        }



        // 3. SELECT
        String selectStatement = "";
        String selectKeyword = ctx.SELECT().getText();
        selectStatement += selectKeyword + " ";
        // selectElements
        int columnCnt = ctx.selectElements().getChildCount();
        System.out.println("columnCnt : " + columnCnt);
        for(int i=0; i < columnCnt; i++) {
            System.out.println(ctx.selectElements().getChild(i).getClass());
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
                System.out.println("cnt 1 : " + cnt1);
                for(int j=0; j< cnt1 ; j++) {
                    // 말단 노드
                    // ex) AS
                    if(selectExpressionContext.getChild(j) instanceof TerminalNodeImpl) {
                        selectStatement += " " + selectExpressionContext.getChild(j).getText() + " ";
                        continue;
                    }
                    // 서브 쿼리
                    else if (selectExpressionContext.getChild(j) instanceof MySqlParser.PredicateExpressionContext) {
                        selectStatement += visitPredicateContext((MySqlParser.PredicateExpressionContext) selectExpressionContext.getChild(j));
                    }
                    // uid
                    else {
                        selectStatement += selectExpressionContext.getChild(j).getText() + " ";
                    }

                }
            }
            // 일반 컬럼 정보
            else if (ctx.selectElements().getChild(i) instanceof MySqlParser.SelectColumnElementContext) {
                ParserRuleContext selectColumnElementContext = (ParserRuleContext) ctx.selectElements().getChild(i);
                int cntOfSelectElement = selectColumnElementContext.getChildCount();
                for(int j=0; j<cntOfSelectElement; j++) {
                    // 일반 컬럼 명
                    if(selectColumnElementContext.getChild(j) instanceof MySqlParser.FullColumnNameContext) {
                        selectStatement += selectColumnElementContext.getChild(j).getText();
                    }
                    // ex) AS
                    else if (selectColumnElementContext.getChild(j) instanceof TerminalNodeImpl) {
                        selectStatement += " " + selectColumnElementContext.getChild(j).getText() + " ";
                    }
                    // Uid
                    else if (selectColumnElementContext.getChild(j) instanceof MySqlParser.UidContext) {
                        selectStatement += selectColumnElementContext.getChild(j).getText();
                    } else {
                        selectStatement += selectColumnElementContext.getChild(j).getText();
                    }
                }
            }
            // terminalNode?
            // ex) COMMA ,
            else if (ctx.selectElements().getChild(i) instanceof TerminalNodeImpl){
                System.out.println("SELECT TERMINAL? -> " + ctx.selectElements().getChild(i).getText());
                selectStatement += str + " ";
            }
            // 기타
            else {
                selectStatement += str + " ";
            }

            System.out.println("middle selectStatement : " +selectStatement);
        }

        sqlStatement = selectStatement + fromStatement + conditionStatement;
        System.out.println();
        System.out.println("sqlStatement : " + sqlStatement);
        System.out.println();

        // 연속된 공백 -> 하나의 공백으로
        sqlStatement = sqlStatement.replaceAll("\\s+", " ");
        sqlStatement = sqlStatement.strip();

        sqlQueue.add(sqlStatement);
        sqlList.add(sqlStatement);

        extractSelectComponentNointo(ctx, sqlStatement);
        return ctx;
    }

    @Override public Object visitUnionSelect(MySqlParser.UnionSelectContext ctx) {
        System.out.println("visitUnionSelect");
        String sql = ctx.getText();
        return visitChildren(ctx);
    }
    @Override public Object visitUnionParenthesisSelect(MySqlParser.UnionParenthesisSelectContext ctx) {
        System.out.println("visitUnionParenthesisSelect");
        String sql = ctx.getText();
        return visitChildren(ctx);
    }

    @Override public Object visitUnionParenthesis(MySqlParser.UnionParenthesisContext ctx) {
        System.out.println("visitUnionParenthesis");
        String sql = ctx.getText();
        return visitChildren(ctx);
    }

    @Override public Object visitUnionStatement(MySqlParser.UnionStatementContext ctx) {
        System.out.println("visitUnionStatement");
        String sql = ctx.getText();

        unionList.add(sqlList.get(sqlList.size()-1));
        System.out.println(sqlList.get(sqlList.size()-1));

        if(unionList.size() >= 2) {


            // queryA
            String queryA = unionList.get(0);
            System.out.println(queryA);

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
            System.out.println(queryB);

            String unionSql = queryA + " " + unionKeyword + " " + queryB;
            sqlList.add(unionSql);

            System.out.println("extract Union Component");
            extractUnionComponent(ctx, unionSql);
        }



        return sql;
    }

}
