package com.upec.factoryscheduling.common.configuration;

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

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableJpaRepositories(
        transactionManagerRef = "h2TransactionManager",
        basePackages = "com.upec.factoryscheduling.aps",
        entityManagerFactoryRef = "h2SessionFactory")
public class H2JPAConfig {

    @Resource(name = "h2DataSource")
    public DataSource h2DataSource;

    @Bean("h2JpaVendorAdapter")
    @Primary
    public JpaVendorAdapter h2JpaVendorAdapter() {
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
        hibernateJpaVendorAdapter.setShowSql(true);
        hibernateJpaVendorAdapter.setGenerateDdl(true);
        return hibernateJpaVendorAdapter;
    }


    @Bean("h2SessionFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean h2SessionFactory(@Qualifier("h2JpaVendorAdapter") JpaVendorAdapter jpaVendorAdapter) {
        LocalContainerEntityManagerFactoryBean managerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        managerFactoryBean.setDataSource(h2DataSource);
        managerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        managerFactoryBean.setPackagesToScan("com.upec.factoryscheduling.aps");
        managerFactoryBean.setPersistenceUnitName("h2PersistenceUnit");
        Properties jpaProperties = getProperties();
        managerFactoryBean.setJpaProperties(jpaProperties);
        return managerFactoryBean;
    }

    private static Properties getProperties() {
        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.show_sql", "false");
        jpaProperties.put("hibernate.format_sql", "false");
        jpaProperties.put("hibernate.jdbc.batch_size", "200");
        jpaProperties.put("hibernate.order_inserts", "true");
        jpaProperties.put("hibernate.order_updates", "true");
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        jpaProperties.put("hibernate.connection.charSet", "UTF-8");
        jpaProperties.put("hibernate.connection.useUnicode", "true");
        jpaProperties.put("hibernate.connection.defaultNCharacterStreams", "true");
        // 增加时区设置
        jpaProperties.put("hibernate.jdbc.time_zone", "Asia/Shanghai");
        // 配置事务超时相关属性
        jpaProperties.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
        return jpaProperties;
    }

    @Bean("h2TransactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("h2SessionFactory") EntityManagerFactory factory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager(factory);
        // 增加事务超时时间到5分钟（300秒），避免长时间运行的事务超时
        transactionManager.setDefaultTimeout(300);
        return transactionManager;
    }

    @Bean("h2Template")
    public JdbcTemplate h2Template() {
        return new JdbcTemplate(h2DataSource);
    }
}
