// /*
//  * Copyright 2020 the original author or authors.
//  * <p>
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  * <p>
//  * https://www.apache.org/licenses/LICENSE-2.0
//  * <p>
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
// package com.sharpdev;

// import org.openrewrite.Cursor;
// import org.openrewrite.ExecutionContext;
// import org.openrewrite.Recipe;
// import org.openrewrite.Tree;
// import org.openrewrite.internal.ListUtils;
// import org.openrewrite.java.JavaIsoVisitor;
// import org.openrewrite.java.tree.J;
// import org.openrewrite.java.tree.JavaType;
// import org.openrewrite.java.tree.Space;
// import org.openrewrite.marker.Markers;

// import java.util.Collections;
// import java.util.HashSet;
// import java.util.Set;

// public class MakePrivateFinalMethodsStatic2 extends Recipe {
//     @Override
//     public String getDisplayName() {
//         return "Make private or final methods static if they do not access any instance data";
//     }

//     @Override
//     public String getDescription() {
//         return "Identifies private or final methods that do not access any instance data and converts them to static methods.";
//     }

//     @Override
//     protected JavaIsoVisitor<ExecutionContext> getVisitor() {
//         return new JavaIsoVisitor<ExecutionContext>() {
//             @Override
//             public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
//                 J.MethodDeclaration updatedMethod = super.visitMethodDeclaration(method, executionContext);

//                 Set<String> instanceFields = new HashSet<>();
//                 Cursor parent = getCursor().getParent();
//                 while (parent != null) {
//                     J j = parent.getValue();
//                     if (j instanceof J.FieldAccess) {
//                         J.FieldAccess fieldAccess = (J.FieldAccess) j;
//                         if (fieldAccess.getTarget() instanceof J.Identifier) {
//                             J.Identifier identifier = (J.Identifier) fieldAccess.getTarget();
//                             if (identifier.getType() != null) {
//                                 JavaType type = identifier.getType();
//                                 if (type instanceof JavaType.Method) {
//                                     JavaType.Method methodType = (JavaType.Method) type;
//                                     JavaType.FullyQualified declaringClass = methodType.getDeclaringType();
//                                     instanceFields.add(declaringClass.getFullyQualifiedName() + "." + identifier.getSimpleName());
//                                 }
//                             }
//                         }
//                     } else if (j instanceof J.VariableDeclarations) {
//                         J.VariableDeclarations varDecls = (J.VariableDeclarations) j;
//                         if (varDecls.getTypeExpression() != null) {
//                             JavaType type = varDecls.getTypeExpression().getType();
//                             if (type instanceof JavaType.Class) {
//                                 JavaType.Class classType = (JavaType.Class) type;
//                                 String fullyQualifiedName = classType.getFullyQualifiedName();
//                                 varDecls.getVariables().forEach(v -> instanceFields.add(fullyQualifiedName + "." + v.getSimpleName()));
//                             }
//                         }
//                     } else if (j instanceof J.ClassDeclaration) {
//                         J.ClassDeclaration classDecl = (J.ClassDeclaration) j;
//                         instanceFields.add(classDecl.getSimpleName());
//                     }
//                     parent = parent.getParent();
//                 }

//                 if ((updatedMethod.hasModifier(J.Modifier.Type.Private) || updatedMethod.hasModifier(J.Modifier.Type.Final)) &&
//                         updatedMethod.getTypeParameters().isEmpty() &&
//                         !updatedMethod.isConstructor() &&
//                         updatedMethod.getBody() != null &&
//                         !Scope.closestEnclosing(updatedMethod.getBody(), J.ClassDeclaration.class).getFieldsInScope().stream()
//                                 .anyMatch(field -> instanceFields.contains(field.getDeclaringType().getFullyQualifiedName() + "." + field.getSimpleName()))) {
//                                     updatedMethod = updatedMethod.withModifiers(ListUtils.concat(updatedMethod.getModifiers(), new J.Modifier(Tree.randomId(), Space.build(" ", Collections.emptyList()), Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList())));
//                 }                

//                 return updatedMethod;
//             }
          
//         };
//     }
// }