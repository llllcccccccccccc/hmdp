package com.hmdp.service.impl;


import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> getList() {
        String key = "shopType:list";
        //1.找缓存中是否存在
        String listJsonToString = stringRedisTemplate.opsForValue().get(key);
        //2.不存在则在数据库中查找
        if (StrUtil.isBlank(listJsonToString)) {
            List<ShopType> shopTypes = query().orderByAsc("sort").list();
            String listToJson = JSONUtil.toJsonStr(shopTypes);
            //并存入缓存中
            stringRedisTemplate.opsForValue().set(key,listToJson);
            return shopTypes;
        }
        //存在则转换为List后返回
        List<ShopType> shopTypes = JSONUtil.toList(listJsonToString, ShopType.class);


        return shopTypes;
    }
}
