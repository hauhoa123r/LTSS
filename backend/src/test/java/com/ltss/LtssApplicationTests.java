package com.ltss;

import com.ltss.support.MockAuthRepositories;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@MockAuthRepositories
class LtssApplicationTests {

    @Test
    void contextLoads() {
    }
}
