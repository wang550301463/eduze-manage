package com.eduze.manage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
@MapperScan("com.eduze.manage.tenant.demo.mapper")
public class TestMapperConfig {}
