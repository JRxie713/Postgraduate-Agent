package com.demo.scaffoldbackend.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 分片上传：上传前检查（check 接口）的返回结果
 */
@Data
public class FileCheckVO {

    /** 文件唯一标识，原样带回给前端 */
    private String fileId;

    /** 分片总数 */
    private Integer totalChunks;

    /**
     * 服务端已经收到的分片序号，从 1 开始，升序排列。
     * 前端只要跳过这些序号，就实现了「断点续传」。
     */
    private List<Integer> uploadedChunks;

    /**
     * 服务端每个分片的 SHA-256，key 是分片序号。
     *
     * 前端拿到后和自己算的哈希一比对，就能发现「服务端上的这一片其实已经坏了」，
     * 从而决定重传哪几片 —— 这就是「分块损坏后通知重传」的完整闭环。
     */
    private Map<Integer, String> chunkHashes;
}