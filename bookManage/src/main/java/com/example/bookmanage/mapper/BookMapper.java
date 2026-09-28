package com.example.bookmanage.mapper;

import com.example.bookmanage.entity.Book;
import com.example.bookmanage.enums.BookStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 图书数据访问。
 *
 * <p>库存变更一律走条件更新（WHERE 中带库存约束），由影响行数判断是否成功，
 * 避免「先查后写」在并发下超卖或把已借出册数击穿。
 */
@Mapper
public interface BookMapper {

    @Select("SELECT id, title, author, isbn, publisher, publish_date, description, cover_url, category_id,"
            + " total_stock, available_stock, status, is_deleted, created_at, updated_at"
            + " FROM b_book WHERE id = #{id}")
    Book selectById(@Param("id") Long id);

    /** 库存是并发热点，状态流转前必须持有行锁再判断是否可借 */
    @Select("SELECT id, title, author, isbn, publisher, publish_date, description, cover_url, category_id,"
            + " total_stock, available_stock, status, is_deleted, created_at, updated_at"
            + " FROM b_book WHERE id = #{id} FOR UPDATE")
    Book selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            <script>
            SELECT id, title, author, isbn, publisher, publish_date, description, cover_url, category_id,
                   total_stock, available_stock, status, is_deleted, created_at, updated_at
            FROM b_book
            <where>
              is_deleted = 0
              <if test="keyword != null and keyword != ''">
                AND (title LIKE CONCAT('%', #{keyword}, '%')
                  OR author LIKE CONCAT('%', #{keyword}, '%')
                  OR isbn LIKE CONCAT('%', #{keyword}, '%')
                  OR publisher LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="categoryId != null">AND category_id = #{categoryId}</if>
              <if test="status != null">AND status = #{status}</if>
              <if test="availableOnly">AND available_stock &gt; 0</if>
              <if test="unavailableOnly">AND available_stock = 0</if>
            </where>
            ORDER BY ${orderBy}
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<Book> selectPage(@Param("keyword") String keyword,
                          @Param("categoryId") Long categoryId,
                          @Param("status") BookStatus status,
                          @Param("availableOnly") boolean availableOnly,
                          @Param("unavailableOnly") boolean unavailableOnly,
                          @Param("orderBy") String orderBy,
                          @Param("offset") long offset,
                          @Param("size") long size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM b_book
            <where>
              is_deleted = 0
              <if test="keyword != null and keyword != ''">
                AND (title LIKE CONCAT('%', #{keyword}, '%')
                  OR author LIKE CONCAT('%', #{keyword}, '%')
                  OR isbn LIKE CONCAT('%', #{keyword}, '%')
                  OR publisher LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="categoryId != null">AND category_id = #{categoryId}</if>
              <if test="status != null">AND status = #{status}</if>
              <if test="availableOnly">AND available_stock &gt; 0</if>
              <if test="unavailableOnly">AND available_stock = 0</if>
            </where>
            </script>
            """)
    long countPage(@Param("keyword") String keyword,
                   @Param("categoryId") Long categoryId,
                   @Param("status") BookStatus status,
                   @Param("availableOnly") boolean availableOnly,
                   @Param("unavailableOnly") boolean unavailableOnly);

    /**
     * ISBN 唯一性校验，excludeId 用于更新时排除自身。
     *
     * <p>excludeId 为 null 时必须显式声明 jdbcType，否则 MyBatis 按 OTHER 传空值，
     * 部分驱动会拒绝这种类型不明的 NULL。
     */
    @Select("SELECT COUNT(*) FROM b_book WHERE isbn = #{isbn} AND is_deleted = 0"
            + " AND (#{excludeId,jdbcType=BIGINT} IS NULL OR id <> #{excludeId,jdbcType=BIGINT})")
    long countByIsbn(@Param("isbn") String isbn, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM b_borrow_order WHERE book_id = #{bookId}"
            + " AND status IN ('PENDING', 'APPROVED', 'BORROWED', 'RETURN_REQUESTED')")
    long countUnfinishedOrders(@Param("bookId") Long bookId);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO b_book (title, author, isbn, publisher, publish_date, description, cover_url,"
            + " category_id, total_stock, available_stock, status, is_deleted)"
            + " VALUES (#{title}, #{author}, #{isbn}, #{publisher}, #{publishDate}, #{description},"
            + " #{coverUrl}, #{categoryId}, #{totalStock}, #{availableStock}, #{status}, 0)")
    void insert(Book book);

    /** 书目信息更新：不含库存字段，库存变更必须走库存接口并留痕 */
    @Update("UPDATE b_book SET title = #{title}, author = #{author}, isbn = #{isbn}, publisher = #{publisher},"
            + " publish_date = #{publishDate}, description = #{description}, cover_url = #{coverUrl},"
            + " category_id = #{categoryId}, status = #{status} WHERE id = #{id} AND is_deleted = 0")
    int update(Book book);

    @Update("UPDATE b_book SET is_deleted = 1, status = 'INACTIVE' WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") Long id);

    /** 预约锁定库存：可用库存不足时影响行数为 0 */
    @Update("UPDATE b_book SET available_stock = available_stock - 1"
            + " WHERE id = #{id} AND is_deleted = 0 AND status = 'ACTIVE' AND available_stock > 0")
    int decreaseAvailable(@Param("id") Long id);

    /** 取消预约、拒绝预约、验收归还时回补库存 */
    @Update("UPDATE b_book SET available_stock = available_stock + 1 WHERE id = #{id} AND is_deleted = 0")
    int increaseAvailable(@Param("id") Long id);

    /**
     * 库存调整。已借出册数 = total_stock - available_stock，调整不应改变该差值，
     * 因此约束等价于「新的可用库存不能为负」。
     */
    @Update("UPDATE b_book SET total_stock = total_stock + #{change}, available_stock = available_stock + #{change}"
            + " WHERE id = #{id} AND is_deleted = 0 AND available_stock + #{change} >= 0")
    int adjustStock(@Param("id") Long id, @Param("change") int change);

    /** 批量加载图书精简信息，供借阅单视图组装 */
    @Select("""
            <script>
            SELECT id, title, isbn, cover_url FROM b_book WHERE id IN
            <foreach item="id" collection="ids" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Book> selectBriefByIds(@Param("ids") java.util.Collection<Long> ids);

    @Select("SELECT COUNT(*) FROM b_book WHERE is_deleted = 0")
    long countTitles();

    @Select("SELECT COALESCE(SUM(total_stock), 0) FROM b_book WHERE is_deleted = 0")
    long sumTotalStock();

    @Select("SELECT COALESCE(SUM(available_stock), 0) FROM b_book WHERE is_deleted = 0")
    long sumAvailableStock();
}
