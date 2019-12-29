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
package org.apache.calcite.adapter.enumerable;

import org.apache.calcite.linq4j.tree.BlockBuilder;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.CorrelationId;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.metadata.RelMdCollation;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.BuiltInMethod;

import com.google.common.collect.ImmutableList;

import java.util.Set;

/** Implementation of {@link org.apache.calcite.rel.core.Join} in
 * {@link org.apache.calcite.adapter.enumerable.EnumerableConvention enumerable calling convention}
 * that allows conditions that are not just {@code =} (equals). */
public class EnumerableNestedLoopJoin extends Join implements EnumerableRel {
  /** Creates an EnumerableNestedLoopJoin. */
  protected EnumerableNestedLoopJoin(RelOptCluster cluster, RelTraitSet traits,
      RelNode left, RelNode right, RexNode condition,
      Set<CorrelationId> variablesSet, JoinRelType joinType) {
    super(cluster, traits, left, right, condition, variablesSet, joinType);
  }

  @Deprecated // to be removed before 2.0
  protected EnumerableNestedLoopJoin(RelOptCluster cluster, RelTraitSet traits,
      RelNode left, RelNode right, RexNode condition, JoinRelType joinType,
      Set<String> variablesStopped) {
    this(cluster, traits, left, right, condition,
        CorrelationId.setOf(variablesStopped), joinType);
  }

  @Override public EnumerableNestedLoopJoin copy(RelTraitSet traitSet,
      RexNode condition, RelNode left, RelNode right, JoinRelType joinType,
      boolean semiJoinDone) {
    return new EnumerableNestedLoopJoin(getCluster(), traitSet, left, right,
        condition, variablesSet, joinType);
  }

  /** Creates an EnumerableNestedLoopJoin. */
  public static EnumerableNestedLoopJoin create(
      RelNode left,
      RelNode right,
      RexNode condition,
      Set<CorrelationId> variablesSet,
      JoinRelType joinType) {
    final RelOptCluster cluster = left.getCluster();
    final RelMetadataQuery mq = cluster.getMetadataQuery();
    final RelTraitSet traitSet =
        cluster.traitSetOf(EnumerableConvention.INSTANCE)
            .replaceIfs(RelCollationTraitDef.INSTANCE,
                () -> RelMdCollation.enumerableNestedLoopJoin(mq, left, right, joinType));
    return new EnumerableNestedLoopJoin(cluster, traitSet, left, right, condition,
        variablesSet, joinType);
  }

  @Override public RelOptCost computeSelfCost(RelOptPlanner planner,
      RelMetadataQuery mq) {
    final double rightRowCount = right.estimateRowCount(mq);
    final double leftRowCount = left.estimateRowCount(mq);
    if (Double.isInfinite(leftRowCount) || Double.isInfinite(rightRowCount)) {
      return planner.getCostFactory().makeInfiniteCost();
    }
    RelOptCost rightCost = planner.getCost(right, mq);
    if (rightCost.isInfinite()) {
      return rightCost;
    }
    double rowCount = mq.getRowCount(this);
    // The cost of evaluating join predicates:
    // TODO: account for joinInfo.nonEquiConditions
    double filterCost = leftRowCount * rightRowCount * OPERATOR_COST * joinInfo.leftKeys.size();
    // Note: -1 here is because the total cost of the plan would include costs of left and right
    // inputs anyway.
    RelOptCost rescanCost = rightCost.multiplyBy(Math.max(1.0, leftRowCount - 1));
    // Note: even if filter would reduce the resulting set to 1 row, we would still have to
    // restart the inner relation several times.
    // Inner relation is restarted multiple times, so an epsilon is added
    // to represent startup cost.
    RelOptCost cost = planner.getCostFactory()
        .makeCost(rowCount + filterCost, 0, 0)
        .plus(rescanCost);
    return EnumUtils.extraJoinCost(cost, this, leftRowCount, rightRowCount);
  }

  public Result implement(EnumerableRelImplementor implementor, Prefer pref) {
    final BlockBuilder builder = new BlockBuilder();
    final Result leftResult =
        implementor.visitChild(this, 0, (EnumerableRel) left, pref);
    Expression leftExpression =
        builder.append("left", leftResult.block);
    final Result rightResult =
        implementor.visitChild(this, 1, (EnumerableRel) right, pref);
    Expression rightExpression =
        builder.append("right", rightResult.block);
    final PhysType physType =
        PhysTypeImpl.of(implementor.getTypeFactory(),
            getRowType(),
            pref.preferArray());
    final Expression predicate =
        EnumUtils.generatePredicate(implementor, getCluster().getRexBuilder(), left, right,
            leftResult.physType, rightResult.physType, condition);
    return implementor.result(
        physType,
        builder.append(
            Expressions.call(BuiltInMethod.NESTED_LOOP_JOIN.method,
                leftExpression,
                rightExpression,
                predicate,
                EnumUtils.joinSelector(joinType,
                    physType,
                    ImmutableList.of(leftResult.physType,
                        rightResult.physType)),
                Expressions.constant(EnumUtils.toLinq4jJoinType(joinType))))
            .toBlock());
  }
}
