/*
 * Copyright 2021 the original author or authors.
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

import java.util.Collections;
import java.util.Set;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.java.tree.JavaType.Variable;
import org.openrewrite.marker.Markers;

@Incubating(since = "7.0.0")
public class MakePrivateOrFinalMethodsStatic extends Recipe {

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
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {   
            
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if ((method.hasModifier(J.Modifier.Type.Private) || method.hasModifier(J.Modifier.Type.Final)) &&
                        !method.hasModifier(J.Modifier.Type.Static) &&
                        method.getTypeParameters() == null &&
                        !method.isConstructor() &&
                        method.getBody() != null && 
                        !containsInstanceData(method.getBody(), classDecl)) {
                            
                    return method.withModifiers(ListUtils.concat(method.getModifiers(), new J.Modifier(Tree.randomId(), Space.build(" ", Collections.emptyList()), Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList())));
                }
                return super.visitMethodDeclaration(method, executionContext);
            }

            private boolean containsInstanceData(J.Block body, J.ClassDeclaration classDecl) {
                for (Statement statement : body.getStatements()) {
                    if (statement instanceof J.FieldAccess) {                        
                        J.FieldAccess field = (J.FieldAccess) statement;
                        J.Identifier identifier = (J.Identifier) field.getTarget();
                        Variable variable = identifier.getFieldType();

                        if (TypeUtils.isOfClassType(variable.getOwner(), classDecl.getName().getSimpleName())) {
                            return true;
                        }                 
                        //OLD way       
                        // J.FieldAccess field = (J.FieldAccess) statement;
                        // J.Identifier identifier = (J.Identifier) field.getTarget();
                        // if (identifier.getSimpleName().equals(field.getClass().getSimpleName())) {
                        //     return true;
                        // }
                    }
                    if (statement instanceof J.Assignment) {                        
                        J.Assignment assignment = (J.Assignment) statement;
                        org.openrewrite.java.tree.Expression expression = assignment.getVariable();
                        if (checkFieldAccess(expression, classDecl)) {
                            return true;
                        }
                    }    
                    if (statement instanceof J.MethodInvocation) {
                        J.MethodInvocation methodInvocation = (J.MethodInvocation) statement;
                        if (checkMethodInvocation(methodInvocation, classDecl)) {
                            return true;
                        }
                    }
                }

                return false;
            }            

            private boolean checkFieldAccess(org.openrewrite.java.tree.Expression expression, J.ClassDeclaration classDecl) {
                //TODO we need a better way here - is this safe as a check? how do we know it's not static?
                if (expression instanceof J.FieldAccess) {                    
                    J.FieldAccess field = (J.FieldAccess) expression;
                    J.Identifier identifier = (J.Identifier) field.getTarget();
                    Variable variable = identifier.getFieldType();
                    if (TypeUtils.isOfClassType(variable.getOwner(), classDecl.getName().getSimpleName())) {
                        return true;
                    }
                }                

                return false;
            }

            private boolean checkMethodInvocation(J.MethodInvocation methodInvocation, J.ClassDeclaration classDecl) {
                for (org.openrewrite.java.tree.Expression expression : methodInvocation.getArguments()) {
                    if (expression instanceof J.MethodInvocation) {
                        if (checkMethodInvocation((J.MethodInvocation) expression, classDecl)) {
                            return true;
                        }
                    }
                    if (expression instanceof J.Identifier) {
                        J.Identifier identifier = (J.Identifier) expression;
                        Variable variable = identifier.getFieldType();
                        if (TypeUtils.isOfClassType(variable.getOwner(), classDecl.getName().getSimpleName())) {
                            return true;
                        }                        
                        return true;
                    }                            
                }

                return false;
            }
        };
    }
}
