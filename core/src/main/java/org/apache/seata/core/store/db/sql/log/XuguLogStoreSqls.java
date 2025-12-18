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
package org.apache.seata.core.store.db.sql.log;

import org.apache.seata.common.loader.LoadLevel;

/**
 * Database log store xugu sql
 *
 * @since 2.1.0
 */
@LoadLevel(name = "xugu")
public class XuguLogStoreSqls extends MysqlLogStoreSqls {

    /**
     * The constant INSERT_BRANCH_TRANSACTION_XUGU.
     */
    public static final String INSERT_BRANCH_TRANSACTION_XUGU = "insert into " + BRANCH_TABLE_PLACEHOLD
            + "(" + ALL_BRANCH_COLUMNS + ")"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())";

    @Override
    public String getInsertBranchTransactionSQL(String branchTable) {
        return INSERT_BRANCH_TRANSACTION_XUGU.replace(BRANCH_TABLE_PLACEHOLD, branchTable);
    }
}
