package com.sharpdev;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MakePrivateOrFinalMethodsStaticTest implements RewriteTest {

    //Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    //In this case, the recipe and the parser are common. See below, on how the defaults can be overridden
    //per test.
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MakePrivateOrFinalMethodsStatic())
            .parser(JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(true));
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
    void finalWithNonStaticFieldAssignmentIsUnchanged() {
        rewriteRun(
            java("""                    
                    class Test {
                        private String something;

                        final void finalMethod() {
                            this.something = \"testing\";
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
    void finalWithNonStaticFieldAccessIsUnchanged() {
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
    void finalWithStaticFieldAssignmentIsConvertedToStatic() {
        rewriteRun(
            java("""                    
                    class Test {
                        static String something;

                        final void finalMethod() {
                            something = \"testing\";
                        }
                    }
                    """,
                """
                    class Test {
                        static String something;

                        final static void finalMethod() {
                            something = \"testing\";
                        }
                    }
                    """
            )
        );
    }            
}
