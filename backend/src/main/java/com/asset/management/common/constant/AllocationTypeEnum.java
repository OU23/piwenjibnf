package com.asset.management.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资产分配方式
 */
@Getter
@AllArgsConstructor
public enum AllocationTypeEnum {

    MANUAL("MANUAL", "手动分配"),
    AUTO("AUTO", "自动分配");

    private final String code;
    private final String desc;
}
