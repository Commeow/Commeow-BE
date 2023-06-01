package com.example.transcodingservice.controller;

import com.example.transcodingservice.service.ProcessManagingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@WebFluxTest(controllers = ProcessController.class)
@ExtendWith(SpringExtension.class)
public class ProcessControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProcessManagingService processManagingService;

    @Test
    void testInitiateFfmpeg() {
        // Given
        String owner = "testOwner";
        long pid = 12345L;

        when(processManagingService.startProcess(anyString())).thenReturn(Mono.just(pid));

        // When & Then
        webTestClient.get()
                .uri("/ffmpeg/{owner}", owner)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class)
                .isEqualTo(pid);

        verify(processManagingService, times(1)).startProcess(owner);
    }

}
