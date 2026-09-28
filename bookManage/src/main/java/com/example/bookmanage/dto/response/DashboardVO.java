package com.example.bookmanage.dto.response;

/**
 * 管理端统计概览。
 *
 * <p>统计口径：bookTitles 为未删除书目数；totalCopies / availableCopies 为册数；
 * activeReaders 为产生过借阅记录的读者数；overdueOrders 为借阅中且已过应还时间的单数。
 */
public record DashboardVO(Long bookTitles,
                          Long totalCopies,
                          Long availableCopies,
                          Long activeReaders,
                          Long pendingOrders,
                          Long borrowedOrders,
                          Long overdueOrders) {
}
