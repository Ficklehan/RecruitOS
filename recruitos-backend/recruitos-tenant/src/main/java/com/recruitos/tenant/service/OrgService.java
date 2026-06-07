package com.recruitos.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruitos.common.exception.BizException;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.tenant.entity.Organization;
import com.recruitos.tenant.mapper.OrganizationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Organization service with CRUD and tree structure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrganizationMapper organizationMapper;

    /**
     * Get all organizations as flat list
     */
    public List<Organization> list() {
        LambdaQueryWrapper<Organization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Organization::getTenantId, TenantContext.getTenantId())
                .orderByAsc(Organization::getSortOrder);
        return organizationMapper.selectList(wrapper);
    }

    /**
     * Get organization tree
     */
    public List<Organization> tree() {
        List<Organization> allOrgs = list();
        return buildTree(allOrgs, 0L);
    }

    /**
     * Build tree structure from flat list
     */
    private List<Organization> buildTree(List<Organization> allOrgs, Long parentId) {
        List<Organization> tree = new ArrayList<>();
        for (Organization org : allOrgs) {
            if (parentId.equals(org.getParentId())) {
                List<Organization> children = buildTree(allOrgs, org.getId());
                tree.add(org);
            }
        }
        return tree;
    }

    /**
     * Create organization
     */
    public Organization create(Organization org) {
        // Validate parent exists if parentId is set
        if (org.getParentId() != null && org.getParentId() > 0) {
            Organization parent = organizationMapper.selectById(org.getParentId());
            if (parent == null) {
                throw new BizException(400, "Parent organization not found");
            }
        } else {
            org.setParentId(0L);
        }
        if (org.getStatus() == null) {
            org.setStatus(1);
        }
        organizationMapper.insert(org);
        return org;
    }

    /**
     * Update organization
     */
    public Organization update(Long id, Organization org) {
        Organization existing = organizationMapper.selectById(id);
        if (existing == null) {
            throw new BizException(404, "Organization not found");
        }
        existing.setName(org.getName());
        existing.setType(org.getType());
        existing.setLeaderId(org.getLeaderId());
        existing.setSortOrder(org.getSortOrder());
        existing.setStatus(org.getStatus());
        organizationMapper.updateById(existing);
        return existing;
    }

    /**
     * Delete organization
     */
    public void delete(Long id) {
        Organization org = organizationMapper.selectById(id);
        if (org == null) {
            throw new BizException(404, "Organization not found");
        }
        // Check if has children
        LambdaQueryWrapper<Organization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Organization::getParentId, id);
        if (organizationMapper.selectCount(wrapper) > 0) {
            throw new BizException(400, "Cannot delete organization with children");
        }
        organizationMapper.deleteById(id);
    }
}
