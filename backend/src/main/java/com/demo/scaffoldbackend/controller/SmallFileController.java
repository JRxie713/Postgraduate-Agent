package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.service.SmallFileService;
import com.demo.scaffoldbackend.vo.SmallFileVO;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件模块：小文件批量读写
 *
 * 磁盘上不是「一个文件一个文件」，而是全部拼进 64MB 的 .blk 大块里，
 * 靠 index.idx 里记的「偏移量 + 长度」找回来。
 *
 * 三个接口：
 *   POST /api/file/small/batch-upload     一次传多个小文件
 *   GET  /api/file/small/download/{key}   按 key 取回一个
 *   POST /api/file/small/batch-download   一次把多个 key 打包成 zip 取回
 */
@RestController
@RequestMapping("/api/file/small")
public class SmallFileController {

    private static final Logger log = LoggerFactory.getLogger(SmallFileController.class);

    private final SmallFileService smallFileService;

    public SmallFileController(SmallFileService smallFileService) {
        this.smallFileService = smallFileService;
    }

    /**
     * 批量上传
     *
     * 请求：POST /api/file/small/batch-upload  表单字段名固定为 files，可以重复出现多次
     */
    @PostMapping("/batch-upload")
    public Result<List<SmallFileVO>> batchUpload(@RequestParam("files") MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            return Result.error(400, "没有选择文件");
        }

        String[] names = new String[files.length];
        byte[][] contents = new byte[files.length][];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getOriginalFilename();
            contents[i] = files[i].getBytes();
        }

        List<SmallFileVO> saved = smallFileService.saveBatch(names, contents);
        return Result.success(saved);
    }

    /**
     * 单个读取
     *
     * 请求：GET /api/file/small/download/{key}
     * 响应头里带 X-File-Md5，前端可以直接和自己上传时拿到的 md5 对比，验证存读一致
     */
    @GetMapping("/download/{key}")
    public ResponseEntity<byte[]> download(@PathVariable String key) {
        SmallFileVO meta = smallFileService.get(key);
        byte[] content = smallFileService.read(key);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                URLEncoder.encode(meta.getFileName(), StandardCharsets.UTF_8) + "\"")
                .header("X-File-Md5", meta.getMd5() == null ? "" : meta.getMd5())
                .header("X-File-Block", meta.getBlock())
                .header("X-File-Offset", String.valueOf(meta.getOffset()))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    /**
     * 批量读取：一次把多个小文件打包成 zip 返回
     *
     * 请求：POST /api/file/small/batch-download   body: ["key1","key2",...]
     */
    @PostMapping("/batch-download")
    public void batchDownload(@RequestBody List<String> keys, HttpServletResponse response) throws IOException {
        if (keys == null || keys.isEmpty()) {
            response.sendError(400, "没有要下载的文件");
            return;
        }

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + URLEncoder.encode("small-files.zip", StandardCharsets.UTF_8) + "\"");

        // 一边读一边往输出流写，不把整个 zip 攒在内存里
        try (OutputStream out = response.getOutputStream();
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {

            int seq = 1;
            for (String key : keys) {
                SmallFileVO meta = smallFileService.get(key);
                byte[] content = smallFileService.read(key);

                // zip 里不允许重名，加个序号前缀
                zip.putNextEntry(new ZipEntry(seq + "_" + meta.getFileName()));
                zip.write(content);
                zip.closeEntry();
                seq++;
            }
        }

        log.info("小文件打包下载完成：{} 个", keys.size());
    }
}