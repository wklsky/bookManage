package com.example.bookmanage.mapper;

import com.example.bookmanage.entity.RefreshToken;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 刷新令牌数据访问。
 *
 * <p>TODO：过期令牌目前不会被物理清理，会持续占用表体积。
 * 后续应补充定时清理任务（@Scheduled + deleteExpiredBefore 类操作）再启用物理删除，
 * 现阶段保留已撤销记录更利于审计登录历史。
 */
@Mapper
public interface RefreshTokenMapper {

    @Insert("INSERT INTO sys_refresh_token (user_id, token_id, revoked, expires_at)"
            + " VALUES (#{userId}, #{tokenId}, 0, #{expiresAt})")
    void insert(RefreshToken token);

    @Select("SELECT id, user_id, token_id, revoked, expires_at, created_at"
            + " FROM sys_refresh_token WHERE token_id = #{tokenId}")
    RefreshToken selectByTokenId(@Param("tokenId") String tokenId);

    @Update("UPDATE sys_refresh_token SET revoked = 1 WHERE token_id = #{tokenId}")
    int revokeByTokenId(@Param("tokenId") String tokenId);

    /** 改密码、停用账号等场景下让该用户所有刷新令牌一次性失效 */
    @Update("UPDATE sys_refresh_token SET revoked = 1 WHERE user_id = #{userId}")
    int revokeByUserId(@Param("userId") Long userId);
}
