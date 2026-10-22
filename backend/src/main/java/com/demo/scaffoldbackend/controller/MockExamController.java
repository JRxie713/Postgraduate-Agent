package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.MockExamDTO;
import com.demo.scaffoldbackend.dto.MockExamStartDTO;
import com.demo.scaffoldbackend.dto.UserAnswerRecordDTO;
import com.demo.scaffoldbackend.vo.MockExamVO;
import com.demo.scaffoldbackend.vo.UserAnswerRecordVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模考模块
 */
@RestController
@RequestMapping("/api/mock-exam")
public class MockExamController {

    // ===== 模考记录 =====

    /** 根据用户ID查询模考记录列表 */
    @GetMapping("/user/{userId}")
    public Result<List<MockExamVO>> listByUserId(@PathVariable Integer userId) {
        return null;
    }

    /** 分页查询模考记录列表 */
    @GetMapping
    public Result<PageResult<MockExamVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 根据ID查询模考记录 */
    @GetMapping("/{id}")
    public Result<MockExamVO> getById(@PathVariable Integer id) {
        return null;
    }

    /** 开始模考 */
    @PostMapping("/start")
    public Result<MockExamVO> start(@RequestBody MockExamStartDTO startDTO) {
        return null;
    }

    /** 交卷 */
    @PostMapping("/{id}/submit")
    public Result<MockExamVO> submit(@PathVariable Integer id) {
        return null;
    }

    /** 更新模考记录 */
    @PutMapping("/{id}")
    public Result<MockExamVO> update(@PathVariable Integer id, @RequestBody MockExamDTO mockExamDTO) {
        return null;
    }

    /** 删除模考记录 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return null;
    }

    // ===== 答题记录 =====

    /** 根据模考ID查询答题记录 */
    @GetMapping("/record/mock-exam/{mockExamId}")
    public Result<List<UserAnswerRecordVO>> listRecordsByMockExamId(@PathVariable Integer mockExamId) {
        return null;
    }

    /** 根据用户ID查询答题记录 */
    @GetMapping("/record/user/{userId}")
    public Result<List<UserAnswerRecordVO>> listRecordsByUserId(@PathVariable Integer userId) {
        return null;
    }

    /** 根据试卷ID查询答题记录 */
    @GetMapping("/record/paper/{paperId}")
    public Result<List<UserAnswerRecordVO>> listRecordsByPaperId(@PathVariable Integer paperId) {
        return null;
    }

    /** 分页查询答题记录列表 */
    @GetMapping("/record")
    public Result<PageResult<UserAnswerRecordVO>> listRecords(@RequestParam(defaultValue = "1") Integer page,
                                                              @RequestParam(defaultValue = "10") Integer size) {
        return null;
    }

    /** 根据ID查询答题记录 */
    @GetMapping("/record/{id}")
    public Result<UserAnswerRecordVO> getRecordById(@PathVariable Integer id) {
        return null;
    }

    /** 新增答题记录 */
    @PostMapping("/record")
    public Result<UserAnswerRecordVO> createRecord(@RequestBody UserAnswerRecordDTO recordDTO) {
        return null;
    }

    /** 更新答题记录 */
    @PutMapping("/record/{id}")
    public Result<UserAnswerRecordVO> updateRecord(@PathVariable Integer id, @RequestBody UserAnswerRecordDTO recordDTO) {
        return null;
    }

    /** 删除答题记录 */
    @DeleteMapping("/record/{id}")
    public Result<Boolean> deleteRecord(@PathVariable Integer id) {
        return null;
    }
}
