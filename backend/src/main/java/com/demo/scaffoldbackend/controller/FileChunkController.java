package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.FileCheckDTO;
import com.demo.scaffoldbackend.service.FileChunkService;
import com.demo.scaffoldbackend.vo.FileCheckVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件模块：分片上传 + 后台重组
 *
 * 和 FileController（整文件直传）是两条并行的路子，
 * 小文件走 FileController，大文件走这里。
 *
 * 完整流程：
 *   1. POST /api/file/chunk/check           问服务端「哪些片已经有了」（顺便实现断点续传）
 *   2. POST /api/file/chunk/upload          把缺的片一片一片传上去
 *   3. POST /api/file/chunk/merge           片传完了，让服务端拼成一个完整文件
 *   4. GET  /api/file/download/{文件名}      用 FileController 的下载接口把文件取回来验证
 */
@RestController
@RequestMapping("/api/file/chunk")
public class FileChunkController {

    private static final Logger log = LoggerFactory.getLogger(FileChunkController.class);

    private final FileChunkService fileChunkService;

    public FileChunkController(FileChunkService fileChunkService) {
        this.fileChunkService = fileChunkService;
    }

    /**
     * 上传前检查
     *
     * 请求：POST /api/file/chunk/check   body: {"fileId":"...","fileName":"...","totalChunks":20}
     */
    @PostMapping("/check")
    public Result<FileCheckVO> check(@RequestBody FileCheckDTO dto) {
        return Result.success(fileChunkService.check(dto.getFileId(), dto.getFileName(), dto.getTotalChunks()));
    }

    /**
     * 上传单个分片
     *
     * 请求：POST /api/file/chunk/upload  表单字段：fileId、chunkIndex、chunkSha256、file
     *
     * 服务端会把收到的字节重新算一遍 SHA-256，和前端传来的一比对：
     *   对得上 → 200，这一片收下了
     *   对不上 → 409，坏片当场删除，前端看到 409 就重传这一片
     */
    @PostMapping("/upload")
    public Result<Integer> upload(@RequestParam("fileId") String fileId,
                                  @RequestParam("chunkIndex") Integer chunkIndex,
                                  @RequestParam(value = "chunkSha256", required = false) String chunkSha256,
                                  @RequestParam("file") MultipartFile file) throws IOException {
        boolean verified = fileChunkService.saveChunk(fileId, chunkIndex, file, chunkSha256);
        if (!verified) {
            // 409 是专门约定给「分片内容坏了」的状态码，和 400 参数错误区分开
            return Result.error(409, "第 " + chunkIndex + " 片校验失败，请重传");
        }
        return Result.success(chunkIndex);
    }

    /**
     * 合并分片
     *
     * 请求：POST /api/file/chunk/merge  表单字段：fileId、fileName、totalChunks
     * 返回：合并后的下载地址
     */
    @PostMapping("/merge")
    public Result<String> merge(@RequestParam("fileId") String fileId,
                                @RequestParam("fileName") String fileName,
                                @RequestParam("totalChunks") Integer totalChunks) throws IOException {
        String url = fileChunkService.merge(fileId, fileName, totalChunks);
        log.info("分片合并完成，下载地址：{}", url);
        return Result.success(url);
    }
}