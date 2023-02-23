/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sharpdev;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Incubating(since = "7.0.0")
public class MakePrivateOrFinalMethodsStatic2 extends Recipe {

    @Override
    public String getDisplayName() {
        return "Make private or final methods static if they do not access any instance data";
    }

    @Override
    public String getDescription() {
        return "Identifies private or final methods that do not access any instance data and converts them to static methods.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2325");
    }   

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, p);

                // if this already has "final", we don't need to bother going any further; we're done
                if (mv.hasModifier(J.Modifier.Type.Static) || mv.hasModifier(J.Modifier.Type.Final)) {
                    return mv;
                }

                // consider uninitialized local variables non-final
                if (mv.getVariables().stream().anyMatch(nv -> nv.getInitializer() == null)) {
                    return mv;
                }

                if(isDeclaredInForLoopControl(getCursor())) {
                    return mv;
                }

                // ignore fields (aka "instance variable" or "class variable")
                if (mv.getVariables().stream().anyMatch(v -> v.isField(getCursor()))) {
                    return mv;
                }

                if (mv.getVariables().stream()
                        .noneMatch(v -> FindAssignmentReferencesToVariable.find(getCursor().getParentTreeCursor().getValue(), v).get())) {
                    mv = autoFormat(
                            mv.withModifiers(
                                    ListUtils.concat(mv.getModifiers(), new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Final, Collections.emptyList()))
                            ), p);
                }

                return mv;
            }
        };
    }

    private boolean isDeclaredInForLoopControl(Cursor cursor) {
        return cursor.getParentTreeCursor()
                .getValue() instanceof J.ForLoop.Control;
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class FindAssignmentReferencesToVariable extends JavaIsoVisitor<AtomicBoolean> {

        J.VariableDeclarations.NamedVariable variable;

        /**
         * @param j        The subtree to search.
         * @param variable A {@link J.VariableDeclarations.NamedVariable} to check for any reassignment calls.
         * @return An {@link AtomicBoolean} that is true if the variable has been reassigned and false otherwise.
         */
        static AtomicBoolean find(J j, J.VariableDeclarations.NamedVariable variable) {
            return new FindAssignmentReferencesToVariable(variable)
                    .reduce(j, new AtomicBoolean());
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean hasAssignment) {
            if (hasAssignment.get()) {
                return assignment;
            }
            J.Assignment a = super.visitAssignment(assignment, hasAssignment);
            if (a.getVariable() instanceof J.Identifier) {
                J.Identifier i = (J.Identifier) a.getVariable();
                if (i.getSimpleName().equals(variable.getSimpleName())) {
                    hasAssignment.set(true);
                }
            }
            return a;
        }

        @Override
        public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, AtomicBoolean hasAssignment) {
            if (hasAssignment.get()) {
                return assignOp;
            }

            J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, hasAssignment);
            if (a.getVariable() instanceof J.Identifier) {
                J.Identifier i = (J.Identifier) a.getVariable();
                if (i.getSimpleName().equals(variable.getSimpleName())) {
                    hasAssignment.set(true);
                }
            }
            return a;
        }

        @Override
        public J.Unary visitUnary(J.Unary unary, AtomicBoolean hasAssignment) {
            if (hasAssignment.get()) {
                return unary;
            }

            J.Unary u = super.visitUnary(unary, hasAssignment);
            if (u.getOperator().isModifying() && u.getExpression() instanceof J.Identifier) {
                J.Identifier i = (J.Identifier) u.getExpression();
                if (i.getSimpleName().equals(variable.getSimpleName())) {
                    hasAssignment.set(true);
                }
            }
            return u;
        }
    }
}