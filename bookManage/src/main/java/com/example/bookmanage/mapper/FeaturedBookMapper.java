package com.example.bookmanage.mapper;

import com.example.bookmanage.entity.FeaturedBook;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 首页推荐位数据访问。
 */
@Mapper
public interface FeaturedBookMapper {

    /** 后台列表：联表带出图书快照，避免逐条再查图书造成 N+1 */
    @Select("""
            <script>
            SELECT f.id, f.book_id, f.position, f.enabled, f.remark, f.created_by, f.created_at, f.updated_at,
                   b.title, b.author, b.isbn, b.cover_url, b.status, b.total_stock, b.available_stock
            FROM b_featured_book f
            JOIN b_book b ON b.id = f.book_id AND b.is_deleted = 0
            <if test="enabledOnly">WHERE f.enabled = 1</if>
            ORDER BY f.position ASC, f.id ASC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<FeaturedBook> selectPage(@Param("enabledOnly") boolean enabledOnly,
                                  @Param("offset") long offset,
                                  @Param("size") long size);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM b_featured_book f
            JOIN b_book b ON b.id = f.book_id AND b.is_deleted = 0
            <if test="enabledOnly">WHERE f.enabled = 1</if>
            </script>
            """)
    long countPage(@Param("enabledOnly") boolean enabledOnly);

    /**
     * 前台可见推荐位。启用与图书上架两个条件缺一不可：
     * 只判 enabled 会让已下架图书出现在首页，读者点进去就是 404。
     */
    @Select("""
            SELECT f.id, f.book_id, f.position, f.enabled, f.remark, f.created_by, f.created_at, f.updated_at,
                   b.title, b.author, b.isbn, b.cover_url, b.status, b.total_stock, b.available_stock
            FROM b_featured_book f
            JOIN b_book b ON b.id = f.book_id AND b.is_deleted = 0 AND b.status = 'ACTIVE'
            WHERE f.enabled = 1
            ORDER BY f.position ASC, f.id ASC
            LIMIT #{limit}
            """)
    List<FeaturedBook> selectVisible(@Param("limit") long limit);

    @Select("SELECT id, book_id, position, enabled, remark, created_by, created_at, updated_at"
            + " FROM b_featured_book WHERE id = #{id}")
    FeaturedBook selectById(@Param("id") Long id);

    /** 同一本书只允许占一个推荐位，唯一键 uk_book_id 已在库表层面兜底，这里用于提前给出友好提示 */
    @Select("SELECT COUNT(*) FROM b_featured_book WHERE book_id = #{bookId}")
    long countByBookId(@Param("bookId") Long bookId);

    /** 新增推荐位默认排到最后一位之后 */
    @Select("SELECT COALESCE(MAX(position), 0) FROM b_featured_book")
    int maxPosition();

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO b_featured_book (book_id, position, enabled, remark, created_by)"
            + " VALUES (#{bookId}, #{position}, #{enabled}, #{remark}, #{createdBy})")
    void insert(FeaturedBook featured);

    @Update("UPDATE b_featured_book SET position = #{position}, enabled = #{enabled}, remark = #{remark}"
            + " WHERE id = #{id}")
    int update(FeaturedBook featured);

    @Delete("DELETE FROM b_featured_book WHERE id = #{id}")
    int delete(@Param("id") Long id);

    /** 图书软删除后同步清理推荐位，否则前台会残留指向已下架图书的展示位 */
    @Delete("DELETE FROM b_featured_book WHERE book_id = #{bookId}")
    int deleteByBookId(@Param("bookId") Long bookId);
}
