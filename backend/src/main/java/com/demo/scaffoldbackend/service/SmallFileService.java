package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.vo.SmallFileVO;

import java.io.IOException;
import java.util.List;

/**
 * 小文件批量读写
 *
 * 解决的痛点：几百万个几百字节的小文件如果每个都单独落盘，
 * 会把磁盘 inode 撑爆、目录扫描变慢、备份也变成灾难。
 *
 * 方案：合并存储。多个小文件首尾相接拼进一个大块文件（.blk），
 * 另外用一条索引记录每个小文件「躺在大块的哪一段」，读的时候按偏移量切回来。
 */
public interface SmallFileService {

    /**
     * 批量保存：一次把多个小文件拼进大块
     */
    List<SmallFileVO> saveBatch(String[] fileNames, byte[][] contents) throws IOException;

    /**
     * 查一个文件的元信息（不含内容）
     */
    SmallFileVO get(String key);

    /**
     * 按 key 读回文件内容
     */
    byte[] read(String key);

    /**
     * 批量读：一次校验多个 key 是否存在
     */
    List<SmallFileVO> list(List<String> keys);
}