package com.eduze.manage.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.auth.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {}
