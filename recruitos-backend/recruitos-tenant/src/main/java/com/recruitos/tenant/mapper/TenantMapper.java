package com.recruitos.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruitos.tenant.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * Tenant mapper
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
