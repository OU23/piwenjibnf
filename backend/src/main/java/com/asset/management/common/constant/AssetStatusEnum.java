package com.asset.management.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资产状态枚举
 * 状态机流转：
 *   IN_STOCK(在库) ──申请占用──> APPLIED(已申请) ──分配──> ALLOCATED(已分配) ──领用──> IN_USE(使用中)
 *   IN_STOCK ──直接分配──> ALLOCATED ──领用──> IN_USE
 *   IN_USE ──提交归还──> PENDING_RETURN(待归还) ──审核通过──> IN_STOCK
 *   PENDING_RETURN ──审核驳回──> IN_USE
 *   IN_USE ──报修──> REPAIRING(维修中) ──维修完成──> IN_USE
 *   IN_STOCK/IN_USE/REPAIRING ──报废──> SCRAPPED(已报废)
 */
@Getter
@AllArgsConstructor
public enum AssetStatusEnum {

    IN_STOCK("IN_STOCK", "在库"),
    APPLIED("APPLIED", "已申请"),
    ALLOCATED("ALLOCATED", "已分配"),
    IN_USE("IN_USE", "使用中"),
    PENDING_RETURN("PENDING_RETURN", "待归还"),
    REPAIRING("REPAIRING", "维修中"),
    SCRAPPED("SCRAPPED", "已报废");

    private final String code;
    private final String desc;

    public static String getDesc(String code) {
        for (AssetStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getDesc();
            }
        }
        return code;
    }
}
