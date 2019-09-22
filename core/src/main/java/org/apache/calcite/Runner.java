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
package org.apache.calcite;

import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.adapter.enumerable.EnumerableRules;
import org.apache.calcite.adapter.java.ReflectiveSchema;
import org.apache.calcite.config.Lex;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.custom.BestMatchOverNullifyRule;
import org.apache.calcite.rel.rules.custom.BestMatchPullUpRule;
import org.apache.calcite.rel.rules.custom.BestMatchReduceRule;
import org.apache.calcite.rel.rules.custom.NullifyJoinRule;
import org.apache.calcite.rel.rules.custom.NullifyPullUpRule;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.Program;
import org.apache.calcite.tools.Programs;

/**
 * A runner class for manual testing.
 */
public class Runner {
  public static void main(String[] args) throws Exception {
    // Builds the schema.
    final SchemaPlus rootSchema = Frameworks.createRootSchema(true);
    final SchemaPlus defaultSchema = rootSchema.add("p", new ReflectiveSchema(new People()));

    // Creates the planner.
    final SqlParser.Config parserConfig = SqlParser.configBuilder().setLex(Lex.MYSQL).build();
    final Program programs = Programs.ofRules(
        BestMatchOverNullifyRule.INSTANCE,
        BestMatchPullUpRule.INSTANCE,
        BestMatchReduceRule.INSTANCE,
        NullifyJoinRule.INSTANCE,
        NullifyPullUpRule.INSTANCE,
        EnumerableRules.ENUMERABLE_PROJECT_RULE,
        EnumerableRules.ENUMERABLE_JOIN_RULE);
    final FrameworkConfig config = Frameworks.newConfigBuilder()
        .parserConfig(parserConfig)
        .defaultSchema(defaultSchema)
        .programs(programs)
        .build();
    final Planner planner = Frameworks.getPlanner(config);

    // A single left outer join.
    String sqlQuery = "select e.name, d.depName "
        + "from p.employees e left join p.departments d on e.depID = d.depID";
    buildAndTransformQuery(planner, sqlQuery);

    // Two joins (left outer join + inner join).
    sqlQuery = "select e.name, d.depName, c.cmpName "
        + "from p.employees e left join p.departments d on e.depID = d.depID "
        + "join p.companies c on d.cmpID = c.cmpID";
    buildAndTransformQuery(planner, sqlQuery);

    // Closes the planner eventually.
    planner.close();
  }

  /**
   * This method emulates the whole life cycle of a given SQL query: parse, validate build and
   * transform. It will close and reset the planner after usage.
   *
   * @param planner is the planner to be used during the life cycle.
   * @param sqlQuery is the original SQL query in its string representation.
   * @throws Exception when there is error during any step.
   */
  private static void buildAndTransformQuery(Planner planner, String sqlQuery) throws Exception {
    System.out.println("============================ Start ============================");

    // Parses, validates and builds the query.
    SqlNode parse = planner.parse(sqlQuery);
    SqlNode validate = planner.validate(parse);
    RelNode relNode = planner.rel(validate).rel;
    System.out.println("Before transformation:\n");
    System.out.println(RelOptUtil.toString(relNode));

    // Transforms the query.
    RelTraitSet traitSet = relNode.getTraitSet().replace(EnumerableConvention.INSTANCE);
    RelNode transformedNode = planner.transform(0, traitSet, relNode);
    System.out.println("After transformation:\n");
    System.out.println(RelOptUtil.toString(transformedNode));

    // Closes and resets the planner.
    planner.close();
    planner.reset();

    System.out.println("============================= End =============================\n");
  }

  /**
   *  Represents the database named company. */
  public static class People {
    public final Employee[] employees = {
        new Employee(10, 1, "Daniel"),
        new Employee(20, 1, "Mark"),
        new Employee(30, 2, "Smith"),
        new Employee(40, 3, "Armstrong")
    };

    public final Department[] departments = {
        new Department(1, "Engineering", 100),
        new Department(2, "Finance", 100)
    };

    public final Company[] companies = {
        new Company(100, "All Link Pte Ltd"),
        new Company(200, "")
    };
  }

  /**
   *  Represents the schema of the employee table. */
  public static class Employee {
    public final int empID;
    public final int depID;
    public final String name;

    Employee(int empID, int depID, String name) {
      this.empID = empID;
      this.depID = depID;
      this.name = name;
    }
  }

  /**
   *  Represents the schema of the department table. */
  public static class Department {
    public final int depID;
    public final String depName;
    public final int cmpID;

    Department(int depID, String depName, int cmpID) {
      this.depID = depID;
      this.depName = depName;
      this.cmpID = cmpID;
    }
  }

  /**
   *  Represents the schema of the company table. */
  public static class Company {
    public final int cmpID;
    public final String cmpName;

    Company(int cmpID, String cmpName) {
      this.cmpID = cmpID;
      this.cmpName = cmpName;
    }
  }

  private Runner() {
  }
}

// End Runner.java
