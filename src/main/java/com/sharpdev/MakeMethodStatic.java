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

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@Incubating(since = "7.0.0")
public class MakeMethodStatic extends Recipe {

    @Override
    public String getDisplayName() {
        return "Finalize local variables";
    }

    @Override
    public String getDescription() {
        return "Adds the `final` modifier keyword to local variables which are not reassigned.";
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                J.MethodDeclaration  md = super.visitMethodDeclaration(method, p);

                // if this is not a private or final method, ignore
                if (!md.hasModifier(J.Modifier.Type.Private) && !md.hasModifier(J.Modifier.Type.Final)) {
                    return md;
                }

                // if this is already static, ignore
                if (md.hasModifier(J.Modifier.Type.Static)) {
                    return md;
                }

                boolean instanceAccessFound = FindReferencesToNonStaticFieldsOrMethods.find(getCursor().getParentTreeCursor().getValue(), md.getBody()).get();
                if (!instanceAccessFound) {
                    md = autoFormat(
                            md.withModifiers(
                                    ListUtils.concat(md.getModifiers(), new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList()))
                            ), p);
                }

                return md;
            }
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class FindReferencesToNonStaticFieldsOrMethods extends JavaIsoVisitor<AtomicBoolean> {

        J.Block block;

        /**
         * @param j        The subtree to search.
         * @param variable A {@link J.Block} to check for any instance field access.
         * @return An {@link AtomicBoolean} that is true if any instance field access was found.
         */
        static AtomicBoolean find(J j, J.Block block) {
            return new FindReferencesToNonStaticFieldsOrMethods(block)
                    .reduce(j, new AtomicBoolean());
        }

        @Override
        public org.openrewrite.java.tree.J.FieldAccess visitFieldAccess(
                org.openrewrite.java.tree.J.FieldAccess fieldAccess, AtomicBoolean hasInstanceRef) {
            
                    if (fieldAccess.getTarget() instanceof J.Identifier) {
                        J.Identifier identifier = (J.Identifier) fieldAccess.getTarget();
                        Object type = identifier.getFieldType();

                        if (type != null) {
                            hasInstanceRef.set(true);
                        }
                    }

            return super.visitFieldAccess(fieldAccess, hasInstanceRef);
        }

        @Override
        public org.openrewrite.java.tree.J.MethodInvocation visitMethodInvocation(
                org.openrewrite.java.tree.J.MethodInvocation method, AtomicBoolean hasInstanceRef) {

            if (!Modifier.isStatic(method.getType().getClass().getModifiers())) {
                hasInstanceRef.set(true);
            }

            for (org.openrewrite.java.tree.Expression expression : method.getArguments()) {
                if (expression instanceof J.MethodInvocation) {
                    super.visitMethodInvocation((J.MethodInvocation) expression, hasInstanceRef);
                } else if (expression instanceof J.Identifier) {  
                    J.Identifier identifier = (J.Identifier) expression;
                    Object type = identifier.getFieldType();

                    if (!Modifier.isStatic(identifier.getFieldType().getOwner().getClass().getModifiers())) {
                        hasInstanceRef.set(true);
                    }                       
                }         
            }

            return super.visitMethodInvocation(method, hasInstanceRef);
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean hasInstanceRef) {
            if (hasInstanceRef.get()) {
                return assignment;
            }
            J.Assignment a = super.visitAssignment(assignment, hasInstanceRef);
            if (a.getVariable() instanceof J.Identifier) {
                J.Identifier i = (J.Identifier) a.getVariable();
                // if (i.getSimpleName().equals(block.getSimpleName())) {
                //     hasInstanceRef.set(true);
                // }
            }
            return a;
        }
    }
}