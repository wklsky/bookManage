package com.example.bookmanage.mapper;

import com.example.bookmanage.entity.BorrowOrder;
import com.example.bookmanage.enums.OrderStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 借阅单数据访问。
 *
 * <p>逾期（OVERDUE）不落库，查询时由 BORROWED + due_at 过期推导，
 * 所以筛选 OVERDUE 需要单独的 SQL 分支。
 */
@Mapper
public interface BorrowOrderMapper {

    String COLUMNS = "o.id, o.order_no, o.user_id, o.book_id, o.status, o.remark,"
            + " o.audit_remark, o.return_remark, o.return_condition, o.reserved_at, o.approved_at,"
            + " o.borrowed_at, o.due_at, o.returned_at, o.created_at, o.updated_at";

    @Select("SELECT " + COLUMNS + " FROM b_borrow_order o WHERE o.id = #{id}")
    BorrowOrder selectById(@Param("id") Long id);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO b_borrow_order (order_no, user_id, book_id, status, remark, reserved_at)"
            + " VALUES (#{orderNo}, #{userId}, #{bookId}, #{status}, #{remark}, #{reservedAt})")
    void insert(BorrowOrder order);

    @Update("UPDATE b_borrow_order SET status = #{status}, remark = #{remark}, audit_remark = #{auditRemark},"
            + " return_remark = #{returnRemark}, return_condition = #{returnCondition}, approved_at = #{approvedAt},"
            + " borrowed_at = #{borrowedAt}, due_at = #{dueAt}, returned_at = #{returnedAt} WHERE id = #{id}")
    int update(BorrowOrder order);

    /** 同一用户对同一本书只允许存在一个未终结借阅单，防止重复预约 */
    @Select("SELECT COUNT(*) FROM b_borrow_order WHERE user_id = #{userId} AND book_id = #{bookId}"
            + " AND status IN ('PENDING', 'APPROVED', 'BORROWED', 'RETURN_REQUESTED')")
    long countUnfinished(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Select("""
            <script>
            SELECT o.id, o.order_no, o.user_id, o.book_id, o.status, o.remark,
                   o.audit_remark, o.return_remark, o.return_condition, o.reserved_at, o.approved_at, o.borrowed_at,
                   o.due_at, o.returned_at, o.created_at, o.updated_at
            FROM b_borrow_order o
            JOIN sys_user u ON u.id = o.user_id
            JOIN b_book b ON b.id = o.book_id
            <where>
              <if test="userId != null">AND o.user_id = #{userId}</if>
              <if test="overdueOnly">AND o.status = 'BORROWED' AND o.due_at &lt; NOW()</if>
              <if test="!overdueOnly">
                <if test="status != null">AND o.status = #{status}</if>
              </if>
              <if test="keyword != null and keyword != ''">
                AND (o.order_no LIKE CONCAT('%', #{keyword}, '%')
                  OR u.username LIKE CONCAT('%', #{keyword}, '%')
                  OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                  OR b.title LIKE CONCAT('%', #{keyword}, '%')
                  OR b.isbn LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="from != null">AND o.created_at &gt;= #{from}</if>
              <if test="to != null">AND o.created_at &lt;= #{to}</if>
            </where>
            ORDER BY o.created_at DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<BorrowOrder> selectPage(@Param("userId") Long userId,
                                 @Param("status") OrderStatus status,
                                 @Param("overdueOnly") boolean overdueOnly,
                                 @Param("keyword") String keyword,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("offset") long offset,
                                 @Param("size") long size);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM b_borrow_order o
            JOIN sys_user u ON u.id = o.user_id
            JOIN b_book b ON b.id = o.book_id
            <where>
              <if test="userId != null">AND o.user_id = #{userId}</if>
              <if test="overdueOnly">AND o.status = 'BORROWED' AND o.due_at &lt; NOW()</if>
              <if test="!overdueOnly">
                <if test="status != null">AND o.status = #{status}</if>
              </if>
              <if test="keyword != null and keyword != ''">
                AND (o.order_no LIKE CONCAT('%', #{keyword}, '%')
                  OR u.username LIKE CONCAT('%', #{keyword}, '%')
                  OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                  OR b.title LIKE CONCAT('%', #{keyword}, '%')
                  OR b.isbn LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="from != null">AND o.created_at &gt;= #{from}</if>
              <if test="to != null">AND o.created_at &lt;= #{to}</if>
            </where>
            </script>
            """)
    long countPage(@Param("userId") Long userId,
                   @Param("status") OrderStatus status,
                   @Param("overdueOnly") boolean overdueOnly,
                   @Param("keyword") String keyword,
                   @Param("from") LocalDateTime from,
                   @Param("to") LocalDateTime to);

    @Select("SELECT COUNT(*) FROM b_borrow_order WHERE status = #{status}")
    long countByStatus(@Param("status") OrderStatus status);

    /** 逾期数量：借阅中且已过应还时间 */
    @Select("SELECT COUNT(*) FROM b_borrow_order WHERE status = 'BORROWED' AND due_at < NOW()")
    long countOverdue();

    /** 活跃读者数：产生过借阅记录的去重用户数 */
    @Select("SELECT COUNT(DISTINCT user_id) FROM b_borrow_order")
    long countBorrowers();
}
