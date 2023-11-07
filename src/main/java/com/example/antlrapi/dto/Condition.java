package com.example.antlrapi.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Condition {
    boolean haveCondition;

    // ex) where account_number = "A-215" -> type : "where", subject : account_number, "operator" : "=", "object: "A-215"
    // ex) where customer_name = (SELECT ~ ) -> type : "where", subject : "customer_name" , operator : "=" , "object" : [subquery]
    String type;
    String subject;
    String operator;
    String object;


    public Condition() {
        haveCondition = false;
        type = null;
        subject = null;
        operator = null;
        object = null;
    }
}
