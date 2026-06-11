package com.recruitos.onboard.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@org.apache.ibatis.annotations.Mapper
public interface ReferralOnboardSyncMapper {

    @Select("SELECT id, referrer_id, referrer_name FROM referral " +
            "WHERE tenant_id = #{tenantId} AND candidate_id = #{candidateId} LIMIT 1")
    java.util.Map<String, Object> findReferral(@Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId);

    @Update("UPDATE referral SET status = 'HIRED', reward_amount = #{amount}, reward_status = 'PENDING', updated_at = NOW() " +
            "WHERE tenant_id = #{tenantId} AND candidate_id = #{candidateId}")
    int markHired(@Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId, @Param("amount") double amount);

    @Insert("INSERT INTO referral_reward (tenant_id, referral_id, referrer_id, referrer_name, reward_type, reward_amount, status, created_at, updated_at) " +
            "SELECT #{tenantId}, r.id, r.referrer_id, r.referrer_name, 'CASH', #{amount}, 'PENDING', NOW(), NOW() " +
            "FROM referral r WHERE r.tenant_id = #{tenantId} AND r.candidate_id = #{candidateId} " +
            "AND NOT EXISTS (SELECT 1 FROM referral_reward rr WHERE rr.referral_id = r.id)")
    int createPendingReward(@Param("tenantId") Long tenantId, @Param("candidateId") Long candidateId, @Param("amount") double amount);
}
