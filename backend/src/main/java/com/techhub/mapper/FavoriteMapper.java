package com.techhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.techhub.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Select("SELECT f.* FROM favorite f " +
            "INNER JOIN post p ON f.post_id = p.id " +
            "WHERE f.user_id = #{userId} AND p.deleted = 0 " +
            "ORDER BY f.create_time DESC")
    Page<Favorite> selectVisibleFavorites(Page<Favorite> page, @Param("userId") Long userId);
}
