package com.example.bookmanage.service;

import com.example.bookmanage.dto.response.DashboardVO;
import com.example.bookmanage.enums.OrderStatus;
import com.example.bookmanage.mapper.BookMapper;
import com.example.bookmanage.mapper.BorrowOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端统计概览。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BookMapper bookMapper;
    private final BorrowOrderMapper orderMapper;

    @Transactional(readOnly = true)
    public DashboardVO summary() {
        return new DashboardVO(
                bookMapper.countTitles(),
                bookMapper.sumTotalStock(),
                bookMapper.sumAvailableStock(),
                orderMapper.countBorrowers(),
                orderMapper.countByStatus(OrderStatus.PENDING),
                orderMapper.countByStatus(OrderStatus.BORROWED),
                orderMapper.countOverdue());
    }
}
