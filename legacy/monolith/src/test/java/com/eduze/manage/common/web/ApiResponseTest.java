package com.eduze.manage.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiResponseTest {

    @Test
    void ok_hasCodeZero() {
        assertEquals(0, ApiResponse.ok("x").getCode());
    }

    @Test
    void fail_unauthorized_hasCode401() {
        assertEquals(401, ApiResponse.fail(ErrorCode.UNAUTHORIZED).getCode());
    }

    @Test
    void bizException_returns404Json() throws Exception {
        MockMvc mockMvc =
                MockMvcBuilders.standaloneSetup(new NotFoundController())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();

        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("测试"));
    }

    @Test
    void unexpectedException_returnsTraceId() throws Exception {
        MockMvc mockMvc =
                MockMvcBuilders.standaloneSetup(new BoomController())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();

        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @RestController
    static class NotFoundController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new BizException(ErrorCode.NOT_FOUND, "测试");
        }
    }

    @RestController
    static class BoomController {

        @GetMapping("/test/boom")
        void boom() {
            throw new RuntimeException("boom");
        }
    }
}
