package com.recruitos.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruitos.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * System user mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
