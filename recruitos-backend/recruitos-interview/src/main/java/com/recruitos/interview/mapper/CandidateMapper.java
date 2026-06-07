package com.recruitos.interview.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CandidateMapper {

    @Select("SELECT name FROM candidate WHERE id = #{id}")
    String selectNameById(@Param("id") Long id);
}
