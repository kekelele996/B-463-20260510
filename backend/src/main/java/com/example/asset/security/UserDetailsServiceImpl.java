package com.example.asset.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.asset.entity.Permission;
import com.example.asset.entity.Role;
import com.example.asset.entity.User;
import com.example.asset.mapper.PermissionMapper;
import com.example.asset.mapper.RoleMapper;
import com.example.asset.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        List<Role> roles = roleMapper.selectByUserId(user.getId());
        List<Permission> permissions = permissionMapper.selectByUserId(user.getId());
        
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            authorities.addAll(roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode()))
                    .collect(Collectors.toList()));
             // Also add the raw code if needed, but ROLE_ prefix is standard for hasRole
             authorities.addAll(roles.stream()
                    .map(r -> new SimpleGrantedAuthority(r.getCode()))
                    .collect(Collectors.toList()));
        }
        
        if (permissions != null) {
            authorities.addAll(permissions.stream()
                    .map(p -> new SimpleGrantedAuthority(p.getCode()))
                    .collect(Collectors.toList()));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus() != null && user.getStatus() == 1, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}
