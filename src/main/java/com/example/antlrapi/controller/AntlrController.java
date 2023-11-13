package com.example.antlrapi.controller;


import com.example.antlrapi.dto.SqlComponent;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

import static com.example.antlr.ParseProcessor.*;

@RestController
@RequestMapping("/antlr")
public class AntlrController {


    @GetMapping("/test")
    public String test() {
        return "success";
    }

    @PostMapping("/run")
    public ArrayList<SqlComponent> runSQL(@RequestParam String sql) {
        ArrayList<SqlComponent> components = new ArrayList<>();
        // 쿼리 개수 파악 1개면 키워드랑 전체 쿼리 보내주기
        // 결국 나는 components 배열을 보내주지만, 백엔드단에서 해당 크기가 1인지 아닌지로 구별

//        components = step3(sql);
        int queryCnt = step1(sql);

        if (queryCnt != 0 && queryCnt != 1) {  // 복잡한 쿼리문 (queryCnt != 0 : insert update delete create ..  /  queryCnt != 1 : 단일 select)
            ArrayList<String> subquery = pullSubquery(sql);
            int subquerySize = subquery.size();
            for(int i = 0; i<subquerySize; i++){

                // step2함수는 SqlComponents 요소들 채워주는 용도
                SqlComponent sqlcmpt = step2(subquery.get(i));
                components.add(i, sqlcmpt);
            }
            // 전체 쿼리 넣어주기
            SqlComponent originalQuery = step2(sql);
            components.add(subquerySize, originalQuery);
        }
        else {   // 단순한 쿼리문 일 경우
            String keyword = getCommand(sql);
            components.add(0, new SqlComponent(1, keyword, sql));
        }

        return components;
    }
}
