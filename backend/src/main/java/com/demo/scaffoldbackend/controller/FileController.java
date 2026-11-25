package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件模块：上传 / 下载
 *
 * 上传：整个文件一次性上传，直接存到本地磁盘（小文件走这里）。
 * 下载：支持 HTTP Range，可以实现断点续传和多线程分段并发下载。
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    /** 文件存到哪个目录，在 application.yml 的 file.upload.root 里配 */
    @Value("${file.upload.root}")
    private String uploadRoot;

    /**
     * 上传文件
     *
     * 用法：POST /api/file/upload  表单字段名固定为 file
     * 返回：一个能直接用来下载的地址
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        // 1. 空文件直接拒掉
        if (file.isEmpty()) {
            return Result.error(400, "上传的文件为空");
        }

        // 2. 给文件起个新名字：8位随机串 + "_" + 原始文件名
        //    为什么要改名？因为两个人都传 "简历.pdf" 时会互相覆盖。
        //    为什么保留原名？因为下载时能还原回原来的名字，方便看。
        String originalName = file.getOriginalFilename();
        String safeName = (originalName == null || originalName.isBlank())
                ? "file"
                : originalName.replaceAll("[\\\\/:*?\"<>|]", "_");   // 去掉文件名里不能用的字符
        String storedName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;

        // 3. 确保目录存在（第一次跑的时候目录还没有）
        Path dir = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        // 4. 真正把文件写到磁盘上
        Path target = dir.resolve(storedName);
        file.transferTo(target);

        log.info("文件上传成功：{} -> {}（{} 字节）", originalName, storedName, file.getSize());

        // 5. 把下载地址返回给前端
        return Result.success("/api/file/download/" + storedName);
    }

    /**
     * 下载文件
     *
     * 用法：GET /api/file/download/{上传时返回的文件名}
     *
     * 支持 HTTP Range（断点续传 / 多线程分段并发下载）：
     *   - 不带 Range：一次性返回整个文件，响应头带 Accept-Ranges: bytes
     *   - 带 Range: bytes=start-end：返回 206 Partial Content + Content-Range，
     *     前端暂停后从已下载字节处继续请求，就是断点续传；
     *     同时开 4 条连接各取一段，就是并发分段下载。
     */
    @GetMapping("/download/{storedName}")
    public ResponseEntity<?> download(@PathVariable String storedName,
                                      @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader)
            throws IOException {
        // 防止有人构造 ../../xxx 这样的路径去读服务器上的其他文件
        if (storedName.contains("/") || storedName.contains("\\") || storedName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Path dir = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path target = dir.resolve(storedName);
        if (!Files.exists(target)) {
            return ResponseEntity.notFound().build();
        }

        long fileSize = Files.size(target);
        String downloadName = originalName(storedName);

        // 1. 普通下载：整文件返回，流式写出，不会把整个文件读进内存
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, attachment(downloadName))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentLength(fileSize)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new FileSystemResource(target));
        }

        // 2. 解析 Range: bytes=start-end / bytes=start- / bytes=-suffix
        long start;
        long end;
        try {
            start = -1;
            end = -1;
            String spec = rangeHeader.trim();
            if (!spec.startsWith("bytes=")) {
                throw new IllegalArgumentException("unsupported range");
            }
            String[] parts = spec.substring("bytes=".length()).split("-", 2);
            if (parts[0].isBlank()) {
                // bytes=-500：取最后 500 字节
                long suffix = Long.parseLong(parts[1].trim());
                if (suffix <= 0) {
                    throw new IllegalArgumentException("bad suffix");
                }
                start = Math.max(0, fileSize - suffix);
                end = fileSize - 1;
            } else {
                start = Long.parseLong(parts[0].trim());
                end = (parts.length < 2 || parts[1].isBlank()) ? fileSize - 1 : Long.parseLong(parts[1].trim());
            }
            if (start < 0 || start >= fileSize || end < start) {
                throw new IllegalArgumentException("range out of bounds");
            }
            end = Math.min(end, fileSize - 1);
        } catch (RuntimeException e) {
            // 416：请求的区间非法，把文件总长度告诉前端，方便它修正重试
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }

        long length = end - start + 1;
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(downloadName))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                .contentLength(length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(new BoundedFileInputStream(target, start, length)));
    }

    /**
     * 文件元信息
     *
     * 用法：GET /api/file/meta/{文件地址里的文件名}
     * 前端在做「分段并发下载」之前，需要先知道文件有多大，才能算出每一段的大小。
     */
    @GetMapping("/meta/{storedName}")
    public Result<Map<String, Object>> meta(@PathVariable String storedName) throws IOException {
        if (storedName.contains("/") || storedName.contains("\\") || storedName.contains("..")) {
            return Result.error(400, "文件名不合法");
        }
        Path target = Paths.get(uploadRoot).toAbsolutePath().normalize().resolve(storedName);
        if (!Files.exists(target)) {
            return Result.error(404, "文件不存在");
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("storedName", storedName);
        meta.put("fileName", originalName(storedName));
        meta.put("size", Files.size(target));
        return Result.success(meta);
    }

    /** 从「8位随机串_原始文件名」里还原出原始文件名 */
    private String originalName(String storedName) {
        int split = storedName.indexOf('_');
        return (split >= 0 && split < storedName.length() - 1)
                ? storedName.substring(split + 1)
                : storedName;
    }

    private String attachment(String fileName) {
        return "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"";
    }

    /**
     * 只读取文件 [start, start+length) 这一段字节的输入流。
     * 用来配合 206 响应，避免把整个文件读进内存。
     */
    private static final class BoundedFileInputStream extends InputStream {

        private final InputStream delegate;
        private long remaining;

        BoundedFileInputStream(Path path, long start, long length) throws IOException {
            InputStream in = Files.newInputStream(path);
            in.skipNBytes(start);
            this.delegate = in;
            this.remaining = length;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int b = delegate.read();
            if (b >= 0) {
                remaining--;
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int n = delegate.read(buffer, offset, (int) Math.min(len, remaining));
            if (n > 0) {
                remaining -= n;
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}