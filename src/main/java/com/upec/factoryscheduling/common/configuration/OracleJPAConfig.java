//package com.upec.factoryscheduling.common.configuration;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
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
//
//@Configuration
//@EnableJpaRepositories(basePackages = "com.upec.factoryscheduling.mes", entityManagerFactoryRef = "oracleSessionFactory")
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
//        managerFactoryBean.setPackagesToScan("com.upec.factoryscheduling.mes");
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
//        jpaProperties.put("hibernate.hbm2ddl.auto", "none");
//        jpaProperties.put("hibernate.connection.charSet", "UTF-8");
//        jpaProperties.put("hibernate.connection.useUnicode", "true");
//        jpaProperties.put("hibernate.connection.defaultNCharacterStreams", "true");
//        return jpaProperties;
//    }
//
////    @Bean("transactionManager")
////    public PlatformTransactionManager transactionManager(@Qualifier("oracleSessionFactory") EntityManagerFactory factory) {
////        return new JpaTransactionManager(factory);
////    }
//
//    @Bean("oracleTemplate")
//    public JdbcTemplate oracleTemplate() {
//        return new JdbcTemplate(oracleDataSource);
//    }
//}
