package com.demo.scaffoldbackend.controller;

import com.demo.scaffoldbackend.common.result.PageResult;
import com.demo.scaffoldbackend.common.result.Result;
import com.demo.scaffoldbackend.dto.SchoolMajorDTO;
import com.demo.scaffoldbackend.vo.SchoolMajorVO;
import org.springframework.web.bind.annotation.*;

/**
 * 择校模块
 */
@RestController
@RequestMapping("/api/school")
public class SchoolController {

    /** 分页查询院校专业列表（可按院校/专业过滤） */
    @GetMapping
    public Result<PageResult<SchoolMajorVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size,
                                                  @RequestParam(required = false) String school,
                                                  @RequestParam(required = false) String major) {
        return null;
    }

    /** 根据ID查询院校专业 */
    @GetMapping("/{id}")
    public Result<SchoolMajorVO> getById(@PathVariable Integer id) {
        return null;
    }

    /** 新增院校专业 */
    @PostMapping
    public Result<SchoolMajorVO> create(@RequestBody SchoolMajorDTO schoolMajorDTO) {
        return null;
    }

    /** 更新院校专业 */
    @PutMapping("/{id}")
    public Result<SchoolMajorVO> update(@PathVariable Integer id, @RequestBody SchoolMajorDTO schoolMajorDTO) {
        return null;
    }

    /** 删除院校专业 */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return null;
    }
}
