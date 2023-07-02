package com.example.transcodingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    // 동시성 떄문에 사용
    private final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();
    private static final long delete_interval = 1L;
    private final ScheduledExecutorService deleteFile = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean stopSearching = new AtomicBoolean(true);
    private final AtomicBoolean showMessage = new AtomicBoolean(true);

    private Flux<Path> walkFilesFlux(Path path) {
        try {
            return Flux.fromStream(Files.walk(path)
                    .filter(file -> file.toString().endsWith(".ts") || file.toString().endsWith(".jpg")));
        } catch (IOException e) {
            log.error("Failed to walk files", e);
            return Flux.empty();
        }
    }

    private void deleteOldTsAndJpgFiles(String owner) {
        if (stopSearching.get()) {
            if (showMessage.getAndSet(false)) {
                log.info("No longer searching for files.");
            }
            return;
        }

        Path directoryPath = Paths.get(path);

        try {
            Flux<Path> dirPaths = Flux.fromStream(Files.list(directoryPath));
            dirPaths.filter(Files::isDirectory)
                    .filter(dirPath -> dirPath.toFile().getName().equals(owner))
                    .flatMap(dirPath -> {
                        List<Path> filesToDelete = new ArrayList<>();
                        return walkFilesFlux(dirPath)
                                .doOnNext(file -> {
                                    try {
                                        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                                        Instant currentInstant = Instant.now();
                                        Instant fileCreationInstant = attributes.creationTime().toInstant();
                                        final long elapsedTime = Duration.between(fileCreationInstant, currentInstant).toMinutes();

                                        if (elapsedTime >= 1) {
                                            filesToDelete.add(file);
                                        }
                                    } catch (IOException e) {
                                        log.error("Failed to read attributes of file {}", file, e);
                                    }
                                })
                                .doOnComplete(() -> {
                                    for (Path file : filesToDelete) {
                                        AtomicBoolean hasFiles = new AtomicBoolean(false);
                                        try {
                                            if (file.toString().endsWith(".ts") || file.toString().endsWith(".jpg")) {
                                                Files.deleteIfExists(file);
                                                hasFiles.set(true);
                                            }
                                        } catch (IOException e) {
                                            log.error("Failed to delete file {}", file, e);
                                        }
                                        if (!hasFiles.get()) {
                                            stopSearching.set(true);
                                        }
                                    }
                                })
                                .then()
                                .subscribeOn(Schedulers.boundedElastic());
                    })
                    .subscribe();
        } catch (IOException e) {
            log.error("Failed to list directories", e);
        }
    }

    // 방송종료 시 남아있는 모든 .ts 파일삭제
    private void deleteAllTsAndJpgFiles(Path dirPath) {
        try {
            Files.walk(dirPath)
                    .filter(file -> file.toString().endsWith(".ts") || file.toString().endsWith(".jpg") || file.toString().endsWith(".m3u8"))
                    .forEach(file -> {
                        try {
                            Files.deleteIfExists(file);
                            log.info("File deleted: {}", file);
                        } catch (IOException e) {
                            log.error("Failed to delete file {}", file, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to walk files", e);
        }
    }

    private String createThumbnailPath(String owner) {
        Path directory = Paths.get(path).resolve(owner);

        // "thumbnail" 폴더 생성
        Path thumbnailDirectory = directory.resolve("thumbnail");
        if (!Files.exists(thumbnailDirectory)) {
            try {
                Files.createDirectories(thumbnailDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return thumbnailDirectory.toAbsolutePath().toString();
    }

    public Mono<Long> startProcess(String owner){
        deleteFile.scheduleAtFixedRate(() -> this.deleteOldTsAndJpgFiles(owner), delete_interval, delete_interval, TimeUnit.MINUTES);
        // isAlive() - 하위 프로세스가 Process활성 상태인지 테스트
        if(processMap.containsKey(owner) && processMap.get(owner).isAlive()){
            // 하위 프로세스를 종료
            processMap.get(owner).destroyForcibly();
            processMap.remove(owner);
        }
        // 파일탐색을 다시 시작
        stopSearching.set(false);
        String thumbnailOutputPath = Paths.get(createThumbnailPath(owner), owner + "_thumbnail_%04d.jpg").toString();
        String command = String.format(template, address + "/" + owner, owner + "_%v/data%d.ts", owner + "_%v.m3u8", thumbnailOutputPath);

        ProcessBuilder processBuilder = new ProcessBuilder();
        log.info(command);

        List<String> splitCommand = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(command);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // 큰따옴표 없이 큰따옴표로 된 문자열을 추가하세요
                splitCommand.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // 작은따옴표 없이 작은따옴표로 된 문자열을 추가하세요
                splitCommand.add(regexMatcher.group(2));
            } else {
                // 따옴표 없는 단어를 추가하세요
                splitCommand.add(regexMatcher.group());
            }
        }

        // processBuilder 의 OS 프로그램과 인수를 설정합니다.
        processBuilder.command(splitCommand);
        // processBuilder 의 redirectErrorStream 속성을 설정
        processBuilder.redirectErrorStream(true);
        // 하위 프로세스 표준 I/O의 소스 및 대상을 현재 Java 프로세스와 동일하게 설정
        processBuilder.inheritIO();

        return Mono
                .fromCallable(() -> {
                    Path directory = Paths.get(path).resolve(owner);
                    // 경로가 폴더인지 확인
                    if (!Files.exists(Paths.get(path))) {
                        try {
                            Files.createDirectory(Paths.get(path));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    /*
                     * 상대경로를 절대경로로 변경, processBuilder 의 작업 디렉토리를 설정
                     * 이후, 이 객체의 start() 메서드로 시작된 서브 프로세스는 이 디렉토리를 작업 디렉토리로서 사용
                     */
                    processBuilder.directory(new File(directory.toAbsolutePath().toString()));
                    /*
                     * ProcessBuilder 클래스의 인스턴스에 정의 된 속성으로 새 프로세스를 만들 수 있다
                     * ProcessBuilder 의 속성을 사용해 새로운 프로세스를 시작합니다.
                     * 새로운 프로세스는 directory() 로 지정된 작업 디렉토리의, environment() 로
                     * 지정된 프로세스 환경을 가지는 command() 로 지정된 커멘드와 인수를 호출
                     */
                    return processBuilder.start();
                })
                .flatMap(process -> {
                    log.info(process.info().toString());
                    log.info(String.valueOf(process.pid()));
                    // onExit() - 프로세스 종료를 위한 CompletableFuture<Process>를 반환
                    process.onExit().thenAccept((c) -> {
                        log.info(owner + " exited with code " + c.exitValue());
                        if (!processMap.get(owner).isAlive()) {
                            processMap.remove(owner);
                        }
                        // 종료된 프로세스 폴더의 .ts 파일 모두 삭제
                        Path ownerDirectory = Paths.get(path, owner);
                        deleteAllTsAndJpgFiles(ownerDirectory);
                        // 파일탐색을 중지
                        stopSearching.set(true);
                    });
                    processMap.put(owner, process);
                    // 프로세스의 기본 프로세스 ID를 반환합니다. 기본 프로세스 ID는 운영 체제가 프로세스에 할당하는 식별 번호
                    return Mono.just(process.pid());
                    // 블로킹 IO 태스크와 같은 생명주기가 긴 태스크들에 적합하다.
                    // boundedElastic 은 요청 할때마다 스레드 생성 단, 스레드 수 제한
                }).subscribeOn(Schedulers.boundedElastic());
    }
}