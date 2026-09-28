package com.example.bookmanage.enums;

/**
 * 归还验收结果。LOST 时不回补可用库存，因为实体书已无法再借出。
 */
public enum ReturnCondition {
    GOOD,
    DAMAGED,
    LOST
}
