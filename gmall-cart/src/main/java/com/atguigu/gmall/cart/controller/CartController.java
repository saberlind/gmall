package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * @author saberlin
 * @create 2021/12/19 23:43
 */
@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("user/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCartsByUserId(@PathVariable("userId")Long userId){
        List<Cart> carts = this.cartService.queryCheckedCartsByUserId(userId);
        return ResponseVo.ok(carts);
    }

    /**
     * 添加购物车成功，重定向到购物车成功页
     */
    @GetMapping
    public String addCart(Cart cart){
        if (cart==null || cart.getSkuId() == null){
            throw new RuntimeException("没有选择添加到购物车的商品信息!");
        }
        this.cartService.addCart(cart);

        return "redirect:http://cart.gmall.com/addCart.html?skuId=" + cart.getSkuId() + "&count=" + cart.getCount();
    }

    @GetMapping("addCart.html")
    public String queryCart(Cart cart, Model model){
        // 新增的数量
        BigDecimal count = cart.getCount();
        cart = this.cartService.queryCart(cart.getSkuId());;
        cart.setCount(count);
        model.addAttribute("cart",cart);
        return "addCart";
    }

    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> carts = this.cartService.querycarts();
        model.addAttribute("carts",carts);
        return "cart";
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo<Object> updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo<Object> deleteCart(@RequestParam("skuId")Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

    @GetMapping("test")
    @ResponseBody
    public String test() throws ExecutionException, InterruptedException {
        long now = System.currentTimeMillis();
        System.out.println("controller.test方法开始执行！");

        this.cartService.executor1().addCallback((s) ->System.out.println("future1的正常执行结果：" + s),(e) -> System.out.println("future1执行出错：" + e.getMessage()));
        this.cartService.executor2().addCallback((result) ->System.out.println("future2的正常执行结果：" + result), (ex)-> System.out.println("future2执行出错：" + ex.getMessage()));
        this.cartService.executor3();
        System.out.println("controller.test方法结束执行！！！" + (System.currentTimeMillis() - now));

        return "hello cart!";
    }
}