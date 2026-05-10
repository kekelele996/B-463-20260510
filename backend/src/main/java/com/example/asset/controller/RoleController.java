package com.example.asset.controller;

import com.example.asset.common.Result;
import com.example.asset.entity.Role;
import com.example.asset.mapper.RoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/roles", produces = "application/json;charset=UTF-8")
@CrossOrigin(origins = "*")
public class RoleController {

    @Autowired
    private RoleMapper roleMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<Role>> getAll() {
        return Result.success(roleMapper.selectList(null));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    public Result<Integer> add(@RequestBody Role role) {
        return Result.success(roleMapper.insert(role));
    }
}
