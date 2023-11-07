package com.example.antlrapi.controller;


import com.example.antlrapi.dto.SqlComponent;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

import static com.example.antlr.ParseProcessor.step3;

@RestController
@RequestMapping("/antlr")
public class AntlrController {

    @GetMapping("/test")
    public String test() {
        return "success";
    }

    @PostMapping("/run")
    public ArrayList<SqlComponent> runSQL(@RequestParam String sql) {
        ArrayList<SqlComponent> components = step3(sql);
        return components;
    }

}
