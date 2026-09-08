package com.eduze.manage.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.auth.domain.UserBranch;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserBranchMapper extends BaseMapper<UserBranch> {

    @Select("SELECT branch_id FROM t_user_branch WHERE user_id = #{userId}")
    List<Long> selectBranchIdsByUserId(@Param("userId") Long userId);
}
