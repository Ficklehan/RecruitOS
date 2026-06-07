package com.recruitos.interview.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JobPositionMapper {

    @Select("SELECT title FROM job_position WHERE id = #{id}")
    String selectTitleById(@Param("id") Long id);
}
