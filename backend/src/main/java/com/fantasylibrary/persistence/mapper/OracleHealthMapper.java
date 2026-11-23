package com.fantasylibrary.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OracleHealthMapper {

    @Select("SELECT 1 FROM DUAL")
    @Options(timeout = 2)
    int ping();
}
