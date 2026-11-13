package com.asset.management.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资产归还单状态
 * PENDING(待审核) -> COMPLETED(已归还) / REJECTED(已驳回)
 */
@Getter
@AllArgsConstructor
public enum ReturnStatusEnum {

    PENDING("PENDING", "待审核"),
    COMPLETED("COMPLETED", "已归还"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String desc;

    public static String getDesc(String code) {
        for (ReturnStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return code;
    }
}
