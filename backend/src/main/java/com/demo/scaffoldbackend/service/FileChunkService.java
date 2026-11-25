package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.vo.FileCheckVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 分片上传 + 后台重组
 */
public interface FileChunkService {

    /**
     * 上传前检查：看看服务端已经收了哪些分片
     */
    FileCheckVO check(String fileId, String fileName, Integer totalChunks);

    /**
     * 保存一个分片到磁盘，并校验这一片的内容有没有坏
     *
     * @param chunkIndex  分片序号，从 1 开始
     * @param chunkSha256 前端对这一片算出来的 SHA-256，传空表示不校验
     * @return true 校验通过；false 表示内容对不上，坏片已被删除，需要前端重传这一片
     */
    boolean saveChunk(String fileId, Integer chunkIndex, MultipartFile chunk, String chunkSha256) throws IOException;

    /**
     * 把所有分片按顺序拼成一个完整文件
     *
     * @return 合并后的下载地址，和普通上传返回的格式一致
     */
    String merge(String fileId, String fileName, Integer totalChunks) throws IOException;
}