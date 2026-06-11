package com.recruitos.offer.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@org.apache.ibatis.annotations.Mapper
public interface ReferralSyncMapper {

    @Update("UPDATE referral SET status = 'INTERVIEWING', reward_status = 'PENDING', updated_at = NOW() " +
            "WHERE tenant_id = #{tenantId} AND candidate_id = #{candidateId}")
    int markOfferAccepted(@Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId);
}
