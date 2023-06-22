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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ProcessManagingService {

    @Value("${ffmpeg.command}")
    private String template;

    @Value("${rtmp.server}")
    private String address;

    @Value("${stream.directory}")
    private String path;

    @Value("${thumbnail.directory}")
    private String thumbnailPath;

    // 동시성 떄문에 사용
    private final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();
    private static final long delete_interval = 1L;
    private final ScheduledExecutorService deleteFile = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean initialCheck = new AtomicBoolean(true);
    private final AtomicBoolean stopSearching = new AtomicBoolean(false);
    private final AtomicBoolean showMessage = new AtomicBoolean(true);

    public ProcessManagingService() {
        init();
    }

    // 생성자 또는 @PostConstruct 된 메서드에서 실행
    private void init() {
        deleteFile.scheduleAtFixedRate(this::deleteOldFiles, delete_interval, delete_interval, TimeUnit.MINUTES);
        deleteFile.scheduleAtFixedRate(() -> deleteOldThumbnails(thumbnailPath), delete_interval, delete_interval, TimeUnit.MINUTES);
    }

    private void walkFiles(Path path, Consumer<Path> consumer) {
        if (Files.isDirectory(path)) {
            try {
                Files.walk(path)
                        .filter(Files::isRegularFile)
                        .forEach(consumer);
            } catch (IOException e) {
                log.error("Error walking directory {}", path.toAbsolutePath(), e);
            }
        } else {
            log.warn("Skipping non-directory: {}", path.toAbsolutePath());
        }
    }


    private void deleteOldFiles() {
        if (stopSearching.get()) {
            if (showMessage.getAndSet(false)) {
                log.info("No longer searching for files.");
            }
            return;
        }

        log.info("Deleting .ts files ...");

        Path directoryPath = Paths.get(path);

        try (Stream<Path> filePaths = Files.list(directoryPath)) {
            filePaths.filter(Files::isDirectory)
                    .forEach(dirPath -> {
                        // 1분 미만으로 수정된 파일이 있는지 확인
                        AtomicBoolean hasFiles = new AtomicBoolean(false);
                        walkFiles(dirPath, file -> {
                            try {
                                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                                Instant currentInstant = Instant.now();
                                final long oneMinuteAgo = currentInstant.minus(1, ChronoUnit.MINUTES).toEpochMilli();

                                // 1분 이상된 파일 삭제
                                if (FileTime.fromMillis(oneMinuteAgo).compareTo(attributes.creationTime()) >= 0) {
                                    Files.delete(file);
                                    log.info("File deleted: {}", file);
                                } else {
                                    // 방문한 파일 중 하나가 아직 삭제되지 않았음을 나타냄
                                    hasFiles.set(true);
                                }
                            } catch (IOException e) {
                                log.error("Failed to read attributes or delete file {}", file, e);
                            }
                        });

                        // 1분 미만으로 수정된 파일이 없으면 검색 중지
                        if (!hasFiles.get() && !initialCheck.get()) {
                            log.info("No files found that are not older than 1 minute in {}. Stopping the search.", dirPath);
                            stopSearching.set(true);
                        }
                    });

            // 첫 실행 후 초기 검사 비활성화
            if (initialCheck.get()) {
                initialCheck.set(false);
            }
        } catch (IOException e) {
            log.error("Failed to list directories", e);
        }
    }

    public void deleteOldThumbnails(String thumbnailPath) {
        File folder = new File(thumbnailPath);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".jpg")) {
                    try {
                        Path filePath = Paths.get(file.getAbsolutePath());
                        BasicFileAttributes attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
                        Instant fileCreationTime = attributes.creationTime().toInstant();
                        Instant currentInstant = Instant.now();
                        Duration fileAge = Duration.between(fileCreationTime, currentInstant);

                        if (fileAge.toMinutes() >= 1) {
                            if (file.delete()) {
                                log.info("Thumbnail deleted: " + file.getName());
                            } else {
                                log.error("Failed to delete: " + file.getName());
                            }
                        }
                    } catch (IOException e) {
                        log.error("Error while reading file attributes", e);
                    }
                }
            }
        } else {
            log.info("No files found in the directory.");
        }
    }

    public Mono<Long> startProcess(String owner){
        // isAlive() - 하위 프로세스가 Process활성 상태인지 테스트
        if(processMap.containsKey(owner) && processMap.get(owner).isAlive()){
            // 하위 프로세스를 종료
            processMap.get(owner).destroyForcibly();
            processMap.remove(owner);
        }
        // 썸네일 출력 경로를 설정합니다. 여기에서는 thumbnailPath와 owner 이름을 이용해 저장 경로를 지정합니다.
        String thumbnailOutputPath = Paths.get(thumbnailPath, owner + "_thumbnail_%04d.jpg").toString();
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
                    });
                    processMap.put(owner, process);
                    // 프로세스의 기본 프로세스 ID를 반환합니다. 기본 프로세스 ID는 운영 체제가 프로세스에 할당하는 식별 번호
                    return Mono.just(process.pid());
                    // 블로킹 IO 태스크와 같은 생명주기가 긴 태스크들에 적합하다.
                    // boundedElastic 은 요청 할때마다 스레드 생성 단, 스레드 수 제한
                }).subscribeOn(Schedulers.boundedElastic());
    }
}