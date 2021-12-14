package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author saberlin
 * @create 2021/12/11 16:48
 */
@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping("/**")
    public String toIndex(Model model){
        List<CategoryEntity> categoryEntities =  this.indexService.queryLv1Categories();
        model.addAttribute("categories",categoryEntities);
        return "index";
    }

    @GetMapping("/index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLv23CategoriesByPid(@PathVariable("pid")Long pid){
        List<CategoryEntity> categoryEntities = this.indexService.queryLv23CategoriesByPid(pid);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping("/index/test/lock")
    @ResponseBody
    public ResponseVo testLock() throws InterruptedException {
        // this.indexService.testLock();
        // this.indexService.testReInLock();
        this.indexService.testRedission();
        return ResponseVo.ok();
    }
}
