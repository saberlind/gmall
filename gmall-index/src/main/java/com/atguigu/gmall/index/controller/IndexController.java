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
        //this.indexService.testRedission();
        String s = this.indexService.testReentrantLock();
        return ResponseVo.ok(s);
    }

    @GetMapping("/index/test/rlock")
    @ResponseBody
    public ResponseVo testReadLock() throws InterruptedException {
        this.indexService.testReadLock();
        return ResponseVo.ok();
    }

    @GetMapping("/index/test/wlock")
    @ResponseBody
    public ResponseVo testWriteLock() throws InterruptedException {
        this.indexService.testWriteLock();
        return ResponseVo.ok();
    }

    @GetMapping("/index/test/s1lock")
    @ResponseBody
    public ResponseVo tests1Lock() throws InterruptedException {
        this.indexService.testSemaphore1();
        return ResponseVo.ok("s1");
    }

    @GetMapping("/index/test/s2lock")
    @ResponseBody
    public ResponseVo tests2Lock() throws InterruptedException {
        this.indexService.testSemaphore2();
        return ResponseVo.ok("s2");
    }

    @GetMapping("/index/test/latch")
    @ResponseBody
    public ResponseVo testLatch() throws InterruptedException {
        this.indexService.testLatch();
        return ResponseVo.ok("latch");
    }

    @GetMapping("/index/test/out")
    @ResponseBody
    public ResponseVo testOut() throws InterruptedException {
        this.indexService.testCountDown();
        return ResponseVo.ok("out");
    }
}
