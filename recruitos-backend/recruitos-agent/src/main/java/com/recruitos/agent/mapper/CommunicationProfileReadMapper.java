package com.recruitos.agent.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface CommunicationProfileReadMapper {

    @Select("SELECT persona, company_background AS companyBackground, communication_logic AS communicationLogic, " +
            "proactive_triggers_json AS proactiveTriggersJson, guardrails " +
            "FROM communication_profile WHERE tenant_id = #{tenantId} AND job_id IS NULL LIMIT 1")
    Map<String, Object> selectTenantDefault(@Param("tenantId") Long tenantId);

    @Select("SELECT persona, company_background AS companyBackground, communication_logic AS communicationLogic, " +
            "proactive_triggers_json AS proactiveTriggersJson, guardrails " +
            "FROM communication_profile WHERE tenant_id = #{tenantId} AND job_id = #{jobId} LIMIT 1")
    Map<String, Object> selectJobOverride(@Param("tenantId") Long tenantId, @Param("jobId") Long jobId);
}
