package com.recruitos.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruitos.tenant.entity.Organization;
import org.apache.ibatis.annotations.Mapper;

/**
 * Organization mapper
 */
@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
}
