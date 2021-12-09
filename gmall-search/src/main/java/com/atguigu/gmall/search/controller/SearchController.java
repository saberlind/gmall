package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author saberlin
 * @create 2021/12/8 20:04
 */
@Controller
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("search")
    public String search(SearchParamVo searchParamVo, Model model){
        SearchResponseVo response = this.searchService.search(searchParamVo);
        model.addAttribute("response",response);
        model.addAttribute("searchParam",searchParamVo);
        return "search";
    }
}
