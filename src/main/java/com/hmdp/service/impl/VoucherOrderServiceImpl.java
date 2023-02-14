package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;


    @Autowired
    private RedisIdWorker redisIdWorker;

    /**
     * CSA法解决超卖问题，类似版本哈法
     * 一.
     * 扣减库存时，判断库存是否和一开始查询时相同，
     * 相同，则无其他人同时修改，可以进行售票
     * 不相同，则有他人同时修改，该次售票失败
     * 问题售票失败率较高，但是不会出现超卖问题
     * 二、
     * 扣减库存时，判断库存是否大于零
     * 使用字段自减，防止超卖现象  ：stock = stock - 1
     *
     * @param voucherId
     * @return
     */
    @Override

    public Result seckillVoucher(Long voucherId) {

        //1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //尚未开始
            return Result.fail("秒杀尚未开始");
        }
        //3.判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //秒杀结束
            return Result.fail("秒杀已经结束");
        }
        //4.判断库存是否充足
        if (voucher.getStock() < 1) {
            //库存不足
            return Result.fail("库存不足");
        }
        //得到用户id
        Long userId = UserHolder.getUser().getId();

        synchronized (userId.toString().intern()){
            //获取代理对象（事务）
            IVoucherService proxy = (IVoucherService)AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }
    @Transactional
    public  Result createVoucherOrder(Long voucherId) {
        //一人只能买一次
        Long userId = UserHolder.getUser().getId();


            int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                //用户已经购买过
                return Result.fail("请勿重复购买");
            }
            //5.扣减库存
            boolean success = seckillVoucherService.update(new LambdaUpdateWrapper<SeckillVoucher>()
                    .eq(SeckillVoucher::getVoucherId, voucherId)
                    .gt(SeckillVoucher::getStock, 0).setSql("stock = stock - 1"));
            if (!success) {
                //扣减失败
                return Result.fail("库存不足");
            }
            //6.创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            //6.1订单id
            long orderId = redisIdWorker.nextId("order");
            voucherOrder.setId(orderId);
            //6.2用户Id

            voucherOrder.setUserId(userId);
            //6.3代金券Id
            voucherOrder.setVoucherId(voucherId);
            save(voucherOrder);
            //7.返回订单Id
            return Result.ok(orderId);

    }
}
