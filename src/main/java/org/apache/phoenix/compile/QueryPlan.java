/*
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.phoenix.compile;

import java.sql.SQLException;
import java.util.List;

import org.apache.phoenix.compile.GroupByCompiler.GroupBy;
import org.apache.phoenix.compile.OrderByCompiler.OrderBy;
import org.apache.phoenix.parse.FilterableStatement;
import org.apache.phoenix.query.KeyRange;
import org.apache.phoenix.query.Scanner;
import org.apache.phoenix.schema.TableRef;



/**
 * 
 * Interface for an executable query plan
 *
 * @author jtaylor
 * @since 0.1
 */
public interface QueryPlan extends StatementPlan {
    /**
     * Get a scanner to iterate over the results
     * @return scanner for iterating over the results
     * @throws SQLException
     */
    Scanner getScanner() throws SQLException;
    
    // TODO: change once joins are supported
    TableRef getTableRef();
    /**
     * Returns projector used to formulate resultSet row
     */
    RowProjector getProjector();
    
    Integer getLimit();

    OrderBy getOrderBy();

    GroupBy getGroupBy();

    List<KeyRange> getSplits();

    StatementContext getContext();
    
    FilterableStatement getStatement();
}
