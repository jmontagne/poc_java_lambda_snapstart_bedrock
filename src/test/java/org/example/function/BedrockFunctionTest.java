package org.example.function;

import org.example.service.BedrockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.FunctionCatalog;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BedrockFunctionTest {

    @Autowired
    private FunctionCatalog catalog;

    @MockBean
    private BedrockService bedrockService;

    @Test
    void testFunctionBeanExists() {
        Function<String, String> function = catalog.lookup("askAi");
        assertThat(function).isNotNull();
    }
}

