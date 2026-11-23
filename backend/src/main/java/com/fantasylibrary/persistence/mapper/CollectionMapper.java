package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.account.AccountModels.SavedWork;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollectionMapper {
    List<SavedWork> list(long userId);

    SavedWork find(@Param("userId") long userId, @Param("workId") long workId);

    void save(@Param("userId") long userId, @Param("item") SavedWork item);

    @Delete("DELETE FROM fl_saved_work WHERE user_id = #{userId} AND work_id = #{workId}")
    void remove(@Param("userId") long userId, @Param("workId") long workId);
}
