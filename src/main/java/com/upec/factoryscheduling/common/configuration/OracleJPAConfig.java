package com.upec.factoryscheduling.common.configuration;//package com.upec.factoryscheduling.common.configuration;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.JpaVendorAdapter;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import javax.annotation.Resource;
//import javax.persistence.EntityManagerFactory;
//import javax.sql.DataSource;
//import java.util.Properties;
//
//
//@Configuration
//@EnableJpaRepositories(basePackages = {
//        "com.upec.factoryscheduling.mes",
//        "com.upec.factoryscheduling.auth",
//        "com.upec.factoryscheduling.aps"
//},
//        entityManagerFactoryRef = "oracleSessionFactory",
//        transactionManagerRef = "oracleTransactionManager")
//public class OracleJPAConfig {
//
//    @Resource(name = "oracleDataSource")
//    public DataSource oracleDataSource;
//
//    @Bean("oracleJpaVendorAdapter")
//    public JpaVendorAdapter oracleJpaVendorAdapter() {
//        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
//        hibernateJpaVendorAdapter.setShowSql(true);
//        hibernateJpaVendorAdapter.setGenerateDdl(true);
//        return hibernateJpaVendorAdapter;
//    }
//
//
//    @Bean("oracleSessionFactory")
//    public LocalContainerEntityManagerFactoryBean oracleSessionFactory(@Qualifier(value = "oracleJpaVendorAdapter") JpaVendorAdapter jpaVendorAdapter) {
//        LocalContainerEntityManagerFactoryBean managerFactoryBean = new LocalContainerEntityManagerFactoryBean();
//        managerFactoryBean.setDataSource(oracleDataSource);
//        managerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
//        managerFactoryBean.setPackagesToScan("com.upec.factoryscheduling.mes", "com.upec.factoryscheduling.auth", "com.upec.factoryscheduling.aps");
//        managerFactoryBean.setPersistenceUnitName("oraclePersistenceUnit");
//        Properties jpaProperties = getProperties();
//        managerFactoryBean.setJpaProperties(jpaProperties);
//        return managerFactoryBean;
//    }
//
//    private static Properties getProperties() {
//        Properties jpaProperties = new Properties();
//        jpaProperties.put("hibernate.show_sql", "false");
//        jpaProperties.put("hibernate.format_sql", "false");
//        jpaProperties.put("hibernate.jdbc.batch_size", "200");
//        jpaProperties.put("hibernate.order_inserts", "true");
//        jpaProperties.put("hibernate.order_updates", "true");
//        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.Oracle10gDialect");
//        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
//        jpaProperties.put("hibernate.connection.charSet", "UTF-8");
//        jpaProperties.put("hibernate.connection.useUnicode", "true");
//        jpaProperties.put("hibernate.connection.defaultNCharacterStreams", "true");
//        // 启用自动提交模式
//        jpaProperties.put("hibernate.connection.autocommit", "true");
//        // 确保Hibernate立即刷新会话，避免延迟写入
//        jpaProperties.put("hibernate.flushMode", "ALWAYS");
//        // 禁用二级缓存，避免缓存导致的数据不同步
//        jpaProperties.put("hibernate.cache.use_second_level_cache", "false");
//        // 增加JDBC超时时间（秒）
//        jpaProperties.put("hibernate.jdbc.time_zone", "Asia/Shanghai");
//        // 配置事务超时相关属性
//        jpaProperties.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
//        return jpaProperties;
//    }
//
//    @Bean("oracleTransactionManager")
//    public PlatformTransactionManager transactionManager(@Qualifier("oracleSessionFactory") EntityManagerFactory factory) {
//        return new JpaTransactionManager(factory);
//    }
//
//    @Bean("oracleTemplate")
//    public JdbcTemplate oracleTemplate() {
//        return new JdbcTemplate(oracleDataSource);
//    }
//}
