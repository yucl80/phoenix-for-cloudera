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
package org.apache.phoenix.parse;

import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;


/**
 * 
 * Node representing the less than operator (<) in SQL
 *
 * @author jtaylor
 * @since 0.1
 */
public class LessThanParseNode extends ComparisonParseNode {

    LessThanParseNode(ParseNode lhs, ParseNode rhs) {
        super(lhs, rhs);
    }

    @Override
    public CompareOp getFilterOp() {
        return CompareFilter.CompareOp.LESS;
    }

    @Override
    public CompareOp getInvertFilterOp() {
        return CompareOp.GREATER;
    }
}
