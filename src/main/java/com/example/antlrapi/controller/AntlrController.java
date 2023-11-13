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

            // 쿼리가 늘어나는 상황에 일반화 해서 짠다고 짰는데,
            // 서브쿼리가 2개인 경우 (ex. Select ~ Union Select ~) 에는 전체 쿼리가 Select 문이 아니라
            // 다른 처리 방식 필요함

            // 서브쿼리 1개(총 쿼리가 2개인 경우)
            ArrayList<String> subquery = pullSubquery(sql);
            int subquerySize = subquery.size();
            for(int i = 0; i < subquerySize; i++){

                // step2함수는 SqlComponents 요소들 채워주는 용도
                System.out.println("subquery Check ! : " + subquery.get(i));
                SqlComponent sqlcmpt = step2(subquery.get(i));

                sqlcmpt.setStep(i+1);
                sqlcmpt.setSql(subquery.get(i));
                components.add(i, sqlcmpt);
            }
            // 전체 쿼리 넣어주기
            SqlComponent originalQuery = step2(sql);
            originalQuery.setStep(subquerySize+1);
            originalQuery.setSql(sql);
            components.add(subquerySize, originalQuery);

            for(int i=0;i<components.size();i++){
                System.out.println(components.get(i).getSql());
            }
        }
        else {   // 단순한 쿼리문 일 경우
            String keyword = getCommand(sql);
            components.add(0, new SqlComponent(1, keyword, sql));
        }

        return components;
    }
}
