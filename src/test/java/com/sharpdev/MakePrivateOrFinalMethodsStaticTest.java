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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
class MakePrivateOrFinalMethodsStaticTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MakePrivateOrFinalMethodsStatic());
    }

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final" })
    void callingStaticMethodChangesCallerToStaticOnNestedClass(String modifier) {
        rewriteRun(
            java(String.format("""
                    class Outer {
                        class Test {
                            static void finalMethod1() {

                            }

                            %s void finalMethod2() {
                                finalMethod1();
                            }
                        }
                    }
                    """, modifier),
                String.format("""
                    class Outer {
                        class Test {
                            static void finalMethod1() {

                            }

                            %s static void finalMethod2() {
                                finalMethod1();
                            }
                        }
                    }
                    """, modifier)           
            )
        );
    } 

    @ParameterizedTest
    @ValueSource(strings = { "public", "protected", "" })
    void callingStaticMethodIsUnchangedOnNestedClassIfDoesNotMatchSpec(String modifier) {
        rewriteRun(
            java(String.format("""
                    class Outer {
                        class Test {
                            static void finalMethod1() {

                            }

                            %s void finalMethod2() {
                                finalMethod1();
                            }
                        }
                    }
                    """, modifier)           
            )
        );
    } 

    @ParameterizedTest
    @ValueSource(strings = { "public", "protected", "" })
    void callingStaticMethodIsUnchangedIfDoesNotMatchSpec(String modifier) {
        rewriteRun(
            java(String.format("""
                    class Test {
                        static void finalMethod1() {

                        }

                        %s void finalMethod2() {
                            finalMethod1();
                        }
                    }
                    """, modifier)
            )
        );
    }   

    @ParameterizedTest
    @ValueSource(strings = { "public", "protected", "" })
    void callingStaticMethodIsUnchanged(String modifier) {
        rewriteRun(
            java(String.format("""
                    class Test {
                        static void finalMethod1() {

                        }

                        %s void finalMethod2() {
                            finalMethod1();
                        }
                    }
                    """, modifier)
            )
        );
    } 

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final" })
    void emptyMethodIsConvertedToStaticIfMatchesSpec(String modifier) {
        rewriteRun(
            java(String.format("""
                    class Test {
                        %s void finalMethod() {

                        }
                    }
                    """, modifier),
                String.format("""
                    class Test {
                        %s static void finalMethod() {

                        }
                    }
                    """, modifier)
            )
        );
    }   

    @ParameterizedTest
    @ValueSource(strings = { "public", "protected", "" })
    void emptyMethodIsUnchangedToStaticIfDoesNotMatchSpec(String modifier) {
        rewriteRun(
            java(String.format("""
                    class Test {
                        %s void finalMethod() {

                        }
                    }
                    """, modifier)
            )
        );
    }   

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final" })
    void methodOnlyUsingLocalVariableIsConvertedToStaticIfMatchesSpec(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        private String something;

                        %s void finalMethod() {
                            boolean bla = false;
                        }
                    }
                    """, modifier),
                    String.format("""                    
                    class Test {
                        private String something;

                        %s static void finalMethod() {
                            boolean bla = false;
                        }
                    }
                    """, modifier)
            )
        );
    } 

    @ParameterizedTest
    @ValueSource(strings = { "public", "protected", "" })
    void methodOnlyUsingLocalVariableIsUnchangedIfDoesNotMatchSpec(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        private String something;

                        %s void finalMethod() {
                            boolean bla = false;
                        }
                    }
                    """, modifier)
            )
        );
    } 

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void methodWithNonStaticFieldAssignmentIsUnchangedUsingThis(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        private String something;

                        %s void finalMethod() {
                            this.something = "testing";
                        }
                    }
                    """, modifier)
            )
        );
    } 

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void methodWithNonStaticFieldAssignmentIsUnchangedNotUsingThis(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        private String something;

                        %s void finalMethod() {
                            something = "testing";
                        }
                    }
                    """, modifier)
            )
        );
    }       

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final" })
    void methodWithStaticFieldAccessIsConvertedToStaticIfMatchesSpec(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        static String something;

                        %s void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """, modifier),
                String.format("""
                    class Test {
                        static String something;

                        %s static void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """, modifier)
            )
        );
    }     

    @ParameterizedTest
    @ValueSource(strings = { "public", "protected", "" })
    void methodWithStaticFieldAccessIsNotChangedIfDoesNotMatchSpec(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        static String something;

                        %s void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """, modifier)
            )
        );
    } 

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void invokeExternalMethodWithInstanceFieldIsUnchangedUsingThis(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        String something;

                        %s void finalMethod() {
                            System.out.println(this.something);
                        }
                    }
                    """, modifier)
            )
        );
    }     

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void invokeExternalMethodWithInstanceFieldIsUnchangedNotUsingThis(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        String something;

                        %s void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """, modifier)
            )
        );
    }  

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void methodWithInstanceFieldAssignmentIsUnchangedUsingThis(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        String something;

                        %s void finalMethod() {
                            this.something = "testing";
                        }
                    }
                    """, modifier)
            )
        );
    } 

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void methodWithInstanceFieldAssignmentIsUnchangedNotUsingThis(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        String something;

                        %s void finalMethod() {
                            something = "testing";
                        }
                    }
                    """, modifier)
            )
        );
    }   

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final" })
    void methodWithStaticFieldAssignmentIsConvertedToStaticIfMatchesSpec(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        static String something;

                        %s void finalMethod() {
                            something = "testing";
                        }
                    }
                    """, modifier),
                String.format("""
                    class Test {
                        static String something;

                        %s static void finalMethod() {
                            something = "testing";
                        }
                    }
                    """, modifier)
            )
        );
    } 


    @ParameterizedTest
    @ValueSource(strings = { "public", "protected", "" })
    void methodWithStaticFieldAssignmentIsUnchangedIfDoesNotMatchSpec(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        static String something;

                        %s void finalMethod() {
                            something = "testing";
                        }
                    }
                    """, modifier)
            )
        );
    }       

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void methodWithInstanceFieldAssignmentOperationIsUnchangedNotUsingThis(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        Integer something = 0;

                        %s void finalMethod() {
                            something += 1;
                        }
                    }
                    """, modifier)
            )
        );
    }  
    
    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void methodWithInstanceFieldAssignmentOperationIsUnchangedUsingThis(String modifier) {
        rewriteRun(
            java(String.format("""
                    class Test {
                        Integer something = 0;

                        %s void finalMethod() {
                            this.something += 1;
                        }
                    }
                    """, modifier)
            )
        );
    }             

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void methodWithInstanceFieldUnaryIsUnchangedNotUsingThis(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        Integer something = 0;

                        %s void finalMethod() {
                            something++;
                        }
                    }
                    """, modifier)
            )
        );
    }  
    
    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void methodWithInstanceFieldUnaryIsUnchangedUsingThis(String modifier) {
        rewriteRun(
            java(String.format("""
                    class Test {
                        Integer something = 0;

                        %s void finalMethod() {
                            this.something++;
                        }
                    }
                    """, modifier)
            )
        );
    }      

    @ParameterizedTest
    @ValueSource(strings = { "private", "public", "protected", "" })
    void constructorIsNeverChanged(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    class Test {
                        Integer something = 0;

                        %s Test() {
                            something++;
                        }
                    }
                    """, modifier)
            )
        );
    }       

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void LambdaUsingInstanceFieldDoesntChange(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    import java.util.ArrayList;
                    import java.util.List;
                    import java.util.stream.Collectors;
                    
                    public class A {
                        static List<Integer> a = new ArrayList<>();
                        Integer something = 0;
                    
                        %s void foo() {
                            a = a.stream()
                                .map(it -> it + something)
                                .collect(Collectors.toList());
                        }
                    }
                    """, modifier)
            )
        );
    } 

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final" })
    void LambdaUsingStaticFieldIsConvertedToStaticIfMatchesSpec(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    import java.util.ArrayList;
                    import java.util.List;
                    import java.util.stream.Collectors;
                    
                    public class A {
                        static List<Integer> a = new ArrayList<>();
                        static Integer something = 0;
                    
                        %s void foo() {
                            a = a.stream()
                                .map(it -> it + something)
                                .collect(Collectors.toList());
                        }
                    }
                    """, modifier),
                String.format("""                    
                    import java.util.ArrayList;
                    import java.util.List;
                    import java.util.stream.Collectors;
                    
                    public class A {
                        static List<Integer> a = new ArrayList<>();
                        static Integer something = 0;
                    
                        %s static void foo() {
                            a = a.stream()
                                    .map(it -> it + something)
                                    .collect(Collectors.toList());
                        }
                    }
                    """, modifier)                    
            )
        );
    }     

    @ParameterizedTest
    @ValueSource(strings = { "public", "protected", "" })
    void LambdaUsingStaticFieldIsUnchangedIfDoesNotMatchSpec(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    import java.util.ArrayList;
                    import java.util.List;
                    import java.util.stream.Collectors;
                    
                    public class A {
                        static List<Integer> a = new ArrayList<>();
                        static Integer something = 0;
                    
                        %s void foo() {
                            a = a.stream()
                                .map(it -> it + something)
                                .collect(Collectors.toList());
                        }
                    }
                    """, modifier)                   
            )
        );
    }         

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final", "public", "protected", "" })
    void ReturnStatementUsingInstanceFieldIsUnchanged(String modifier) {
        rewriteRun(
            java(String.format("""                    
                    import java.util.ArrayList;
                    import java.util.List;
                    import java.util.stream.Collectors;
                    
                    public class A {
                    
                        List<Integer> a = new ArrayList<>();
                    
                        %s List<Integer> foo() {
                            return this.a;
                        }
                    }
                    """, modifier)
            )
        );
    }     

    @ParameterizedTest
    @ValueSource(strings = { "final", "private", "private final" })
    void returnStatementUsingStaticFieldIsConvertedToStaticIfMatchesSpec(String modifier) {
        rewriteRun(
            java(String.format("""
                    import java.util.ArrayList;
                    import java.util.List;
                    import java.util.stream.Collectors;
                    
                    public class A {
                    
                        static List<Integer> a = new ArrayList<>();
                    
                        %s List<Integer> foo() {
                            return a;
                        }
                    }
                    """, modifier),
                String.format("""         
                        import java.util.ArrayList;
                        import java.util.List;
                        import java.util.stream.Collectors;
                        
                        public class A {
                        
                            static List<Integer> a = new ArrayList<>();
                        
                            %s static List<Integer> foo() {
                                return a;
                            }
                        }
                    """, modifier)
            )
        );
    }      

    @ParameterizedTest
    @ValueSource(strings = { "public", "protected", "" })
    void returnStatementUsingStaticFieldIsUnchangedIfDoesNotMatchSpec(String modifier) {
        rewriteRun(
            java(String.format("""
                    import java.util.ArrayList;
                    import java.util.List;
                    import java.util.stream.Collectors;
                    
                    public class A {
                    
                        static List<Integer> a = new ArrayList<>();
                    
                        %s List<Integer> foo() {
                            return a;
                        }
                    }
                    """, modifier)
            )
        );
    }       

    @Test
    void writeObjectSerializableIsIgnored() {
        rewriteRun(
            java("""                    
                    import java.io.IOException;
                    import java.io.ObjectOutputStream;
                    import java.io.Serializable;
                    
                    public class MyClass implements Serializable {
                        
                        private static final long serialVersionUID = 123456789L;
                        
                        private String myTransientField;
                    
                        private void writeObject(ObjectOutputStream out) throws IOException {
                            out.defaultWriteObject();
                            out.writeObject(myTransientField);
                        }
                    }                
                    """                   
            )
        );
    } 

    @Test
    void readObjectSerializableIsIgnored() {
        rewriteRun(
            java("""                    
                    import java.io.IOException;
                    import java.io.ObjectInputStream;
                    import java.io.Serializable;
                    
                    public class MyClass implements Serializable {
                        
                        private static final long serialVersionUID = 123456789L;
                        
                        private String myTransientField;
                    
                        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
                            in.defaultReadObject();
                            myTransientField = (String) in.readObject();
                        }
                    }                
                    """                   
            )
        );
    }     

    @Test
    void readObjectNoDataSerializableIsIgnored() {
        rewriteRun(
            java("""                    
                    import java.io.IOException;
                    import java.io.ObjectInputStream;
                    import java.io.Serializable;
                    
                    public class MyClass implements Serializable {
                        
                        private static final long serialVersionUID = 123456789L;
                        
                        private Integer myField;
                    
                        private void readObjectNoData() throws IOException, ClassNotFoundException {
                            myField = 0;
                        }                    
                    }                
                    """                   
            )
        );
    }     
}