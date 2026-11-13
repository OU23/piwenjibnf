package com.asset.management.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 统计分析自定义查询
 */
@Mapper
public interface StatisticsMapper {

    @Select("SELECT status AS name, COUNT(*) AS value FROM asset WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus();

    @Select("SELECT c.category_name AS name, COUNT(a.id) AS value " +
            "FROM asset a LEFT JOIN asset_category c ON a.category_id = c.id " +
            "WHERE a.deleted = 0 GROUP BY a.category_id, c.category_name ORDER BY value DESC")
    List<Map<String, Object>> countByCategory();

    @Select("SELECT COALESCE(SUM(price), 0) FROM asset WHERE deleted = 0")
    BigDecimal sumAssetValue();

    @Select("SELECT COUNT(*) FROM asset_application WHERE deleted = 0 AND status = #{status}")
    Long countApplicationByStatus(@Param("status") String status);

    @Select("SELECT COUNT(*) FROM asset_return WHERE deleted = 0 AND status = #{status}")
    Long countReturnByStatus(@Param("status") String status);

    @Select("SELECT COUNT(*) FROM asset_allocation WHERE deleted = 0 AND status = #{status}")
    Long countAllocationByStatus(@Param("status") String status);

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS count " +
            "FROM asset WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) " +
            "GROUP BY month ORDER BY month")
    List<Map<String, Object>> monthlyAssetTrend();

    @Select("SELECT DATE_FORMAT(allocate_time, '%Y-%m') AS month, COUNT(*) AS count " +
            "FROM asset_allocation WHERE deleted = 0 AND allocate_time >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) " +
            "GROUP BY month ORDER BY month")
    List<Map<String, Object>> monthlyAllocationTrend();
}
