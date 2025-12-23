package com.example.demo.service;

import cn.hutool.crypto.digest.BCrypt;
import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.entity.Users;
import com.example.demo.mapper.PermissionMapper;
import com.example.demo.mapper.RoleMapper;
import com.example.demo.mapper.UsersMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public List<Users> testMapper() {
        return usersMapper.testMapper();
    }

    @Override
    public boolean checkUser(Users users) {
        if (StringUtils.isEmpty(users.getUsername()) || StringUtils.isEmpty(users.getPassword())) {
            return false;
        }
        Users find = usersMapper.findByUsername(users.getUsername());
        String password = BCrypt.hashpw(users.getPassword(), "$2a$10$Y2Cgsuie2spIjKHfwi8VL.");
        if (find == null || find.getStatus() == 0 || !password.equals(find.getPassword())) {
            return false;
        }
        return true;
    }

    @Override
    public Users getUser(String username) {
        return usersMapper.findByUsername(username);
    }

    @Override
    public Set<String> getUserPermissions(String username) {
        List<Permission> permissionList = permissionMapper.listByUsername(username);
        Set<String> permissionSet = new HashSet<>();
        for (Permission p : permissionList) {
            permissionSet.add(p.getPermissionName());
        }
        return permissionSet;
    }

    @Override
    public Set<String> getUserRoles(String username) {
        List<Role> roleList = roleMapper.listByUsername(username);
        Set<String> roleSet = new HashSet<>();
        for (Role r : roleList) {
            roleSet.add(r.getRoleName());
        }
        return roleSet;
    }
}
