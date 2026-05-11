package com.example.asset.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.asset.common.Result;
import com.example.asset.entity.BorrowRecord;
import com.example.asset.entity.Equipment;
import com.example.asset.entity.User;
import com.example.asset.service.BorrowRecordService;
import com.example.asset.service.EquipmentService;
import com.example.asset.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/borrow-records", produces = "application/json;charset=UTF-8")
@CrossOrigin(origins = "*")
public class BorrowRecordController {

    @Autowired
    private BorrowRecordService borrowRecordService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<List<BorrowRecord>> getAll() {
        return Result.success(borrowRecordService.list());
    }

    @GetMapping("/my")
    public Result<List<BorrowRecord>> getMyRecords() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.getUserByUsername(username);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        if (currentUser != null) {
            wrapper.eq(BorrowRecord::getApplicantId, currentUser.getId());
        }
        wrapper.orderByDesc(BorrowRecord::getCreateTime);
        return Result.success(borrowRecordService.list(wrapper));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<BorrowRecord>> getPendingRecords() {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getStatus, "pending");
        wrapper.orderByAsc(BorrowRecord::getCreateTime);
        return Result.success(borrowRecordService.list(wrapper));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody BorrowRecord record) {
        if (record.getAssetId() == null) {
            return Result.error("资产ID不能为空");
        }
        if (record.getBorrowDate() == null) {
            return Result.error("借用日期不能为空");
        }
        if (record.getDueDate() == null) {
            return Result.error("归还日期不能为空");
        }
        if (record.getDueDate().isBefore(record.getBorrowDate())) {
            return Result.error("归还日期不能早于借用日期");
        }

        Equipment equipment = equipmentService.getById(record.getAssetId());
        if (equipment == null) {
            return Result.error("资产不存在");
        }
        if ("borrowed".equals(equipment.getStatus())) {
            return Result.error("该资产已被借出");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.getUserByUsername(username);
        if (currentUser != null) {
            record.setApplicantId(currentUser.getId());
        }
        record.setAssetName(equipment.getName());
        record.setAssetType("equipment");
        record.setStatus("pending");
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        return Result.success(borrowRecordService.save(record));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> approve(@PathVariable Long id) {
        BorrowRecord record = borrowRecordService.getById(id);
        if (record == null) {
            return Result.error("记录不存在");
        }
        if (!"pending".equals(record.getStatus())) {
            return Result.error("该记录已处理");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.getUserByUsername(username);
        if (currentUser != null) {
            record.setApproverId(currentUser.getId());
        }
        record.setStatus("approved");
        record.setApproveTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        Equipment equipment = equipmentService.getById(record.getAssetId());
        if (equipment != null) {
            equipment.setStatus("borrowed");
            equipmentService.updateById(equipment);
        }

        return Result.success(borrowRecordService.updateById(record));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> reject(@PathVariable Long id, @RequestBody BorrowRecord req) {
        BorrowRecord record = borrowRecordService.getById(id);
        if (record == null) {
            return Result.error("记录不存在");
        }
        if (!"pending".equals(record.getStatus())) {
            return Result.error("该记录已处理");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.getUserByUsername(username);
        if (currentUser != null) {
            record.setApproverId(currentUser.getId());
        }
        record.setStatus("rejected");
        record.setApproveTime(LocalDateTime.now());
        record.setRejectReason(req.getRejectReason());
        record.setUpdateTime(LocalDateTime.now());

        return Result.success(borrowRecordService.updateById(record));
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> returnAsset(@PathVariable Long id) {
        BorrowRecord record = borrowRecordService.getById(id);
        if (record == null) {
            return Result.error("记录不存在");
        }
        if (!"approved".equals(record.getStatus())) {
            return Result.error("该记录不能归还");
        }

        record.setStatus("returned");
        record.setReturnTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());

        Equipment equipment = equipmentService.getById(record.getAssetId());
        if (equipment != null) {
            equipment.setStatus("online");
            equipmentService.updateById(equipment);
        }

        return Result.success(borrowRecordService.updateById(record));
    }
}
