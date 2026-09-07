package com.eduze.manage.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduze.manage.attendance.domain.Attendance;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {}
