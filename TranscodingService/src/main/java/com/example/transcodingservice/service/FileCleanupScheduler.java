//package com.example.transcodingservice.service;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.DisposableBean;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.time.Duration;
//import java.time.Instant;
//
//@Slf4j
//@Service
//public class FileCleanupScheduler implements DisposableBean {
//
//    @Value("${stream.directory}")
//    private String path;
//
//    private long fileTTL = 1L; // 파일의 TTL을 분 단위로 설정
//    @Scheduled(fixedDelay = 120000) // 2분(60,000밀리초)마다 실행
//    public void cleanupFiles(String owner) throws IOException {
//        log.info("* File cleaner : Started");
//        Path directory = Paths.get(path).resolve(owner);
//        if (Files.isDirectory(directory)) {
//            Files.list(directory)
//                    .filter(Files::isDirectory)
//                    .forEach(subDirectory->{
//                        try{
//                           Files.list(subDirectory)
//                                   .filter(file -> Files.isRegularFile(file) && file.toString().endsWith(".ts"))
//                                   .forEach(tsFile -> {
//                                       try {
//                                           Instant creationTime = Files.getLastModifiedTime(tsFile).toInstant();
//                                           Instant currentTime = Instant.now();
//                                           Duration duration = Duration.between(creationTime, currentTime);
//                                           long minutesElapsed = duration.toMinutes();
//                                           if (minutesElapsed >= fileTTL) {
//                                               Files.delete(tsFile);
//                                               log.info("* File Cleaner: Deleted file " + tsFile.getFileName());
//                                           }
//                                       } catch (IOException e) {
//                                           log.error("* File Cleaner: Failed to delete file " + tsFile.getFileName());
//                                           e.printStackTrace();
//                                       }
//                                   });
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    });
//        }
//
//        if (Files.list(directory).noneMatch(Files::isRegularFile)) {
//            destroy();
//        }
//    }
//
//    @Override
//    public void destroy(){
//        log.info("* File Cleaner: Stopping scheduler and cleaning up resources.");
//    }
//}
