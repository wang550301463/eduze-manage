package com.eduze.manage.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.auth.domain.Permission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select(
            """
            SELECT DISTINCT p.code
            FROM t_user_role ur
            JOIN t_role_permission rp ON ur.role_id = rp.role_id
            JOIN t_permission p ON rp.permission_id = p.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
