package com.example.bookmanage.mapper;

import com.example.bookmanage.entity.SysUser;
import com.example.bookmanage.enums.UserRole;
import com.example.bookmanage.enums.UserStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户数据访问。
 */
@Mapper
public interface SysUserMapper {

    @Select("SELECT id, username, password, email, nickname, phone, avatar_url, role, status, created_at, updated_at"
            + " FROM sys_user WHERE id = #{id}")
    SysUser selectById(@Param("id") Long id);

    @Select("SELECT id, username, password, email, nickname, phone, avatar_url, role, status, created_at, updated_at"
            + " FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(@Param("username") String username);

    /** 登录账号允许是用户名或邮箱，两者都是唯一键，合并在一条 SQL 里避免两次查询 */
    @Select("SELECT id, username, password, email, nickname, phone, avatar_url, role, status, created_at, updated_at"
            + " FROM sys_user WHERE username = #{account} OR email = #{account} LIMIT 1")
    SysUser selectByUsernameOrEmail(@Param("account") String account);

    /** 批量加载用户精简信息，供借阅单视图组装 */
    @Select("""
            <script>
            SELECT id, username, nickname FROM sys_user WHERE id IN
            <foreach item="id" collection="ids" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<SysUser> selectBriefByIds(@Param("ids") java.util.Collection<Long> ids);

    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
    long countByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM sys_user WHERE email = #{email}")
    long countByEmail(@Param("email") String email);

    /**
     * 除指定用户外的启用管理员数量，用于保护"最后一个管理员"。
     *
     * <p>必须排除被调整的目标账号：若把当前账号自身算进去，
     * 停用后再改回管理员时会被自己的记录挡住，永远无法通过校验。
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE role = 'ADMIN' AND status = 'ACTIVE' AND id <> #{id}")
    long countActiveAdminExcluding(@Param("id") Long id);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO sys_user (username, password, email, nickname, phone, avatar_url, role, status)"
            + " VALUES (#{username}, #{password}, #{email}, #{nickname}, #{phone}, #{avatarUrl}, #{role}, #{status})")
    void insert(SysUser user);

    @Update("UPDATE sys_user SET nickname = #{nickname}, email = #{email}, phone = #{phone},"
            + " avatar_url = #{avatarUrl} WHERE id = #{id}")
    int updateProfile(SysUser user);

    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE sys_user SET role = #{role}, status = #{status} WHERE id = #{id}")
    int updateAccess(@Param("id") Long id, @Param("role") UserRole role, @Param("status") UserStatus status);

    @Select("""
            <script>
            SELECT id, username, password, email, nickname, phone, avatar_url, role, status, created_at, updated_at
            FROM sys_user
            <where>
              <if test="keyword != null and keyword != ''">
                AND (username LIKE CONCAT('%', #{keyword}, '%')
                  OR nickname LIKE CONCAT('%', #{keyword}, '%')
                  OR email LIKE CONCAT('%', #{keyword}, '%')
                  OR phone LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="role != null">AND role = #{role}</if>
              <if test="status != null">AND status = #{status}</if>
            </where>
            ORDER BY ${orderBy}
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<SysUser> selectPage(@Param("keyword") String keyword,
                             @Param("role") UserRole role,
                             @Param("status") UserStatus status,
                             @Param("orderBy") String orderBy,
                             @Param("offset") long offset,
                             @Param("size") long size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_user
            <where>
              <if test="keyword != null and keyword != ''">
                AND (username LIKE CONCAT('%', #{keyword}, '%')
                  OR nickname LIKE CONCAT('%', #{keyword}, '%')
                  OR email LIKE CONCAT('%', #{keyword}, '%')
                  OR phone LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="role != null">AND role = #{role}</if>
              <if test="status != null">AND status = #{status}</if>
            </where>
            </script>
            """)
    long countPage(@Param("keyword") String keyword,
                   @Param("role") UserRole role,
                   @Param("status") UserStatus status);
}
