/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.sql.ddl;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDelete;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.ImmutableNullableList;

import java.util.List;

/**
 * Parse tree for {@code UNIQUE}, {@code ASSUMEUNIQUE}, {@code PRIMARY KEY} constraints.
 *
 * <p>And {@code FOREIGN KEY}, when we support it.
 */
public class SqlKeyConstraint extends SqlCall {
  private static final SqlSpecialOperator UNIQUE =
      new SqlSpecialOperator("UNIQUE", SqlKind.UNIQUE);

  protected static final SqlSpecialOperator ASSUME_UNIQUE =
      new SqlSpecialOperator("ASSUMEUNIQUE", SqlKind.ASSUME_UNIQUE);

  protected static final SqlSpecialOperator PRIMARY =
      new SqlSpecialOperator("PRIMARY KEY", SqlKind.PRIMARY_KEY);

  private final SqlIdentifier name;
  private final SqlNodeList columnList;

  /** Creates a SqlKeyConstraint. */
  SqlKeyConstraint(SqlParserPos pos, SqlIdentifier name,
      SqlNodeList columnList) {
    super(pos);
    this.name = name;
    this.columnList = columnList;
  }

  /** Creates a UNIQUE constraint. */
  public static SqlKeyConstraint unique(SqlParserPos pos, SqlIdentifier name,
      SqlNodeList columnList) {
    return new SqlKeyConstraint(pos, name, columnList);
  }

  /** Creates an ASSUME_UNIQUE constraint. */
  public static SqlKeyConstraint assumeUnique(SqlParserPos pos, SqlIdentifier name,
      SqlNodeList columnList) {
    return new SqlKeyConstraint(pos, name, columnList) {
      @Override public SqlOperator getOperator() {
        return ASSUME_UNIQUE;
      }
    };
  }

  /** Creates a PRIMARY KEY constraint. */
  public static SqlKeyConstraint primary(SqlParserPos pos, SqlIdentifier name,
      SqlNodeList columnList) {
    return new SqlKeyConstraint(pos, name, columnList) {
      @Override public SqlOperator getOperator() {
        return PRIMARY;
      }
    };
  }

  /** Creates a LIMIT PARTITION ROWS constraint. */
  public static SqlKeyConstraint limitPartitionRows(SqlParserPos pos, SqlIdentifier name,
      int rowCount, SqlDelete delStmt) {
    return new SqlLimitPartitionRowsConstraint(pos, name, rowCount, delStmt);
  }

  @Override public SqlOperator getOperator() {
    return UNIQUE;
  }

  @Override public List<SqlNode> getOperandList() {
    return ImmutableNullableList.of(name, columnList);
  }

  @Override public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    if (name != null) {
      writer.keyword("CONSTRAINT");
      name.unparse(writer, 0, 0);
    }
    writer.keyword(getOperator().getName()); // "UNIQUE", "ASSUMEUNIQUE", or "PRIMARY KEY"
    columnList.unparse(writer, 1, 1);
  }
}

// End SqlKeyConstraint.java
