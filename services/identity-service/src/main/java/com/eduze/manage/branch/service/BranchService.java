package com.eduze.manage.branch.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eduze.manage.branch.domain.Branch;
import com.eduze.manage.branch.dto.BranchRequest;
import com.eduze.manage.branch.dto.BranchResponse;
import com.eduze.manage.branch.mapper.BranchMapper;
import com.eduze.manage.common.exception.BizException;
import com.eduze.manage.common.exception.ErrorCode;
import com.eduze.manage.tenant.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchMapper branchMapper;
    private final JdbcTemplate jdbcTemplate;

    public List<BranchResponse> list() {
        return branchMapper
                .selectList(
                        Wrappers.<Branch>lambdaQuery()
                                .eq(Branch::getTenantId, TenantContext.getTenantId())
                                .orderByAsc(Branch::getCode))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BranchResponse get(Long id) {
        return toResponse(requireBranch(id));
    }

    @Transactional
    public BranchResponse create(BranchRequest request) {
        Branch branch = new Branch();
        branch.setTenantId(TenantContext.getTenantId());
        branch.setName(request.getName());
        branch.setCode(request.getCode());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        try {
            branchMapper.insert(branch);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.TENANT_UNIQUE_VIOLATION, "校区编码已存在", ex);
        }
        return toResponse(branch);
    }

    @Transactional
    public BranchResponse update(Long id, BranchRequest request) {
        Branch branch = requireBranch(id);
        branch.setName(request.getName());
        branch.setCode(request.getCode());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        if (request.getStatus() != null) {
            branch.setStatus(request.getStatus());
        }
        try {
            branchMapper.updateById(branch);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ErrorCode.TENANT_UNIQUE_VIOLATION, "校区编码已存在", ex);
        }
        return toResponse(branch);
    }

    @Transactional
    public void delete(Long id) {
        requireBranch(id);
        if (hasStudents(id)) {
            throw new BizException(ErrorCode.CONFLICT, "校区下仍有学员，无法删除");
        }
        branchMapper.deleteById(id);
    }

    private boolean hasStudents(Long branchId) {
        if (!tableExists("t_student")) {
            return false;
        }
        Long count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM t_student WHERE branch_id = ? AND deleted_at = 0",
                        Long.class,
                        branchId);
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """,
                        Integer.class,
                        tableName);
        return count != null && count > 0;
    }

    private Branch requireBranch(Long id) {
        Branch branch = branchMapper.selectById(id);
        if (branch == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "校区不存在");
        }
        return branch;
    }

    private BranchResponse toResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .code(branch.getCode())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .status(branch.getStatus())
                .build();
    }
}
