package com.recruitos.interview.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecruitDemandMapper {

    @Select("SELECT final_interviewer_ids FROM recruit_demand WHERE id = #{id}")
    String selectFinalInterviewerIdsById(@Param("id") Long id);
}
