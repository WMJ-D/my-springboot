package com.example.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.test.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据条件查询用户（动态 SQL）
     */
    List<User> selectByCondition(@Param("name") String name, 
                                  @Param("minId") Integer minId);

    /**
     * 查询 id > 指定值的用户
     */
    List<User> selectByIdGreaterThan(@Param("id") Integer id);
}
