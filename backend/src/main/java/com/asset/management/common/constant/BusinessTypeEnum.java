package com.asset.management.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批业务类型（用于审批流水表）
 */
@Getter
@AllArgsConstructor
public enum BusinessTypeEnum {

    APPLICATION("APPLICATION", "资产申请"),
    RETURN("RETURN", "资产归还");

    private final String code;
    private final String desc;
}
