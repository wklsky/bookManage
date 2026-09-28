package com.example.bookmanage.mapper;

import com.example.bookmanage.entity.BookStockLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * 库存流水数据访问。每一笔库存调整都必须留痕，便于追溯责任人与原因。
 */
@Mapper
public interface BookStockLogMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO b_book_stock_log (book_id, operator_id, change_amount, reason)"
            + " VALUES (#{bookId}, #{operatorId}, #{changeAmount}, #{reason})")
    void insert(BookStockLog log);
}
