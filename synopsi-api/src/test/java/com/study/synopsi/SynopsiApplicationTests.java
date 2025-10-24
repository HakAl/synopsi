package com.study.synopsi;

import com.study.synopsi.config.JwtAuthenticationFilter;
import com.study.synopsi.config.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;

@SpringBootTest
class SynopsiApplicationTests {
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthenticationManager authenticationManager;

	@Test
	void contextLoads() {
	}

}
