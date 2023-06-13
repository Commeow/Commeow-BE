package com.example.transcodingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ProcessManagingService {

    @Value("${ffmpeg.command}")
    private String template;

    @Value("${rtmp.server}")
    private String address;

    @Value("${stream.directory}")
    private String path;

    private final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();
    // 수행할 시간 간격
    private static final long delete_interval = 1L;
    // 주기적인 작업을 스케쥴링하고 실행할 수 있는 단일 스레드 스케쥴러를 생성하는 것
    private final ScheduledExecutorService deleteFile = Executors.newSingleThreadScheduledExecutor();

    private void init() {
        /* this::deleteOldFiles: 메서드 참조로, 스케줄링된 작업을 실행할 때 호출될 메서드입니다. 이 예에서 deleteOldFiles 메서드는 주기적으로 실행
         * delete_interval: 작업이 처음으로 실행될 때까지의 초기 지연시간. 이 코드에서는 delete_interval 변수로 설정
         * delete_interval: 이후 작업이 반복 실행될 시간 간격. 이 코드에서는 delete_interval 변수로 설정
         * TimeUnit.MINUTES: delete_interval이 표현하는 시간 단위가 분
         */
        deleteFile.scheduleAtFixedRate(this::deleteOldFiles, delete_interval, delete_interval, TimeUnit.MINUTES);
    }

    // 기존 .ts 파일 삭제를 수행하는 메서드
    private void deleteOldFiles() {
        log.info("Deleting old .ts files ...");
        try {
            // 현재 시간에서 1분 전 시간 계산
            final long oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli();

            Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".ts"))
                    .forEach(file -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                            FileTime fileTime = attrs.lastModifiedTime();

                            if (fileTime.toMillis() < oneMinuteAgo) {
                                Files.delete(file);
                                log.info("Deleted file: {}", file.toAbsolutePath().toString());
                            }
                        } catch (IOException e) {
                            log.error("Failed to delete file {}", file.toAbsolutePath().toString(), e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to delete old .ts files", e);
        }
    }

    public Mono<Long> startProcess(String owner) {
        if (processMap.containsKey(owner) && processMap.get(owner).isAlive()) {
            processMap.get(owner).destroyForcibly();
            processMap.remove(owner);
        }
        String command = String.format(template, address + "/" + owner, owner + "_%v/data%d.ts", owner + "_%v.m3u8");

        ProcessBuilder processBuilder = new ProcessBuilder();
        log.info(command);

        List<String> splitCommand = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(command);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                splitCommand.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null)
                splitCommand.add(regexMatcher.group(2));
            else
                splitCommand.add(regexMatcher.group());
        }

        processBuilder.command(splitCommand);
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO();

        return Mono
                .fromCallable(() -> {
                    Path directory = Paths.get(path).resolve(owner);
                    if (!Files.isDirectory(directory)) {
                        try {
                            Files.createDirectory(directory);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    processBuilder.directory(new File(directory.toAbsolutePath().toString()));
                    init();  // 파일삭제
                    return processBuilder.start();
                })
                .flatMap(process -> {
                    log.info(process.info().toString());
                    log.info(String.valueOf(process.pid()));
                    process.onExit().thenAccept((c) -> {
                        log.info(owner + " exited with code " + c.exitValue());
                        if (!processMap.get(owner).isAlive()) {
                            processMap.remove(owner);
                        }
                    });
                    processMap.put(owner, process);
                    return Mono.just(process.pid());
                }).subscribeOn(Schedulers.boundedElastic());
    }
}

