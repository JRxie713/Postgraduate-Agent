package com.demo.scaffoldbackend.service.impl;

import com.demo.scaffoldbackend.service.FileChunkService;
import com.demo.scaffoldbackend.vo.FileCheckVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 分片上传 + 后台重组
 *
 * 磁盘上的样子（uploadRoot = ./data/upload）：
 *
 *   ./data/upload/chunks/{fileId}/00000001.part    ← 还没合并的分片
 *   ./data/upload/{8位随机串}_{原名}                ← 合并后的完整文件
 *
 * 合并出来的文件故意放在和「普通上传」同一个目录，
 * 这样下载接口 /api/file/download/{文件名} 不用改，小文件大文件共用一套下载逻辑。
 */
@Service
public class FileChunkServiceImpl implements FileChunkService {

    private static final Logger log = LoggerFactory.getLogger(FileChunkServiceImpl.class);

    /**
     * fileId 是前端传上来的，服务端要拿它拼目录名。
     * 只允许字母数字和 _ - ，长度限制 64，防止有人传 ../../ 来读别的目录。
     */
    private static final Pattern FILE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    /** 分片临时目录名（在 uploadRoot 下面） */
    private static final String CHUNK_DIR_NAME = "chunks";

    /** 分片文件后缀 */
    private static final String PART_SUFFIX = ".part";

    /**
     * 分片哈希文件后缀。
     * 每收到一片就顺手在同一个目录写一个 00000001.sha256，
     * 合并前重新算一遍哈希和它对，就能发现「片在磁盘上待了很久之后坏了」。
     */
    private static final String HASH_SUFFIX = ".sha256";

    /** 每个文件一把锁：防止同一个文件被并发合并两次 */
    private final ConcurrentHashMap<String, Object> mergeLocks = new ConcurrentHashMap<>();

    /** 文件存到哪个目录，在 application.yml 的 file.upload.root 里配 */
    @Value("${file.upload.root}")
    private String uploadRoot;

    // ==================== 对外三个方法 ====================

