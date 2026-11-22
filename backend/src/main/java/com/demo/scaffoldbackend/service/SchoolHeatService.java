package com.demo.scaffoldbackend.service;

import com.demo.scaffoldbackend.dto.SchoolHeatDTO;
import com.demo.scaffoldbackend.vo.SchoolCardVO;
import com.demo.scaffoldbackend.vo.SchoolHeatVO;
import com.demo.scaffoldbackend.vo.SchoolOptionVO;
import com.demo.scaffoldbackend.vo.SchoolTrackVO;

import java.util.List;

/**
 * 院校热度服务：Redis 当日去重 / 计数 / ZSet 排行 + Kafka 异步聚合历史热度
 */
public interface SchoolHeatService {

    /**
     * 上报一次院校浏览 / 关注 / 取关行为
     *
     * @param dto 热度埋点请求
     * @return 该校当前热度信息
     */
    SchoolHeatVO record(SchoolHeatDTO dto);

    /**
     * 院校热度排行榜
     *
     * @param top 前 N 名
     * @return 热度榜
     */
    List<SchoolHeatVO> rank(int top);

    /**
     * 可选院校列表（去重后的院校名称 + 记录 ID）
     *
     * @return 院校下拉项
     */
    List<SchoolOptionVO> options();

    /**
     * 择校面板卡片列表：含关注人数、当前用户是否已关注
     *
     * @param userId 当前用户 ID（可空，未登录时 followed 一律 false）
     * @return 院校卡片
     */
    List<SchoolCardVO> cards(Integer userId);

    /**
     * 当前用户今日浏览 / 关注过的院校明细
     *
     * @param userId 用户 ID
     * @return 今日行为明细
     */
    List<SchoolTrackVO> myTracks(Integer userId);
}
