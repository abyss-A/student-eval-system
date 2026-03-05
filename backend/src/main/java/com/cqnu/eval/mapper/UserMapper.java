package com.cqnu.eval.mapper;

import com.cqnu.eval.model.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("select * from sys_user where id = #{id} limit 1")
    UserEntity findById(@Param("id") Long id);

    @Select("select * from sys_user where account_no = #{accountNo} and enabled = 1 limit 1")
    UserEntity findByAccountNo(@Param("accountNo") String accountNo);

    @Select("select * from sys_user where account_no = #{accountNo} and enabled = 1 limit 1")
    UserEntity findEnabledByAccountNo(@Param("accountNo") String accountNo);

    @Insert("insert into sys_user(password_hash, role, account_no, real_name, gender, phone, class_name, major_name, enabled) " +
            "values(#{passwordHash}, #{role}, #{accountNo}, #{realName}, #{gender}, #{phone}, #{className}, #{majorName}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity entity);

    @Update("update sys_user set phone = #{phone} where id = #{id}")
    int updatePhoneById(@Param("id") Long id, @Param("phone") String phone);

    @Update("update sys_user set password_hash = #{passwordHash} where id = #{id}")
    int updatePasswordById(@Param("id") Long id, @Param("passwordHash") String passwordHash);
}
