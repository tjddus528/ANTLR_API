package com.example.antlrapi;

import com.example.antlrapi.dto.Condition;
import com.example.antlrapi.dto.SqlComponent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;

import static com.example.antlr.ParseProcessor.step3;

@SpringBootApplication
public class AntlrApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AntlrApiApplication.class, args);

        String sql = "SELECT loan_number FROM borrower WHERE customer_name = (SELECT customer_name FROM depositor WHERE account_number = \"A-215\");";
        ArrayList<SqlComponent> components = step3(sql);
        for(int i=0;i<components.size();i++) {
            SqlComponent component= components.get(i);
            System.out.println("step: " +component.getStep());
            System.out.println("colums: " +component.getColumns());
            System.out.println("tables: " +component.getTables());
            System.out.println("condition: [ " );
            Condition c = component.getCondition();
            System.out.println("haveCondition" + c.isHaveCondition());
            System.out.println("type" + c.getType());
            System.out.println("subject" + c.getSubject());
            System.out.println("operator" + c.getOperator());
            System.out.println("object" + c.getObject() + " ]");


        }
    }

}
