package com.dac.graphql.app;

import com.dac.graphql.app.GraphqlServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = GraphqlServerApplication.class)
@ActiveProfiles("sqlite")
class GraphqlServerApplicationTests {

    @Test
    void contextLoads() {
    }

} 