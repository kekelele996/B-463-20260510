package com.example.asset.controller;

import com.example.asset.common.Result;
import com.example.asset.entity.Permission;
import com.example.asset.mapper.PermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/permissions", produces = "application/json;charset=UTF-8")
@CrossOrigin(origins = "*")
public class PermissionController {

    @Autowired
    private PermissionMapper permissionMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<Permission>> getAll() {
        return Result.success(permissionMapper.selectList(null));
    }
}
