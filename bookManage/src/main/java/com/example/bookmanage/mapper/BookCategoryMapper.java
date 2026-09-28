package com.example.bookmanage.mapper;

import com.example.bookmanage.entity.BookCategory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 图书分类数据访问。
 */
@Mapper
public interface BookCategoryMapper {

    /** bookCount 用子查询统计未删除图书数量，避免列表页逐条查询 */
    @Select("SELECT c.id, c.name, c.description, c.sort_order, c.status, c.created_at, c.updated_at,"
            + " (SELECT COUNT(*) FROM b_book b WHERE b.category_id = c.id AND b.is_deleted = 0) AS book_count"
            + " FROM b_category c WHERE c.id = #{id}")
    BookCategory selectById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT c.id, c.name, c.description, c.sort_order, c.status, c.created_at, c.updated_at,
                   (SELECT COUNT(*) FROM b_book b WHERE b.category_id = c.id AND b.is_deleted = 0) AS book_count
            FROM b_category c
            <where>
              <if test="keyword != null and keyword != ''">AND c.name LIKE CONCAT('%', #{keyword}, '%')</if>
              <if test="!includeDisabled">AND c.status = 'ACTIVE'</if>
            </where>
            ORDER BY ${orderBy}
            </script>
            """)
    List<BookCategory> selectList(@Param("keyword") String keyword,
                                  @Param("includeDisabled") boolean includeDisabled,
                                  @Param("orderBy") String orderBy);

    @Select("SELECT COUNT(*) FROM b_book WHERE category_id = #{categoryId} AND is_deleted = 0")
    long countBooks(@Param("categoryId") Long categoryId);

    /** 批量加载分类精简信息，避免列表接口按行回查产生 N+1 */
    @Select("""
            <script>
            SELECT id, name FROM b_category WHERE id IN
            <foreach item="id" collection="ids" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<BookCategory> selectBriefByIds(@Param("ids") java.util.Collection<Long> ids);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO b_category (name, description, sort_order, status)"
            + " VALUES (#{name}, #{description}, #{sortOrder}, #{status})")
    void insert(BookCategory category);

    @Update("UPDATE b_category SET name = #{name}, description = #{description},"
            + " sort_order = #{sortOrder}, status = #{status} WHERE id = #{id}")
    int update(BookCategory category);

    @Delete("DELETE FROM b_category WHERE id = #{id}")
    int delete(@Param("id") Long id);
}
