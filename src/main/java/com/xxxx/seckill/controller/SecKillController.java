package com.xxxx.seckill.controller;

import com.xxxx.seckill.config.AccessLimit;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.rabbitmq.MQSender;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.service.ISeckillOrderService;
import com.xxxx.seckill.util.JsonUtil;
import com.xxxx.seckill.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/seckill")
public class SecKillController implements InitializingBean {
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private ISeckillOrderService seckillOrderService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender sender;

    //用于内存标记，标记当前商品是否有库存
    private Map<Long, Boolean> emptyStockMap = new HashMap<>();

    /*QPR
     * 优化前5000个用户QPS : 655.5
     * 前后端分离优化后QPS:498
     * redis优化后1011
     * */
    @RequestMapping(value = "/{path}/doSeckill", method = RequestMethod.POST)
    @ResponseBody
//记住如果没加responseBody会报template might not exist or might not be accessible by any of the configured Template Resolvers
    //这个错误
    public RespBean doSeckill(@PathVariable String path, User user, Long goodsId) {
        //判断用户是否登录，如果没有登陆返回登陆页
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        ValueOperations ops = redisTemplate.opsForValue();
        boolean check = orderService.checkPath(path, user, goodsId);
        if (!check) {
            return RespBean.error(RespBeanEnum.STATUS_ILLEGAL);
        }
        //根据id查询商品
//        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
  /*      if (goods.getGoodsStock() < 1) {
            //如果商品数小于一，则给个提示，返回页面
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }*/
        //检查重复购买
        //select * from seckill_id where user_i = xx and goods_id = xxx
       /* SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().
                eq("user_id",user.getId()).eq("goods_id",goodsId));*/
        //让操作通过redis而不是数据库
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (seckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }
        //检查内存标记，减少对redis的无意义访问
        if (!emptyStockMap.get(goodsId)) {
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //预减库存
        Long stock = ops.decrement("seckillGoods:" + goodsId);
        if (stock < 0) {
            emptyStockMap.put(goodsId, false);
            //如果库存是0，那么减完会变成-1，所以要加回成0
            ops.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //创建一个对象，用于向mq发送用户和商品，用于下单
        SeckillMessage message = new SeckillMessage(user, goodsId);
        //将对象转成json串放入mq
        sender.sendSeckillMessage(JsonUtil.object2JsonStr(message));
        return RespBean.success();
    }

    //实现InitializingBean接口，重写方法，当系统重启，启动流程加载完配置文件之后会自动执行这个方法
    //在系统初始化的时候，读取数据库秒杀商品，将商品加入redis中
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = goodsService.findGoodsVo();
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        //,1, TimeUnit.DAYS理论上讲应该加上商品秒杀时间的有效期
        list.forEach(goodsVo -> {
            redisTemplate.opsForValue().set("seckillGoods:" + goodsVo.getId(), goodsVo.getStockCount());
            //通过内存标记，当前商品是否有库存，减少无意义的对redis的访问
            emptyStockMap.put(goodsVo.getId(), true);
        });
    }

    /*
     * @return orderId : 成功 0 排队中 -1  秒杀失败
     * */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user, Long goodsId) {
        if (user == null) {
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        Long orderId = seckillOrderService.getResult(user, goodsId);
        return RespBean.success(orderId);
    }

    /*
     * @param goodsId 商品ID
     * @return 秒杀真实地址
     * */
    @AccessLimit(second = 5, maxCount = 5)
    @RequestMapping("/path")
    @ResponseBody
    public RespBean getPath(User user, Long goodsId) {
        String path = orderService.createPath(user, goodsId);
        return RespBean.success(path);
    }
}
