package com.recruitos.common.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.recruitos.common.tenant.TenantContext;
import com.recruitos.common.tenant.TenantMyBatisInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus configuration
 */
@Configuration
public class MyBatisPlusConfig {

    private static final Logger log = LoggerFactory.getLogger(MyBatisPlusConfig.class);

    /**
     * MetaObjectHandler for auto-filling tenantId, createdAt, updatedAt
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                log.debug("Auto-fill on insert");
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                Long tenantId = TenantContext.getTenantId();
                if (tenantId != null) {
                    this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                log.debug("Auto-fill on update");
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }

    /**
     * MyBatis-Plus interceptor chain
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // Pagination plugin
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        // Tenant interceptor
        interceptor.addInnerInterceptor(new TenantMyBatisInterceptor());
        return interceptor;
    }
}
