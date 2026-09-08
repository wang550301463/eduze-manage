package com.eduze.manage.tenant;

import com.eduze.manage.auth.security.CustomUserDetails;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class BranchAccessGuard {

    public CustomUserDetails requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        throw new BizException(ErrorCode.UNAUTHORIZED);
    }

    public void requireBranchAccess(Long branchId) {
        if (branchId == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "校区不能为空");
        }
        CustomUserDetails user = requireUser();
        if (user.isSuperAdmin()) {
            return;
        }
        List<Long> branchIds = user.getBranchIds();
        if (branchIds == null || branchIds.isEmpty() || !branchIds.contains(branchId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该校区");
        }
    }

    public void requireAnyBranchAccess(Collection<Long> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "校区不能为空");
        }
        for (Long branchId : branchIds) {
            requireBranchAccess(branchId);
        }
    }

    public List<Long> visibleBranchIdsOrNullForSuperAdmin() {
        CustomUserDetails user = requireUser();
        if (user.isSuperAdmin()) {
            return null;
        }
        List<Long> branchIds = user.getBranchIds();
        if (branchIds == null || branchIds.isEmpty()) {
            return List.of(-1L);
        }
        return branchIds;
    }
}
