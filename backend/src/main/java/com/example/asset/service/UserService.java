package com.example.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.asset.entity.Role;
import com.example.asset.entity.User;
import com.example.asset.entity.UserRole;
import com.example.asset.mapper.RoleMapper;
import com.example.asset.mapper.UserMapper;
import com.example.asset.mapper.UserRoleMapper;
import com.example.asset.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private com.example.asset.mapper.PermissionMapper permissionMapper;

    public User login(String username, String password) {
        // 1. Authenticate using Spring Security
        // This will call UserDetailsServiceImpl.loadUserByUsername and check password hash
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Generate Token
        String jwt = jwtUtils.generateToken(username);

        // 3. Get User info
        User user = lambdaQuery().eq(User::getUsername, username).one();
        
        // 4. Populate Roles and Token
        List<Role> roles = roleMapper.selectByUserId(user.getId());
        user.setRoles(roles);
        user.setToken(jwt);
        user.setPassword(null); // Security best practice

        return user;
    }

    public User getUserByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    public User getUserWithRolesAndPermissions(Long userId) {
        User user = getById(userId);
        if (user != null) {
            user.setRoles(roleMapper.selectByUserId(userId));
            user.setPermissions(permissionMapper.selectByUserId(userId));
            user.setPassword(null);
        }
        return user;
    }

    public List<User> getAllUsersWithRoles() {
        List<User> users = list();
        for (User user : users) {
            user.setRoles(roleMapper.selectByUserId(user.getId()));
            user.setPassword(null);
        }
        return users;
    }

    @Transactional
    public boolean saveUserWithRole(User user) {
        // 1. Save User
        if (!save(user)) {
            return false;
        }

        // 2. Assign Role
        // Default to 'user' role if not specified
        String roleCode = (user.getRole() != null && user.getRole().equalsIgnoreCase("admin")) ? "ROLE_ADMIN" : "ROLE_USER";
        
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
        if (role != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
        return true;
    }

    @Transactional
    public boolean updateUserWithRole(User user) {
        // 1. Update User
        if (!updateById(user)) {
            return false;
        }

        // 2. Update Role (if provided)
        if (user.getRole() != null) {
            // Delete existing roles
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
            
            // Add new role
            String roleCode = user.getRole().equalsIgnoreCase("admin") ? "ROLE_ADMIN" : "ROLE_USER";
            Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, roleCode));
            if (role != null) {
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(role.getId());
                userRoleMapper.insert(userRole);
            }
        }
        return true;
    }
}
