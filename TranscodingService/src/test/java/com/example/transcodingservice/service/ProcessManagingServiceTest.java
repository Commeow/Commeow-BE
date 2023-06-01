package com.example.transcodingservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ProcessManagingServiceTest {

    @Autowired
    private ProcessManagingService processManagingService;

    @MockBean
    private ProcessBuilder processBuilder;

    @Test
    void testStartProcess() throws IOException {
        // Given
        String owner = "testOwner";
        Process process = new ProcessBuilder().inheritIO().command("echo", "testOwner").start();
        when(processBuilder.start()).thenReturn(process);
        when(processManagingService.startProcess(any())).thenReturn(Mono.just(process.pid()));

        // When
        Mono<Long> result = processManagingService.startProcess(owner);

        // Then
        StepVerifier.create(result)
                .expectNext(process.pid())
                .expectComplete()
                .verify();
    }
}
