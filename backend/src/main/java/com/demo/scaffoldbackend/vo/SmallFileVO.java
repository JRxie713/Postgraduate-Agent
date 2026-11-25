package com.demo.scaffoldbackend.vo;

import lombok.Data;

/**
 * 小文件的信息
 *
 * block 和 offset 是「合并存储」的关键：
 * 小文件本身不是独立文件，而是躺在某个 .blk 大块里的 [offset, offset+size) 这段字节。
 */
@Data
public class SmallFileVO {

    /** 文件 key，读的时候靠它找回文件 */
    private String key;

    /** 原始文件名 */
    private String fileName;

    /** 文件字节数 */
    private Long size;

    /** 内容的 MD5，用来校验存进去和读回来的是不是同一份 */
    private String md5;

    /** 存在哪个大块文件里，例如 000001.blk */
    private String block;

    /** 在这个大块文件里的起始偏移量 */
    private Long offset;
}