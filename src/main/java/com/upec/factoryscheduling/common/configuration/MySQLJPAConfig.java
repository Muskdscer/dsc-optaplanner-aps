package com.upec.factoryscheduling.common.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;


/**
 * MySQL数据库JPA配置类
 * 设置为主要的JPA配置，替代Oracle配置
 */
@Configuration
@EnableJpaRepositories(
        transactionManagerRef = "mysqlTransactionManager",
        entityManagerFactoryRef = "mySqlSessionFactory",
        basePackages = {"com.upec.factoryscheduling.mes","com.upec.factoryscheduling.auth"}  // 确保扫描所有存储库
)
public class MySQLJPAConfig {

    @Autowired
    @Qualifier("mysqlDataSource")
    private DataSource mysqlDataSource;

    @Bean("mySqlJpaVendorAdapter")
    public JpaVendorAdapter mySqlJpaVendorAdapter() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setShowSql(true);
        hibernateJpaVendorAdapter.setGenerateDdl(true);
        return hibernateJpaVendorAdapter;
    }


    @Bean("mySqlSessionFactory")
    public LocalContainerEntityManagerFactoryBean mySqlSessionFactory(@Qualifier(value = "mySqlJpaVendorAdapter") JpaVendorAdapter jpaVendorAdapter) {
        LocalContainerEntityManagerFactoryBean managerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        managerFactoryBean.setDataSource(mysqlDataSource);
        managerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        managerFactoryBean.setPackagesToScan("com.upec.factoryscheduling.mes", "com.upec.factoryscheduling.auth");
        managerFactoryBean.setPersistenceUnitName("mysqlPersistenceUnit");
        Properties jpaProperties = getProperties();
        managerFactoryBean.setJpaProperties(jpaProperties);
        return managerFactoryBean;
    }

    private static Properties getProperties() {
        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.show_sql", "false"); // 开启SQL日志，便于调试
        jpaProperties.put("hibernate.format_sql", "false");
        jpaProperties.put("hibernate.jdbc.batch_size", "200");
        jpaProperties.put("hibernate.jdbc.fetch_size", "100");
        jpaProperties.put("hibernate.order_inserts", "true");
        jpaProperties.put("hibernate.order_updates", "true");
        jpaProperties.put("hibernate.batch_versioned_data", "true");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.connection.charSet", "UTF-8");
        jpaProperties.put("hibernate.connection.useUnicode", "true");
        jpaProperties.put("hibernate.connection.defaultNCharacterStreams", "true");
        // 启用自动提交模式
        jpaProperties.put("hibernate.connection.autocommit", "true");
        // 确保Hibernate立即刷新会话，避免延迟写入
        jpaProperties.put("hibernate.flushMode", "ALWAYS");
        // 禁用二级缓存，避免缓存导致的数据不同步
        jpaProperties.put("hibernate.cache.use_second_level_cache", "false");
        // 增加JDBC超时时间（秒）
        jpaProperties.put("hibernate.jdbc.time_zone", "Asia/Shanghai");
        // 配置事务超时相关属性
        jpaProperties.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
        return jpaProperties;
    }

    /**
     * 配置事务管理器，支持自动提交
     * 由于代码中使用了@Transactional注解，需要确保事务管理器正确配置
     */
    @Bean(name = "mysqlTransactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("mySqlSessionFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        // 增加事务超时时间到5分钟（300秒），避免长时间运行的事务超时
        transactionManager.setDefaultTimeout(300);
        return transactionManager;
    }

    @Bean("mySqlTemplate")
    public JdbcTemplate mySqlTemplate() {
        return new JdbcTemplate(java.util.Objects.requireNonNull(mysqlDataSource));
    }
}
