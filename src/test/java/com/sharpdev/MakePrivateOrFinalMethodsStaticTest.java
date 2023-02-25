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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
class MakePrivateOrFinalMethodsStaticTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MakePrivateOrFinalMethodsStatic());
    }

    @Test
    void finalCallingStaticMethodChangesCallerToStaticOnNestedClass() {
        rewriteRun(
            java("""
                    class Outer {
                        class Test {
                            static void finalMethod1() {

                            }

                            final void finalMethod2() {
                                finalMethod1();
                            }
                        }
                    }
                    """,
                """
                    class Outer {
                        class Test {
                            static void finalMethod1() {

                            }

                            final static void finalMethod2() {
                                finalMethod1();
                            }
                        }
                    }
                    """                    
            )
        );
    } 

    @Test
    void finalCallingStaticMethodChangesCallerToStatic() {
        rewriteRun(
            java("""
                    class Test {
                        static void finalMethod1() {

                        }

                        final void finalMethod2() {
                            finalMethod1();
                        }
                    }
                    """,
                """
                    class Test {
                        static void finalMethod1() {

                        }

                        final static void finalMethod2() {
                            finalMethod1();
                        }
                    }
                    """                    
            )
        );
    } 

    @Test
    void privateCallingStaticMethodChangesCallerToStatic() {
        rewriteRun(
            java("""
                    class Test {
                        static void finalMethod1() {

                        }

                        private void finalMethod2() {
                            finalMethod1();
                        }
                    }
                    """,
                """
                    class Test {
                        static void finalMethod1() {

                        }

                        private static void finalMethod2() {
                            finalMethod1();
                        }
                    }
                    """                    
            )
        );
    }     

    @Test
    void publicCallingStaticMethodDoesNotChange() {
        rewriteRun(
            java("""
                    class Test {
                        static void finalMethod1() {

                        }

                        public void finalMethod2() {
                            finalMethod1();
                        }
                    }
                    """                   
            )
        );
    } 

    @Test
    void protectedCallingStaticMethodDoesNotChange() {
        rewriteRun(
            java("""
                    class Test {
                        static void finalMethod1() {

                        }

                        protected void finalMethod2() {
                            finalMethod1();
                        }
                    }
                    """                   
            )
        );
    } 

    @Test
    void emptyPrivateMethodIsConvertedToStatic() {
        rewriteRun(
            java("""
                    class Test {
                        private void finalMethod() {

                        }
                    }
                    """,
                """
                    class Test {
                        private static void finalMethod() {

                        }
                    }
                    """
            )
        );
    } 

    @Test
    void emptyFinalMethodIsConvertedToStatic() {
        rewriteRun(
            java("""
                    class Test {
                        final void finalMethod() {

                        }
                    }
                    """,
                """
                    class Test {
                        final static void finalMethod() {

                        }
                    }
                    """
            )
        );
    }   

    @Test
    void emptyPublicMethodIsNotChanged() {
        rewriteRun(
            java("""
                    class Test {
                        public void finalMethod() {

                        }
                    }
                    """
            )
        );
    }          

    @Test
    void emptyProtectedMethodIsNotChanged() {
        rewriteRun(
            java("""
                    class Test {
                        protected void finalMethod() {

                        }
                    }
                    """
            )
        );
    }    

    @Test
    void publicWithNonStaticFieldAssignmentIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        private String something;

                        public void finalMethod() {
                            this.something = "testing";
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void publicWithNonStaticFieldAssignmentIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        private String something;

                        public void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void finalWithNonStaticFieldAssignmentIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        private String something;

                        final void finalMethod() {
                            this.something = "testing";
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void finalWithNonStaticFieldAssignmentIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        private String something;

                        final void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void privateWithNonStaticFieldAssignmentIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        private String something;

                        private void finalMethod() {
                            this.something = "testing";
                        }
                    }
                    """
            )
        );
    }     

    @Test
    void privateWithNonStaticFieldAssignmentIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        private String something;

                        private void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    }     

    @Test
    void protectedWithNonStaticFieldAssignmentIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        private String something;

                        protected void finalMethod() {
                            this.something = "testing";
                        }
                    }
                    """
            )
        );
    }     

    @Test
    void protectedWithNonStaticFieldAssignmentIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        private String something;

                        protected void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    }     

    @Test
    void finalWithStaticFieldAccessIsConvertedToStatic() {
        rewriteRun(
            java("""                    
                    class Test {
                        static String something;

                        final void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """,
                """
                    class Test {
                        static String something;

                        final static void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void privateWithStaticFieldAccessIsConvertedToStatic() {
        rewriteRun(
            java("""                    
                    class Test {
                        static String something;

                        private void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """,
                """
                    class Test {
                        static String something;

                        private static void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """
            )
        );
    }     

    @Test
    void publicWithStaticFieldAccessIsNotChanged() {
        rewriteRun(
            java("""                    
                    class Test {
                        static String something;

                        public void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void protectedWithStaticFieldAccessIsNotChanged() {
        rewriteRun(
            java("""                    
                    class Test {
                        static String something;

                        protected void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void finalInvokeExternalMethodWithInstanceFieldIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        final void finalMethod() {
                            System.out.println(this.something);
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void finalInvokeExternalMethodWithInstanceFieldIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        final void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void privateInvokeExternalMethodWithInstanceFieldIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        private void finalMethod() {
                            System.out.println(this.something);
                        }
                    }
                    """
            )
        );
    }         

    @Test
    void privateInvokeExternalMethodWithInstanceFieldIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        private void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void publicInvokeExternalMethodWithInstanceFieldIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        public void finalMethod() {
                            System.out.println(this.something);
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void publicInvokeExternalMethodWithInstanceFieldIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        public void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void protectedInvokeExternalMethodWithInstanceFieldIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        protected void finalMethod() {
                            System.out.println(this.something);
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void protectedInvokeExternalMethodWithInstanceFieldIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        protected void finalMethod() {
                            System.out.println(something);
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void finalWithInstanceFieldAssignmentIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        final void finalMethod() {
                            this.something = "testing";
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void finalWithInstanceFieldAssignmentIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        final void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void privateWithInstanceFieldAssignmentIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        private void finalMethod() {
                            this.something = "testing";
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void privateWithInstanceFieldAssignmentIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        private void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    }     

    @Test
    void publicWithInstanceFieldAssignmentIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        public void finalMethod() {
                            this.something = "testing";
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void publicWithInstanceFieldAssignmentIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        String something;

                        public void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void finalWithStaticFieldAssignmentIsConvertedToStatic() {
        rewriteRun(
            java("""                    
                    class Test {
                        static String something;

                        final void finalMethod() {
                            something = "testing";
                        }
                    }
                    """,
                """
                    class Test {
                        static String something;

                        final static void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    } 

    @Test
    void privateWithStaticFieldAssignmentIsConvertedToStatic() {
        rewriteRun(
            java("""                    
                    class Test {
                        static String something;

                        private void finalMethod() {
                            something = "testing";
                        }
                    }
                    """,
                """
                    class Test {
                        static String something;

                        private static void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void publicWithStaticFieldAssignmentIsUnchanged() {
        rewriteRun(
            java("""                    
                    class Test {
                        static String something;

                        public void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void protectedWithStaticFieldAssignmentIsUnchanged() {
        rewriteRun(
            java("""                    
                    class Test {
                        static String something;

                        protected void finalMethod() {
                            something = "testing";
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void privateWithInstanceFieldAssignmentOperationIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        private void finalMethod() {
                            something += 1;
                        }
                    }
                    """
            )
        );
    }  
    
    @Test
    void privateWithInstanceFieldAssignmentOperationIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        private void finalMethod() {
                            this.something += 1;
                        }
                    }
                    """
            )
        );
    }            
    
    @Test
    void finalWithInstanceFieldAssignmentOperationIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        final void finalMethod() {
                            something += 1;
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void finalWithInstanceFieldAssignmentOperationIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        final void finalMethod() {
                            this.something += 1;
                        }
                    }
                    """
            )
        );
    }     
    @Test
    void publicWithInstanceFieldAssignmentOperationIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        public void finalMethod() {
                            this.something += 1;
                        }
                    }
                    """
            )
        );
    }     
    
    @Test
    void publicWithInstanceFieldAssignmentOperationIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        public void finalMethod() {
                            something += 1;
                        }
                    }
                    """
            )
        );
    }  
        
    @Test
    void protectedWithInstanceFieldAssignmentOperationIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        protected void finalMethod() {
                            this.something += 1;
                        }
                    }
                    """
            )
        );
    }     
    
    @Test
    void protectedWithInstanceFieldAssignmentOperationIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        protected void finalMethod() {
                            something += 1;
                        }
                    }
                    """
            )
        );
    }  

    @Test
    void finalWithInstanceFieldUnaryIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        final void finalMethod() {
                            this.something++;
                        }
                    }
                    """
            )
        );
    }  
    
    @Test
    void finalWithInstanceFieldUnaryIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        final void finalMethod() {
                            something++;
                        }
                    }
                    """
            )
        );
    }             
    
    @Test
    void privateWithInstanceFieldUnaryIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        private void finalMethod() {
                            this.something++;
                        }
                    }
                    """
            )
        );
    }  
    
    @Test
    void privateWithInstanceFieldUnaryIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        private void finalMethod() {
                            something++;
                        }
                    }
                    """
            )
        );
    }   
        
    @Test
    void publicWithInstanceFieldUnaryIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        public void finalMethod() {
                            this.something++;
                        }
                    }
                    """
            )
        );
    }  
    
    @Test
    void publicWithInstanceFieldUnaryIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        public void finalMethod() {
                            something++;
                        }
                    }
                    """
            )
        );
    }   
        
    @Test
    void protectedWithInstanceFieldUnaryIsUnchangedUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        protected void finalMethod() {
                            this.something++;
                        }
                    }
                    """
            )
        );
    }  
    
    @Test
    void protectedWithInstanceFieldUnaryIsUnchangedNotUsingThis() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        protected void finalMethod() {
                            something++;
                        }
                    }
                    """
            )
        );
    }   

    @Test
    void constructorIsNeverChanged() {
        rewriteRun(
            java("""                    
                    class Test {
                        Integer something = 0;

                        private Test() {
                            something++;
                        }
                    }
                    """
            )
        );
    }       
}
