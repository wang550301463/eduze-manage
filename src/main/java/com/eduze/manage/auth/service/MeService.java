package com.eduze.manage.auth.service;

import com.eduze.manage.auth.domain.User;
import com.eduze.manage.auth.dto.ChangePasswordRequest;
import com.eduze.manage.auth.dto.LoginResponse;
import com.eduze.manage.auth.dto.UpdateProfileRequest;
import com.eduze.manage.auth.mapper.UserMapper;
import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.auth.service.TokenVersionService;
import com.eduze.manage.branch.domain.Branch;
import com.eduze.manage.branch.mapper.BranchMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserMapper userMapper;
    private final BranchMapper branchMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenVersionService tokenVersionService;

    public LoginResponse.UserInfo current() {
        CustomUserDetails details = currentUser();
        User user = userMapper.selectById(details.getUserId());
        List<LoginResponse.BranchInfo> branches = new ArrayList<>();
        if (!details.getBranchIds().isEmpty()) {
            for (Branch branch : branchMapper.selectBatchIds(details.getBranchIds())) {
                branches.add(LoginResponse.BranchInfo.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .code(branch.getCode())
                        .build());
            }
        }
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .roles(List.copyOf(details.getRoleCodes()))
                .permissions(details.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .toList())
                .branches(branches)
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        CustomUserDetails details = currentUser();
        User user = userMapper.selectById(details.getUserId());
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "原密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        tokenVersionService.bump(user.getId());
    }

    @Transactional
    public void updateProfile(UpdateProfileRequest request) {
        CustomUserDetails details = currentUser();
        User user = userMapper.selectById(details.getUserId());
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        userMapper.updateById(user);
    }

    private CustomUserDetails currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails details) {
            return details;
        }
        throw new BizException(ErrorCode.UNAUTHORIZED);
    }
}
