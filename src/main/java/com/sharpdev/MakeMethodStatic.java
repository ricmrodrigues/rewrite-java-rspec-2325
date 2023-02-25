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

import org.abego.treelayout.internal.util.java.util.ListUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.J.Assignment;
import org.openrewrite.java.tree.J.AssignmentOperation;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.FieldAccess;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodDeclaration;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.J.VariableDeclarations;
import org.openrewrite.java.tree.JavaType.Variable;
import org.openrewrite.marker.Markers;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

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

                // if method is empty let's ignore, for now 
                if (md.getBody().getStatements().isEmpty()) {
                    return md;
                }

                //System.out.println(TreeVisitingPrinter.printTree(getCursor()));

                System.out.println(TreeVisitingPrinter.printTree(getCursor()));

                AtomicBoolean foundInstanceAccess = FindInstanceUsagesWithinMethod.find(getCursor().getValue(), md.getBody());
                //boolean noInstanceMethodAccess = false;
                //  md.get.stream()
                //     .filter(s -> s instanceof J.MethodInvocation)
                //     .map(s -> (J.MethodInvocation) s)
                //     .noneMatch(s -> FindReferencesToInstanceMethods.find(getCursor().getParentTreeCursor().getValue(), s).get());


                if (!foundInstanceAccess.get()) {
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
    private static class FindInstanceUsagesWithinMethod extends JavaIsoVisitor<AtomicBoolean> {

        J.Block block;

        /**
         * @param subtree   The subtree to search.
         * @param block  A {@link J.Block} to check for instance access.
         * @return An {@link AtomicBoolean} that is true if instance access has been found.
         */
        static AtomicBoolean find(J subtree, J.Block block) {
            return new FindInstanceUsagesWithinMethod(block)
                    .reduce(subtree, new AtomicBoolean());
        }

        @Override
        public FieldAccess visitFieldAccess(FieldAccess fieldAccess, AtomicBoolean hasInstanceAccess) {            
            
            FieldAccess fa = super.visitFieldAccess(fieldAccess, hasInstanceAccess);
            
            if (fa.getTarget() instanceof J.Identifier) {
                J.Identifier identifier = (J.Identifier) fa.getTarget();
                if (identifier.getFieldType() != null) {
                    // if we have field access and target has fieldType, means we're accessing something from the instance
                    hasInstanceAccess.set(true);
                }
            }
            
            return fa;
        }

        // @Override
        // public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean hasInstanceAccess) {
        //     J.Identifier i = super.visitIdentifier(identifier, hasInstanceAccess);

        //     Variable fieldType = i.getFieldType();
        //     if (fieldType != null && !fieldType.getOwner().toString().startsWith("java.lang.")) {
        //         hasInstanceAccess.set(true);
        //     }

        //     if (i.getSimpleName().equals("something")) {
        //         boolean field = isField(getCursor());
        //         System.out.println(i.getSimpleName());
        //     }

        //     return i;
        // }     

        @Override
        public MethodInvocation visitMethodInvocation(MethodInvocation method, AtomicBoolean hasInstanceAccess) {
            // if instance access has been found already, return immediately
            if (hasInstanceAccess.get()) {
                return method;
            }

            MethodInvocation mi = super.visitMethodInvocation(method, hasInstanceAccess);

            //Class<?> c = Class.forName(args[0])
            J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);

            // for (JavaType.Method m : mi.getMethodType().getDeclaringType().getMethods())
            // {
            //     if (m.toString().equals(mi.getMethodType().toString())) {

            //         m.

            //         if (!Modifier.isStatic(m.getModifiers())) {
            //             hasInstanceAccess.set(true);
            //             break;
            //         }                    
            //     }
            // }

            // Method targetMethod = Arrays.asList().stream().findFirst(Method m -> m.equals(mi.getSimpleName()));
            // if (!Modifier.isStatic(targetMethod.getModifiers())) {
            //     hasInstanceAccess.set(true);
            // }

            // Arrays.asList().stream().findAny(m -> m)

            // J.ClassDeclaration methodDeclaration = getCursor().dropParentUntil(parent -> parent instanceof J.ClassDeclaration).getValue();
            // J.ClassDeclaration classDecl = getCursor().getParentOrThrow().firstEnclosing(J.ClassDeclaration.class);

            // boolean isTargetAnInstanceMethod = Arrays.asList(classDecl.getClass().getMethods()).stream()
            //     .anyMatch(m -> m.getName().equals(mi.getSimpleName()) && !Modifier.isStatic(m.getModifiers()));

            // if (isTargetAnInstanceMethod) {
            //     hasInstanceAccess.set(true);
            // }
            //Method m = java.lang.reflect.Method.class.getMethod(mi.getSimpleName(), mi.getMethodType().getDeclaringType().getClass());
            //Optional<Method> foundMethod = Arrays.asList(mi.getMethodType().getDeclaringType().getClass().getMethods()).stream().filter(m -> m.getName() == mi.getSimpleName()).findFirst();

            //JavaType methodType = mi.getName().getType();
            //mi.getMethodType().getClass().getModifiers()

            return mi;
        }

        // TODO: Implement visit methods
    }    
}