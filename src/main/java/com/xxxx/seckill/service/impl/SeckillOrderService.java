package com.xxxx.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxxx.seckill.mapper.SeckillOrderMapper;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.ISeckillOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeckillOrderService extends ServiceImpl<SeckillOrderMapper, SeckillOrder> implements ISeckillOrderService {
    /*
     * @return orderId : 成功 0 排队中 -1  秒杀失败
     * */
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public Long getResult(User user, Long goodsId) {
        //select * from seckill where userid = *** and goods_id = ***;
        SeckillOrder seckillOrder = seckillOrderMapper.selectOne(new QueryWrapper<SeckillOrder>().eq("user_id",user.getId()).eq("goods_id",goodsId));
        if(null != seckillOrder){
            return seckillOrder.getOrderId();
            //首先不能以redis中的库存作为依据，因为可能当前正在mq中等待，如果成功，则上个判断进
            //此时，如果出现这个key，并且没有订单，没买到，且没库存
            //则是失败
        }else if(redisTemplate.hasKey("isStockEmpty"+goodsId)){
            return -1L;
        }else{
            return 0L;
        }
    }
}
