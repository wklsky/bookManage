package com.example.bookmanage.mapper;

import com.example.bookmanage.entity.AuditLog;
import com.example.bookmanage.enums.AuditAction;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作审计日志数据访问。
 */
@Mapper
public interface AuditLogMapper {

    // 接口字段隐含 public static final，不能加 private 修饰
    String COLUMNS =
            "id, operator_id, operator_name, action, target_type, target_id, summary, ip, created_at";

    /** 只追加、不更新不删除：审计记录一旦落库即为事实，改动它会让审计本身失去意义 */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO sys_audit_log (operator_id, operator_name, action, target_type, target_id, summary, ip)"
            + " VALUES (#{operatorId}, #{operatorName}, #{action}, #{targetType}, #{targetId}, #{summary}, #{ip})")
    void insert(AuditLog log);

    @Select("""
            <script>
            SELECT """ + COLUMNS + """
            FROM sys_audit_log
            <where>
              <if test="keyword != null and keyword != ''">
                AND (operator_name LIKE CONCAT('%', #{keyword}, '%')
                  OR summary LIKE CONCAT('%', #{keyword}, '%')
                  OR target_id LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="action != null">AND action = #{action}</if>
              <if test="operatorId != null">AND operator_id = #{operatorId}</if>
            </where>
            ORDER BY created_at DESC, id DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<AuditLog> selectPage(@Param("keyword") String keyword,
                              @Param("action") AuditAction action,
                              @Param("operatorId") Long operatorId,
                              @Param("offset") long offset,
                              @Param("size") long size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_audit_log
            <where>
              <if test="keyword != null and keyword != ''">
                AND (operator_name LIKE CONCAT('%', #{keyword}, '%')
                  OR summary LIKE CONCAT('%', #{keyword}, '%')
                  OR target_id LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="action != null">AND action = #{action}</if>
              <if test="operatorId != null">AND operator_id = #{operatorId}</if>
            </where>
            </script>
            """)
    long countPage(@Param("keyword") String keyword,
                   @Param("action") AuditAction action,
                   @Param("operatorId") Long operatorId);
}
