package com.xxxx.seckill.controller;

import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.vo.DetailVo;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/goods")
public class GoodsController {
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

    /**
     * 优化前QPS吞吐量935
     * 页面静态化优化后：QPS : 1106
     */
    //跳转到商品详情页
    @RequestMapping(value = "/toList", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String toList(Model model, User user, HttpServletRequest request, HttpServletResponse response) {
        //从redis中获取页面，如果不为空，直接返回页面
        ValueOperations valueOperations = redisTemplate.opsForValue();//开始操作redis
        String html = (String) valueOperations.get("goodsList");
        if (StringUtils.hasLength(html)) {
            return html;
        }

        System.out.println("请求商品列表" + user);
        model.addAttribute("user", user);
        model.addAttribute("goodsList", goodsService.findGoodsVo());

        //如果redis中页面为空，则需要生成页面的字符串放入redis中
        WebContext context = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", context);
        //process("goodsList",context),goodsList代表页面，会找goodsList.html，将context中的数据加载到goodsList.html中,生成html字符串
        if (!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsList", html, 1, TimeUnit.HOURS);
        }
        return html;
    }

    /**
     * 优化前QPS:604
     * csv数据配置1033
     * 前后端分离优化后QPS:1223
     */
    //  /detail/1 restful风格的传参
    @RequestMapping("/detail/{goodsId}")
    @ResponseBody
    public RespBean toDetail(User user, @PathVariable Long goodsId) {
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        System.out.println("----------" + goodsVo.toString());
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        //秒杀状态 0 表示未开始 1 表示进行中 2 表示结束
        int secKillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;
        //判断当前状态
        if (nowDate.before(startDate)) {
            //当前的时间在秒杀开始之前，表示秒杀还未开始
            //获取距离秒杀的时间差
            remainSeconds = (int) (((startDate.getTime() - nowDate.getTime())) / 1000);
        } else if (nowDate.after(endDate)) {
            //结束状态
            secKillStatus = 2;
            remainSeconds = -1;
        } else {
            secKillStatus = 1;
            remainSeconds = 0;
        }
        DetailVo detailVo = new DetailVo();
        detailVo.setUser(user);
        detailVo.setGoodsVo(goodsVo);
        detailVo.setSecKillStatus(secKillStatus);
        detailVo.setRemainSeconds(remainSeconds);
        return RespBean.success(detailVo);
    }
}
