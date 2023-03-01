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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.J.FieldAccess;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JavaType.Variable;
import org.openrewrite.marker.Markers;
import org.openrewrite.scheduling.WatchableExecutionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;

@Incubating(since = "7.0.0")
public class MakePrivateOrFinalMethodsStatic extends Recipe {

    @Override
    public String getDisplayName() {
        return "Try to make 'private' and 'final' methods static";
    }

    @Override
    public String getDescription() {
        return "RSPEC-2325: 'private' and 'final' methods that don't access instance data should be 'static'.";
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visitSourceFile(SourceFile sourceFile, ExecutionContext p) {
                SourceFile ss = (SourceFile) super.visitSourceFile(sourceFile, p);

                SourceFile newSource = (SourceFile) new ApplyStaticIfApplicable().visitNonNull(ss, p);
                WatchableExecutionContext ec = (WatchableExecutionContext) p;
                while (ec.hasNewMessages()) {
                    ec.resetHasNewMessages();
                    newSource = (SourceFile) new ApplyStaticIfApplicable().visitNonNull(newSource, p);
                    ec = (WatchableExecutionContext) p;
                }           

                return (J) newSource;
            }    
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class ApplyStaticIfApplicable extends JavaIsoVisitor<ExecutionContext> {

        private List<String> methodsToUpdateMeta = new ArrayList<>();

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, p);

            // if this is not a private or final method, ignore
            if (!md.hasModifier(J.Modifier.Type.Private) && !md.hasModifier(J.Modifier.Type.Final)) {
                return md;
            }

            // if this is already static or is a constructor, ignore
            if (md.hasModifier(J.Modifier.Type.Static) || md.isConstructor()) {
                return md;
            }

            J.ClassDeclaration classDecl = getCursor().dropParentUntil(parent -> parent instanceof J.ClassDeclaration).getValue();

            AtomicBoolean foundInstanceAccess = FindInstanceUsagesWithinMethod.find(getCursor().getValue(), md.getBody(), classDecl, p);
            if (!foundInstanceAccess.get()) {
                md = md.withModifiers(
                            ListUtils.concat(md.getModifiers(), new J.Modifier(Tree.randomId(), Space.build(" ", emptyList()), Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList()))
                    );

                p.putMessage(md.getMethodType().toString(), true);

                return md;
            }

            return md;
        }               
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class FindInstanceUsagesWithinMethod extends JavaIsoVisitor<AtomicBoolean> {

        J.Block block;
        J.ClassDeclaration rootClass;
        ExecutionContext p;

        /**
         * @param subtree   The subtree to search.
         * @param block  A {@link J.Block} to check for instance access.
         * @param rootClass  A {@link J.ClassDeclaration} root class to check if visited items belong.
         * @return An {@link AtomicBoolean} that is true if instance access has been found.
         */
        static AtomicBoolean find(J subtree, J.Block block, J.ClassDeclaration rootClass, ExecutionContext p) {
            return new FindInstanceUsagesWithinMethod(block, rootClass, p)
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

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean hasInstanceAccess) {
            J.Identifier i = super.visitIdentifier(identifier, hasInstanceAccess);

            Variable fieldType = i.getFieldType();
            if (fieldType != null && rootClass.getType().equals(i.getFieldType().getOwner()) && !fieldType.hasFlags(Flag.Static)) {                
                hasInstanceAccess.set(true);
            }
            return i;
        }     

        @Override
        public MethodInvocation visitMethodInvocation(MethodInvocation method, AtomicBoolean hasInstanceAccess) {
            // if instance access has been found already, return immediately
            if (hasInstanceAccess.get()) {
                return method;
            }

            MethodInvocation mi = super.visitMethodInvocation(method, hasInstanceAccess);
            //if method was changed to static already, we can ignore it when checking here
            Object isTargetStatic = p.getMessage(mi.getMethodType().toString());

            if (!mi.getMethodType().hasFlags(Flag.Static) && rootClass.getType().equals(mi.getMethodType().getDeclaringType())
                && isTargetStatic == null) {
                hasInstanceAccess.set(true);
            }

            return mi;
        }
    }    
}