    @Override
    public FileCheckVO check(String fileId, String fileName, Integer totalChunks) {
        String id = checkFileId(fileId);
        checkTotalChunks(totalChunks);

        Path dir = chunkDir(id);
        List<Integer> uploaded = new ArrayList<>();
        Map<Integer, String> hashes = new LinkedHashMap<>();

        // 目录不存在 = 一片都没传过，这是正常的，直接返回空列表
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                files.forEach(p -> {
                    String name = p.getFileName().toString();
                    // 只认 00000001.part 这种格式
                    if (!name.endsWith(PART_SUFFIX)) {
                        return;
                    }
                    int index;
                    try {
                        index = Integer.parseInt(name.substring(0, name.length() - PART_SUFFIX.length()));
                    } catch (NumberFormatException ignore) {
                        // 不是我们生成的文件名，忽略
                        return;
                    }
                    uploaded.add(index);

                    // 顺手把这一片上传时记下的哈希也带回去，前端用来判断片有没有坏
                    Path hashFile = dir.resolve(hashName(index));
                    if (Files.exists(hashFile)) {
                        try {
                            hashes.put(index, Files.readString(hashFile, StandardCharsets.UTF_8).trim());
                        } catch (IOException e) {
                            // 哈希文件读不出来，就当这片不可信，前端会重传
                            log.warn("读取分片哈希失败：{}", hashFile);
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("读取分片目录失败：" + dir, e);
            }
        }

        Collections.sort(uploaded);
        log.info("检查分片：fileId={}, 已收到 {}/{} 片", id, uploaded.size(), totalChunks);

        FileCheckVO vo = new FileCheckVO();
        vo.setFileId(id);
        vo.setTotalChunks(totalChunks);
        vo.setUploadedChunks(uploaded);
        vo.setChunkHashes(hashes);
        return vo;
    }

    @Override
    public boolean saveChunk(String fileId, Integer chunkIndex, MultipartFile chunk, String chunkSha256) throws IOException {
        String id = checkFileId(fileId);
        if (chunkIndex == null || chunkIndex < 1) {
            throw new IllegalArgumentException("分片序号必须从 1 开始");
        }
        if (chunk == null || chunk.isEmpty()) {
            throw new IllegalArgumentException("分片内容为空");
        }

        Path dir = chunkDir(id);
        Files.createDirectories(dir);

        Path part = dir.resolve(partName(chunkIndex));
        // 重复上传同一片时直接覆盖，这样重试逻辑不用特殊处理
        chunk.transferTo(part);

        // 落盘之后重新算一遍哈希，和前端传来的比对：
        // 传输出错、磁盘写坏、被中间设备篡改，都会在这一步暴露出来
        String actual = sha256(part);
        if (chunkSha256 != null && !chunkSha256.isBlank()
                && !actual.equalsIgnoreCase(chunkSha256.trim())) {
            // 坏片必须删掉。留着的话合并时会被拼进成品，
            // 生成一个「看起来正常、实际内容已损坏」的文件，比直接失败更危险
            Files.deleteIfExists(part);
            log.warn("分片校验失败：fileId={}, 第 {} 片，前端 {} / 实际 {}",
                    id, chunkIndex, chunkSha256, actual);
            return false;
        }

        // 记下这一片的真实哈希，合并前还要再比对一次
        Files.writeString(dir.resolve(hashName(chunkIndex)), actual, StandardCharsets.UTF_8);

        log.info("收到分片：fileId={}, 第 {} 片, {} 字节, sha256={}",
                id, chunkIndex, chunk.getSize(), actual);
        return true;
    }

    @Override
    public String merge(String fileId, String fileName, Integer totalChunks) throws IOException {
        String id = checkFileId(fileId);
        checkTotalChunks(totalChunks);

        // 同一个文件的合并过程串行化
        Object lock = mergeLocks.computeIfAbsent(id, k -> new Object());
        synchronized (lock) {
            try {
                Path dir = chunkDir(id);

                // 1. 先确认一片都不少，少一片就拒绝合并（避免拼出一个坏文件）
                List<Integer> missing = new ArrayList<>();
                for (int i = 1; i <= totalChunks; i++) {
                    if (!Files.exists(dir.resolve(partName(i)))) {
                        missing.add(i);
                    }
                }
                if (!missing.isEmpty()) {
                    throw new IllegalArgumentException("缺少第 " + missing + " 个分片，无法合并");
                }

                // 2. 再确认每一片的内容都没坏：重算哈希，和上传时记下的一一比对。
                //    这一步挡的是「片传成功了、但在磁盘上放坏了/被人改了」的情况
                List<Integer> corrupt = new ArrayList<>();
                for (int i = 1; i <= totalChunks; i++) {
                    Path part = dir.resolve(partName(i));
                    Path hashFile = dir.resolve(hashName(i));
                    if (!Files.exists(hashFile)) {
                        corrupt.add(i);       // 没有哈希记录，这片不可信
                        continue;
                    }
                    String expected = Files.readString(hashFile, StandardCharsets.UTF_8).trim();
                    if (!sha256(part).equalsIgnoreCase(expected)) {
                        corrupt.add(i);       // 内容对不上，这片坏了
                    }
                }
                if (!corrupt.isEmpty()) {
                    // 坏片留着没意义，删掉；前端收到错误后会只补传这几片
                    for (Integer i : corrupt) {
                        Files.deleteIfExists(dir.resolve(partName(i)));
                        Files.deleteIfExists(dir.resolve(hashName(i)));
                    }
                    throw new IllegalArgumentException("分片校验失败，需重传第 " + corrupt + " 片");
                }

                // 3. 给合并后的文件起名，规则和普通上传保持一致
                String originalName = (fileName == null || fileName.isBlank()) ? "file" : fileName;
                String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
                String storedName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;

                Path outDir = Paths.get(uploadRoot).toAbsolutePath().normalize();
                Files.createDirectories(outDir);
                Path target = outDir.resolve(storedName);

                // 4. 按 1,2,3... 的顺序把分片首尾相接写进目标文件
                long totalBytes = 0;
                try (FileChannel out = FileChannel.open(target,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    for (int i = 1; i <= totalChunks; i++) {
                        try (FileChannel in = FileChannel.open(dir.resolve(partName(i)), StandardOpenOption.READ)) {
                            long size = in.size();
                            long position = 0;
                            // transferTo 由操作系统直接搬运数据，不用先读进 Java 内存（零拷贝）
                            while (position < size) {
                                position += in.transferTo(position, size - position, out);
                            }
                            totalBytes += size;
                        }
                    }
                }

                // 5. 合并成功，把分片目录删掉，否则磁盘会越积越多
                deleteDirectory(dir);

                log.info("合并完成：fileId={} -> {}（共 {} 片 / {} 字节）", id, storedName, totalChunks, totalBytes);
                return "/api/file/download/" + storedName;

            } finally {
                mergeLocks.remove(id);
            }
        }
    }

    // ==================== 内部小工具 ====================

    /** 校验并返回合法的 fileId */
    private String checkFileId(String fileId) {
        if (fileId == null || !FILE_ID_PATTERN.matcher(fileId).matches()) {
            throw new IllegalArgumentException("fileId 不合法");
        }
        return fileId;
    }

    private void checkTotalChunks(Integer totalChunks) {
        if (totalChunks == null || totalChunks < 1) {
            throw new IllegalArgumentException("分片总数必须大于 0");
        }
    }

    /** 某个文件的分片目录：{uploadRoot}/chunks/{fileId} */
    private Path chunkDir(String fileId) {
        return Paths.get(uploadRoot).toAbsolutePath().normalize()
                .resolve(CHUNK_DIR_NAME)
                .resolve(fileId);
    }

    /** 分片文件名：序号补零到 8 位，保证按文件名排序就是正确的拼接顺序 */
    private String partName(int chunkIndex) {
        return String.format("%08d.part", chunkIndex);
    }

    /** 分片哈希文件名：和分片一一对应，例如 00000001.sha256 */
    private String hashName(int chunkIndex) {
        return String.format("%08d%s", chunkIndex, HASH_SUFFIX);
    }

    /** 算一个文件的 SHA-256，返回 64 位小写十六进制字符串 */
    private String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            // JDK 一定带 SHA-256，走不到这里
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
        byte[] buffer = new byte[8192];
        try (InputStream in = Files.newInputStream(file)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                digest.update(buffer, 0, n);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** 递归删除目录（合并完清分片用） */
    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            // 先删文件后删目录，所以要倒序
            List<Path> paths = walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
            for (Path p : paths) {
                Files.deleteIfExists(p);
            }
        }
    }
}