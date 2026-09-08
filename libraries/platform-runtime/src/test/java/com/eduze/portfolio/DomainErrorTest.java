package com.eduze.portfolio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eduze.platform.runtime.PlatformException;
import com.eduze.platform.runtime.RuntimeExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class DomainErrorTest {
    @RestController
    static class GuardedResource {
        @GetMapping("/resource")
        String read() {
            throw new PlatformException(403, "无权访问");
        }
    }

    @Test
    void domainAuthorizationErrorsPreserveHttpStatusAndSafeEnvelope() throws Exception {
        MockMvcBuilders.standaloneSetup(new GuardedResource())
                .setControllerAdvice(new RuntimeExceptionHandler())
                .build()
                .perform(get("/resource"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
