package com.recruitos.evolution.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TenantEvolutionConfigMapper {

    @Select("SELECT config_json FROM tenant WHERE id = #{tenantId}")
    String selectConfigJson(@Param("tenantId") Long tenantId);

    @Update("UPDATE tenant SET config_json = #{configJson}, updated_at = NOW() WHERE id = #{tenantId}")
    int updateConfigJson(@Param("tenantId") Long tenantId, @Param("configJson") String configJson);
}
