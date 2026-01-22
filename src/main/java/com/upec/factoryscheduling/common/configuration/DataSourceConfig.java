package com.upec.factoryscheduling.common.configuration;


import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

//    @Bean(name = "h2DataSource")
//    @ConfigurationProperties(prefix = "spring.datasource.h2")
//    public DataSource h2DataSource() {
//        return DataSourceBuilder.create().type(HikariDataSource.class).build();
//    }

//    @Bean(name = "oracleDataSource")
//    @Primary
//    @ConfigurationProperties(prefix = "spring.datasource.oracle")
//    public DataSource oracleDataSource() {
//        return DataSourceBuilder.create().type(HikariDataSource.class).build();
//    }

    @Bean(name = "mysqlDataSource")
    @Primary  // 设置MySQL为主要数据源
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

}
