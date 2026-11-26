package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.dto.StudyCheckinDTO;
import com.demo.scaffoldbackend.vo.StudyCheckinVO;
import com.demo.scaffoldbackend.vo.StudyOverviewVO;
import com.demo.scaffoldbackend.vo.StudyRankVO;

import java.util.List;

/**
 * 学习打卡服务：Redis 实时缓存 / 计数 / 去重 / 排行 + Kafka 异步事件聚合
 */
public interface StudyCheckinService {

    /**
     * 每日学习打卡
     *
     * @param dto 打卡请求
     * @return 打卡结果
     */
    StudyCheckinVO checkIn(StudyCheckinDTO dto);

    /**
     * 查询个人学习概览（Hash 缓存，Cache-Aside 读穿透）
     *
     * @param userId 用户 ID
     * @return 学习概览
     */
    StudyOverviewVO overview(Integer userId);

    /**
     * 连续打卡天数排行榜
     *
     * @param top 前 N 名
     * @return 榜单
     */
    List<StudyRankVO> streakRank(int top);

    /**
     * 今日学习时长排行榜
     *
     * @param top 前 N 名
     * @return 榜单
     */
    List<StudyRankVO> todayMinutesRank(int top);

    /**
     * 主动失效个人概览缓存
     *
     * @param userId 用户 ID
     */
    void evictOverviewCache(Integer userId);
}
