package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("select * from sys_user where username = #{username} and enabled = 1 limit 1")
    UserEntity findByUsername(@Param("username") String username);

    @Select("select * from sys_user where id = #{id} limit 1")
    UserEntity findById(@Param("id") Long id);
}
