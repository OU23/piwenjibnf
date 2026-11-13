package com.asset.management.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资产申请单状态
 * PENDING(待审批) -> APPROVED(已通过) / REJECTED(已驳回) / CANCELLED(已撤销)
 */
@Getter
@AllArgsConstructor
public enum ApplicationStatusEnum {

    PENDING("PENDING", "待审批"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    CANCELLED("CANCELLED", "已撤销");

    private final String code;
    private final String desc;

    public static String getDesc(String code) {
        for (ApplicationStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return code;
    }
}
