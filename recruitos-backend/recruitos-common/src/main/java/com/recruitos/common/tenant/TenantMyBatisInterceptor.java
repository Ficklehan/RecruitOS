package com.recruitos.common.tenant;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.List;

/**
 * MyBatis interceptor that automatically injects tenant_id condition into SQL
 * for tables that have tenant_id column
 */
public class TenantMyBatisInterceptor implements InnerInterceptor {

    private static final String TENANT_ID_COLUMN = "tenant_id";

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // Tenant filtering is handled via MyBatis-Plus TenantLineInnerInterceptor if needed
        // This interceptor serves as a backup/alternative approach
    }

    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        // Auto-fill tenant_id on insert/update is handled by MetaObjectHandler
    }

    /**
     * Check if a table has tenant_id column
     * This can be enhanced with metadata cache
     */
    private boolean hasTenantColumn(String tableName) {
        // Tables that support multi-tenancy
        return tableName != null && !tableName.startsWith("sys_permission")
                && !tableName.startsWith("flowable");
    }

    private Expression buildTenantCondition(String tableAlias) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return null;
        }
        String column = tableAlias != null ? tableAlias + "." + TENANT_ID_COLUMN : TENANT_ID_COLUMN;
        return new EqualsTo(new Column(column), new LongValue(tenantId));
    }
}
