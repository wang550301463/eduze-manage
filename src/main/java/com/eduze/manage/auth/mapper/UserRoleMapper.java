package com.eduze.manage.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.auth.domain.UserRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Select("""
            SELECT r.code
            FROM t_user_role ur
            JOIN t_role r ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.deleted_at = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
