package com.eduze.manage.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.auth.domain.UserBranch;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserBranchMapper extends BaseMapper<UserBranch> {

    @Select(
            "SELECT ub.branch_id FROM t_user_branch ub JOIN t_branch b ON b.id=ub.branch_id AND b.tenant_id=ub.tenant_id WHERE ub.user_id = #{userId} AND b.deleted_at=0 AND b.status=1")
    List<Long> selectBranchIdsByUserId(@Param("userId") Long userId);
}
