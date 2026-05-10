package com.example.asset.controller;

import com.example.asset.common.Result;
import com.example.asset.entity.BorrowRequest;
import com.example.asset.entity.Equipment;
import com.example.asset.entity.Vehicle;
import com.example.asset.entity.User;
import com.example.asset.service.BorrowRequestService;
import com.example.asset.service.EquipmentService;
import com.example.asset.service.UserService;
import com.example.asset.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/borrows", produces = "application/json;charset=UTF-8")
@CrossOrigin(origins = "*")
public class BorrowRequestController {

    @Autowired
    private BorrowRequestService borrowRequestService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<List<BorrowRequest>> list() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserWithRolesAndPermissions(
                userService.getUserByUsername(username).getId());
        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getCode()));
        List<BorrowRequest> list;
        if (isAdmin) {
            list = borrowRequestService.listWithDetail();
        } else {
            list = borrowRequestService.listByApplicantId(user.getId());
        }
        return Result.success(list);
    }

    @PostMapping
    public Result<Boolean> submit(@RequestBody BorrowRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        request.setApplicantId(user.getId());
        request.setStatus("pending");
        request.setCreateTime(LocalDateTime.now());

        if (request.getAssetType() == null || request.getAssetId() == null) {
            return Result.error("请选择借用资产");
        }
        if ("equipment".equals(request.getAssetType())) {
            Equipment eq = equipmentService.getById(request.getAssetId());
            if (eq == null) return Result.error("设备不存在");
            if ("borrowed".equals(eq.getStatus())) return Result.error("该设备已被借出");
        } else if ("vehicle".equals(request.getAssetType())) {
            Vehicle v = vehicleService.getById(request.getAssetId());
            if (v == null) return Result.error("车辆不存在");
            if ("borrowed".equals(v.getStatus())) return Result.error("该车辆已被借出");
        } else {
            return Result.error("不支持的资产类型");
        }
        if (request.getExpectedReturnDate() == null) {
            return Result.error("请填写预计归还日期");
        }
        if (request.getExpectedReturnDate().isBefore(LocalDateTime.now())) {
            return Result.error("预计归还日期不能早于当前时间");
        }
        return Result.success(borrowRequestService.save(request));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> approve(@PathVariable Long id, @RequestBody BorrowRequest dto) {
        BorrowRequest request = borrowRequestService.getById(id);
        if (request == null) return Result.error("申请不存在");
        if (!"pending".equals(request.getStatus())) return Result.error("该申请已处理");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userService.getUserByUsername(username);

        request.setStatus("approved");
        request.setApprovedBy(admin.getId());
        request.setApproveRemark(dto.getApproveRemark());
        request.setUpdateTime(LocalDateTime.now());

        if ("equipment".equals(request.getAssetType())) {
            Equipment eq = new Equipment();
            eq.setId(request.getAssetId());
            eq.setStatus("borrowed");
            equipmentService.updateById(eq);
        } else if ("vehicle".equals(request.getAssetType())) {
            Vehicle v = new Vehicle();
            v.setId(request.getAssetId());
            v.setStatus("borrowed");
            vehicleService.updateById(v);
        }
        return Result.success(borrowRequestService.updateById(request));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> reject(@PathVariable Long id, @RequestBody BorrowRequest dto) {
        BorrowRequest request = borrowRequestService.getById(id);
        if (request == null) return Result.error("申请不存在");
        if (!"pending".equals(request.getStatus())) return Result.error("该申请已处理");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userService.getUserByUsername(username);

        request.setStatus("rejected");
        request.setApprovedBy(admin.getId());
        request.setApproveRemark(dto.getApproveRemark());
        request.setUpdateTime(LocalDateTime.now());
        return Result.success(borrowRequestService.updateById(request));
    }

    @PutMapping("/{id}/return")
    public Result<Boolean> returnAsset(@PathVariable Long id) {
        BorrowRequest request = borrowRequestService.getById(id);
        if (request == null) return Result.error("申请不存在");
        if (!"approved".equals(request.getStatus())) return Result.error("只有已批准的申请可以归还");

        request.setStatus("returned");
        request.setActualReturnDate(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());

        if ("equipment".equals(request.getAssetType())) {
            Equipment eq = new Equipment();
            eq.setId(request.getAssetId());
            eq.setStatus("online");
            equipmentService.updateById(eq);
        } else if ("vehicle".equals(request.getAssetType())) {
            Vehicle v = new Vehicle();
            v.setId(request.getAssetId());
            v.setStatus("parked");
            vehicleService.updateById(v);
        }
        return Result.success(borrowRequestService.updateById(request));
    }
}
