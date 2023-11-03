package com.example.antlrapi.controller;


import com.example.antlrapi.dto.SqlComponent;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import static com.example.antlr.ParseProcessor.step3;

@RestController
@RequestMapping("/antlr")
public class AntlrController {
    @PostMapping("/run")
    public SqlComponent[] runSQL(@RequestParam String sql) {
        SqlComponent[] component = step3(sql);
        return component;
    }

}
