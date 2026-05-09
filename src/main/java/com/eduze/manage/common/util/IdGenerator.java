package com.eduze.manage.common.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

public final class IdGenerator {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    private IdGenerator() {
    }

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }
}
