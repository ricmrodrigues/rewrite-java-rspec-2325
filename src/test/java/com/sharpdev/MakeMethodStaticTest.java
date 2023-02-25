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
class MakeMethodStaticTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MakeMethodStatic());
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
}
