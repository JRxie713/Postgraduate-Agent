package com.demo.scaffoldbackend.service.impl;

import com.demo.scaffoldbackend.service.SmallFileService;
import com.demo.scaffoldbackend.vo.SmallFileVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 小文件批量读写：合并存储（类似 Haystack / 淘宝 TFS 的思路）
 *
 * 磁盘上的样子（uploadRoot = ./data/upload）：
 *
 *   ./data/upload/small/000001.blk    ← 第一个大块，一堆小文件首尾相接拼在这里
 *   ./data/upload/small/000002.blk    ← 第一个写满 64MB 后再开一个
 *   ./data/upload/small/index.idx     ← 索引，一行一个文件：
 *                                        key|block|offset|length|fileName
 *
 * 为什么这样省事：
 *   - 磁盘上只有几个大文件，block 数量 = 总字节 / 64MB，不会因为小文件多而爆目录
 *   - 读小文件不用打开百万个文件句柄，只要 open 对应 block 再 seek 到 offset
 *
 * 代价：
 *   - 删一个文件只能标记删除 / 定期重写 block，不能直接删（这里没做，属于作业外的优化）
 *   - 索引必须持久化，丢了 block 里的字节就找不回来了
 */
@Service
public class SmallFileServiceImpl implements SmallFileService {

    private static final Logger log = LoggerFactory.getLogger(SmallFileServiceImpl.class);

    /** 一个大块文件的上限：64MB，写满就开下一个 */
    private static final long BLOCK_MAX_BYTES = 64L * 1024 * 1024;

    /** 大块文件和索引都放在这个子目录下 */
    private static final String DIR_NAME = "small";

    /** 索引文件名 */
    private static final String INDEX_FILE = "index.idx";

    /** key 的格式，要拿去拼文件名和查表，所以限制得死一点 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,32}$");

    @Value("${file.upload.root}")
    private String uploadRoot;

    /** key -> 这个文件躺在大块的哪一段 */
    private final Map<String, BlockEntry> index = new ConcurrentHashMap<>();

    /** block 文件名 -> 已经写到第几字节（也就是下一个文件的 offset） */
    private final Map<String, Long> blockSizes = new HashMap<>();

    /** 写 block 和写索引都要串行，否则两个请求会算出同一个 offset 互相覆盖 */
    private final Object writeLock = new Object();

    private volatile boolean loaded = false;

    // ==================== 写入 ====================

    @Override
    public List<SmallFileVO> saveBatch(String[] fileNames, byte[][] contents) throws IOException {
        if (contents == null || contents.length == 0) {
            throw new IllegalArgumentException("没有要保存的文件");
        }

        List<SmallFileVO> result = new ArrayList<>(contents.length);

        // 整批加一次锁，而不是每写一个文件加一次，这样批量写更快也更安全
        synchronized (writeLock) {
            ensureLoaded();

            for (int i = 0; i < contents.length; i++) {
                byte[] data = contents[i];
                if (data == null || data.length == 0) {
                    throw new IllegalArgumentException("第 " + (i + 1) + " 个文件内容是空的");
                }
                String fileName = (fileNames != null && i < fileNames.length) ? fileNames[i] : null;
                result.add(appendOne(fileName, data));
            }
        }

        log.info("小文件批量写入完成：本次 {} 个，索引中共 {} 个", contents.length, index.size());
        return result;
    }

