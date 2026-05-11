package com.example.asset.controller;

import com.example.asset.common.Result;
import com.example.asset.entity.BorrowApplication;
import com.example.asset.entity.User;
import com.example.asset.service.BorrowApplicationService;
import com.example.asset.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/borrow-applications", produces = "application/json;charset=UTF-8")
@CrossOrigin(origins = "*")
public class BorrowApplicationController {

    @Autowired
    private BorrowApplicationService borrowApplicationService;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<List<BorrowApplication>> getAll() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long userId = user != null ? user.getId() : null;
        return Result.success(borrowApplicationService.listWithOverdue(userId, isAdmin));
    }

    @PostMapping
    public Result<BorrowApplication> create(@RequestBody BorrowApplication app) {
        if (app.getAssetType() == null || app.getAssetId() == null) {
            return Result.error("请选择要借用的资产");
        }
        if (app.getReason() == null || app.getReason().trim().isEmpty()) {
            return Result.error("请填写借用原因");
        }
        if (app.getExpectedReturnDate() == null) {
            return Result.error("请选择预计归还日期");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        try {
            return Result.success(borrowApplicationService.create(app, user.getId()));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        String reason = body != null ? body.get("reason") : null;
        return Result.success(borrowApplicationService.approve(id, user.getId(), reason));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        String reason = body != null ? body.get("reason") : null;
        return Result.success(borrowApplicationService.reject(id, user.getId(), reason));
    }

    @PutMapping("/{id}/return")
    public Result<Boolean> returnAsset(@PathVariable Long id) {
        return Result.success(borrowApplicationService.returnAsset(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(borrowApplicationService.removeById(id));
    }
}
