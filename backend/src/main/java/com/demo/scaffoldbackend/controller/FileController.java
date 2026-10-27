package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件模块：上传 / 下载
 *
 * 当前是「最简单版本」：整个文件一次性上传，直接存到本地磁盘。
 * 后续加功能时再往上叠：分片上传、秒传、断点续传、对象存储。
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
     */
    @GetMapping("/download/{storedName}")
    public ResponseEntity<Resource> download(@PathVariable String storedName) throws IOException {
        // 防止有人构造 ../../xxx 这样的路径去读服务器上的其他文件
        if (storedName.contains("/") || storedName.contains("\\") || storedName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        Path dir = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path target = dir.resolve(storedName);
        if (!Files.exists(target)) {
            return ResponseEntity.notFound().build();
        }

        // 下载时把名字还原成用户当初上传的那个名字
        int split = storedName.indexOf('_');
        String downloadName = (split >= 0 && split < storedName.length() - 1)
                ? storedName.substring(split + 1)
                : storedName;

        Resource resource = new FileSystemResource(target);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                URLEncoder.encode(downloadName, StandardCharsets.UTF_8) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}