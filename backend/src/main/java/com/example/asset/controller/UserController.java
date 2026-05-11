package com.example.asset.controller;

import com.example.asset.common.Result;
import com.example.asset.entity.Permission;
import com.example.asset.entity.Role;
import com.example.asset.entity.User;
import com.example.asset.mapper.PermissionMapper;
import com.example.asset.mapper.RoleMapper;
import com.example.asset.util.JwtUtils;
import com.example.asset.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/users", produces = "application/json;charset=UTF-8")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @PostMapping("/login")
    public Result<User> login(@RequestBody User user) {
        try {
            User loggedInUser = userService.login(user.getUsername(), user.getPassword());
            return Result.success(loggedInUser);
        } catch (Exception e) {
            return Result.error("登录失败: 用户名或密码错误");
        }
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.getUserWithRolesAndPermissions(userService.getUserByUsername(username).getId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("roles", user.getRoles() != null ? user.getRoles().stream().map(Role::getCode).collect(Collectors.toList()) : List.of());
        data.put("permissions", user.getPermissions() != null ? user.getPermissions().stream().map(Permission::getCode).collect(Collectors.toList()) : List.of());
        
        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody User user) {
        if (userService.getUserByUsername(user.getUsername()) != null) {
            return Result.error("Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        // Default register as regular user if not specified
        if (user.getRole() == null) {
            user.setRole("user");
        }
        return Result.success(userService.saveUserWithRole(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<User>> getAll() {
        return Result.success(userService.getAllUsersWithRoles());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> add(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (userService.getUserByUsername(user.getUsername()) != null) {
            return Result.error("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1); // Ensure user is enabled
        return Result.success(userService.saveUserWithRole(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody User user) {
        if (user.getUsername() != null && user.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // Do not update password if empty
        }
        return Result.success(userService.updateUserWithRole(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(userService.removeById(id));
    }
}
