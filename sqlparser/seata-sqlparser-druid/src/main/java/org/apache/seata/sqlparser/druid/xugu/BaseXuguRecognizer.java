/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.sqlparser.druid.xugu;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.dialect.xugu.ast.statement.XuGuDeleteStatement;
import com.alibaba.druid.sql.dialect.xugu.ast.statement.XuGuUpdateStatement;
import com.alibaba.druid.sql.dialect.xugu.visitor.XuGuASTVisitor;
import com.alibaba.druid.sql.dialect.xugu.visitor.XuGuASTVisitorAdapter;
import com.alibaba.druid.sql.dialect.xugu.visitor.XuGuOutputVisitor;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.druid.BaseRecognizer;
import org.apache.seata.sqlparser.struct.Null;
import org.apache.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BaseXuguRecognizer extends BaseRecognizer {

    /**
     * Instantiates a new xugu base recognizer
     *
     * @param originalSql the original sql
     */
    public BaseXuguRecognizer(String originalSql) {
        super(originalSql);
    }

    public XuGuOutputVisitor createOutputVisitor(
            final ParametersHolder parametersHolder,
            final ArrayList<List<Object>> paramAppenderList,
            final StringBuilder sb) {
        return new XuGuOutputVisitor(sb) {

            @Override
            public boolean visit(SQLVariantRefExpr x) {
                if ("?".equals(x.getName())) {
                    ArrayList<Object> oneParamValues =
                            parametersHolder.getParameters().get(x.getIndex() + 1);
                    if (paramAppenderList.isEmpty()) {
                        oneParamValues.forEach(t -> paramAppenderList.add(new ArrayList<>()));
                    }
                    for (int i = 0; i < oneParamValues.size(); i++) {
                        Object o = oneParamValues.get(i);
                        paramAppenderList.get(i).add(o instanceof Null ? null : o);
                    }
                }
                return super.visit(x);
            }
        };
    }

    public String getWhereCondition(
            SQLExpr where, final ParametersHolder parametersHolder, final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        executeVisit(where, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    public String getWhereCondition(SQLExpr where) {
        if (Objects.isNull(where)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        executeVisit(where, new XuGuOutputVisitor(sb));
        return sb.toString();
    }

    protected String getLimitCondition(SQLLimit sqlLimit) {
        if (Objects.isNull(sqlLimit)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeLimit(sqlLimit, new XuGuOutputVisitor(sb));

        return sb.toString();
    }

    protected String getLimitCondition(
            SQLLimit sqlLimit,
            final ParametersHolder parametersHolder,
            final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(sqlLimit)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();

        executeLimit(sqlLimit, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    protected String getOrderByCondition(SQLOrderBy sqlOrderBy) {
        if (Objects.isNull(sqlOrderBy)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeOrderBy(sqlOrderBy, new XuGuOutputVisitor(sb));

        return sb.toString();
    }

    protected String getOrderByCondition(
            SQLOrderBy sqlOrderBy,
            final ParametersHolder parametersHolder,
            final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(sqlOrderBy)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeOrderBy(sqlOrderBy, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    protected String getJoinCondition(
            SQLExpr joinCondition,
            final ParametersHolder parametersHolder,
            final ArrayList<List<Object>> paramAppenderList) {
        if (Objects.isNull(joinCondition)) {
            return StringUtils.EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        executeVisit(joinCondition, createOutputVisitor(parametersHolder, paramAppenderList, sb));
        return sb.toString();
    }

    @Override
    public boolean isSqlSyntaxSupports() {
        XuGuASTVisitor visitor = new XuGuASTVisitorAdapter() {
            // 自1.6.0版本，MySQL支持UPDATE JOIN语句
            /*@Override
            public boolean visit(SQLJoinTableSource x) {
                // just like: UPDATE table a INNER JOIN table b ON a.id = b.pid ...
                throw new NotSupportYetException(
                        "not support the sql syntax with join table:" + x
                                + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
            }*/

            @Override
            public boolean visit(XuGuDeleteStatement x) {
                // just like: DELETE tb_top t1 FROM tb_top_merge t2 WHERE t1.id = t2.id AND t2.tid > 1;
                if (x.getFrom() != null) {
                    throw new NotSupportYetException(
                            "not support the sql syntax with join table:" + x
                                    + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
                }
                return true;
            }

            @Override
            public boolean visit(XuGuUpdateStatement x) {
                if (x.getTableSource() instanceof SQLSubqueryTableSource) {
                    // just like: "update (select a.id,a.name from a inner join b on a.id = b.id) t set t.name = 'xxx'"
                    throw new NotSupportYetException(
                            "not support the sql syntax with join table:" + x
                                    + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
                }
                /*List<SQLUpdateSetItem> updateSetItems = x.getItems();
                for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                    if (updateSetItem.getValue() instanceof SQLQueryExpr) {
                        // just like: "update a set a.id = (select id from b where a.pid = b.pid)"
                        throw new NotSupportYetException(
                                "not support the sql syntax with join table:" + x
                                        + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
                    }
                }*/
                if (x.getFrom() != null) {
                    // just like: update a set id = b.pid from b where a.id = b.id
                    throw new NotSupportYetException(
                            "not support the sql syntax with join table:" + x
                                    + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
                }
                return true;
            }

            @Override
            public boolean visit(SQLInSubQueryExpr x) {
                // just like: ...where id in (select id from t)
                throw new NotSupportYetException(
                        "not support the sql syntax with InSubQuery:" + x
                                + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
            }

            @Override
            public boolean visit(SQLSubqueryTableSource x) {
                // just like: select * from (select * from t)
                throw new NotSupportYetException(
                        "not support the sql syntax with SubQuery:" + x
                                + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
            }

            @Override
            public boolean visit(SQLInsertStatement x) {
                if (null != x.getQuery()) {
                    // just like: insert into t select * from t1
                    throw new NotSupportYetException(
                            "not support the sql syntax insert with query:" + x
                                    + "\nplease see the doc about SQL restrictions https://seata.apache.org/zh-cn/docs/user/sqlreference/dml");
                }
                return true;
            }
        };
        getAst().accept(visitor);
        return true;
    }

    public String getDbType() {
        return JdbcConstants.XUGU;
    }
}
