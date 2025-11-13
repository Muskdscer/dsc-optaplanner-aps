package com.upec.factoryscheduling.common.utils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

/**
 * JdbcTemplate分页查询扩展工具类
 * 精简版：提供基本的JdbcTemplate分页查询功能，所有查询信息都包含在SQL语句中
 */
public class JdbcTemplatePaginationUtils {

    /**
     * 基本分页查询方法
     * 
     * @param jdbcTemplate JdbcTemplate实例
     * @param sql 查询SQL（包含WHERE条件和排序，但不包含分页）
     * @param rowMapper 结果映射器
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页数量
     * @param <T> 返回类型
     * @return 分页结果
     */
    public static <T> Page<T> queryForPage(
            JdbcTemplate jdbcTemplate,
            String sql,
            RowMapper<T> rowMapper,
            Integer pageNum,
            Integer pageSize) {
        
        // 参数验证和默认值设置
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        
        // 构建总数查询SQL
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") temp_count";
        
        // 执行总数查询
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class);
        
        // 计算偏移量
        int offset = (pageNum - 1) * pageSize;
        
        // 构建分页查询SQL（添加LIMIT和OFFSET）
        String pageSql = sql + " LIMIT " + pageSize + " OFFSET " + offset;
        
        // 执行分页查询
        List<T> resultList = jdbcTemplate.query(pageSql, rowMapper);
        
        // 创建Pageable对象
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        
        // 返回Page对象
        return new PageImpl<>(resultList, pageable, total);
    }
}