    /** 把一个小文件追加到大块末尾 */
    private SmallFileVO appendOne(String fileName, byte[] data) throws IOException {
        // 文件名里的 | 会被替换掉，正好避免把索引的字段分隔符搞乱
        String safeName = (fileName == null || fileName.isBlank())
                ? "file"
                : fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        long currentSize = currentBlockSize();
        String block;
        long offset;

        if (currentSize > 0 && currentSize + data.length > BLOCK_MAX_BYTES) {
            // 当前大块装不下了，开一个新的
            block = nextBlockName();
            offset = 0;
        } else {
            block = currentBlockName();
            offset = currentSize;
        }

        Path blockPath = smallDir().resolve(block);
        try (FileChannel channel = FileChannel.open(blockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }

        blockSizes.put(block, offset + data.length);

        BlockEntry entry = new BlockEntry(newKey(), block, offset, data.length, safeName, md5(data));
        index.put(entry.key, entry);
        appendIndexLine(entry);

        return toVO(entry);
    }

    // ==================== 读取 ====================

    @Override
    public SmallFileVO get(String key) {
        return toVO(lookup(key));
    }

    @Override
    public byte[] read(String key) {
        BlockEntry entry = lookup(key);
        Path blockPath = smallDir().resolve(entry.block);

        try (FileChannel channel = FileChannel.open(blockPath, StandardOpenOption.READ)) {
            // 关键一步：直接跳到这个文件在大块里的起始位置，只读它自己那一段
            channel.position(entry.offset);
            ByteBuffer buffer = ByteBuffer.allocate(entry.length);
            while (buffer.hasRemaining()) {
                if (channel.read(buffer) < 0) {
                    break;
                }
            }
            return buffer.array();
        } catch (IOException e) {
            throw new RuntimeException("从大块 " + entry.block + " 读取失败", e);
        }
    }

    @Override
    public List<SmallFileVO> list(List<String> keys) {
        List<SmallFileVO> result = new ArrayList<>();
        if (keys == null) {
            return result;
        }
        for (String key : keys) {
            result.add(get(key));
        }
        return result;
    }

    // ==================== 索引的加载与追加 ====================

    /** 第一次用到的时候把 index.idx 全量读进内存 */
    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (writeLock) {
            if (loaded) {
                return;
            }
            try {
                Path dir = smallDir();
                Files.createDirectories(dir);
                Path indexPath = dir.resolve(INDEX_FILE);

                if (Files.exists(indexPath)) {
                    for (String line : Files.readAllLines(indexPath, StandardCharsets.UTF_8)) {
                        BlockEntry entry = parseIndexLine(line);
                        if (entry != null) {
                            index.put(entry.key, entry);
                            long end = entry.offset + entry.length;
                            blockSizes.merge(entry.block, end, Math::max);
                        }
                    }
                }
                loaded = true;
                log.info("小文件索引加载完成：{} 个文件，{} 个大块", index.size(), blockSizes.size());
            } catch (IOException e) {
                throw new RuntimeException("加载小文件索引失败：" + smallDir().resolve(INDEX_FILE), e);
            }
        }
    }

    /** 索引是追加写的：一行一个文件，追加完立刻刷盘，服务重启也不会丢 */
    private void appendIndexLine(BlockEntry entry) throws IOException {
        String line = entry.key + "|" + entry.block + "|" + entry.offset + "|" + entry.length + "|"
                + entry.md5 + "|" + entry.fileName + System.lineSeparator();
        Files.write(smallDir().resolve(INDEX_FILE),
                line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    private BlockEntry parseIndexLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        // limit = 6：文件名里万一还有 | 也不会被切坏，整段都归文件名
        String[] parts = line.split("\\|", 6);
        if (parts.length < 6) {
            return null;
        }
        try {
            return new BlockEntry(parts[0], parts[1], Long.parseLong(parts[2]),
                    Integer.parseInt(parts[3]), parts[5], parts[4]);
        } catch (NumberFormatException e) {
            log.warn("索引里有一行格式不对，已跳过：{}", line);
            return null;
        }
    }

    // ==================== 内部小工具 ====================

    private BlockEntry lookup(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("key 不合法");
        }
        ensureLoaded();
        BlockEntry entry = index.get(key);
        if (entry == null) {
            throw new IllegalArgumentException("找不到这个文件：" + key);
        }
        return entry;
    }

    private Path smallDir() {
        return Paths.get(uploadRoot).toAbsolutePath().normalize().resolve(DIR_NAME);
    }

    /** 当前正在写的那个大块的名字，没有就从 000001.blk 开始 */
    private String currentBlockName() {
        return blockSizes.keySet().stream().max(Comparator.naturalOrder()).orElse("000001.blk");
    }

    private long currentBlockSize() {
        return blockSizes.getOrDefault(currentBlockName(), 0L);
    }

    /** 下一个大块的名字，序号 +1 并补零到 6 位，保证按名字排序就是写入顺序 */
    private String nextBlockName() {
        String current = currentBlockName();
        int seq = Integer.parseInt(current.substring(0, current.indexOf('.')));
        return String.format("%06d.blk", seq + 1);
    }

    private String newKey() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private SmallFileVO toVO(BlockEntry entry) {
        SmallFileVO vo = new SmallFileVO();
        vo.setKey(entry.key);
        vo.setFileName(entry.fileName);
        vo.setSize((long) entry.length);
        vo.setMd5(entry.md5);
        vo.setBlock(entry.block);
        vo.setOffset(entry.offset);
        return vo;
    }

    private String md5(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // JDK 一定带 MD5，走不到这里
            throw new IllegalStateException(e);
        }
    }

    /** 一个小文件在大块里的位置信息 */
    private static class BlockEntry {
        final String key;
        final String block;
        final long offset;
        final int length;
        final String fileName;
        final String md5;

        BlockEntry(String key, String block, long offset, int length, String fileName, String md5) {
            this.key = key;
            this.block = block;
            this.offset = offset;
            this.length = length;
            this.fileName = fileName;
            this.md5 = md5;
        }
    }
}