/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.auto.value.processor;

import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;
import static com.google.common.truth.TruthJUnit.assume;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class AutoBuilderCompilationTest {
  private static final JavaFileObject EXPECTED_SIMPLE_OUTPUT =
      JavaFileObjects.forSourceLines(
          "foo.bar.AutoBuilder_Baz_Builder",
          "package foo.bar;",
          "",
          GeneratedImport.importGeneratedAnnotationType(),
          "",
          "@Generated(\"" + AutoBuilderProcessor.class.getName() + "\")",
          "class AutoBuilder_Baz_Builder implements Baz.Builder {",
          "  private Integer anInt;",
          "  private String aString;",
          "",
          "  AutoBuilder_Baz_Builder() {}",
          "",
          "  @Override public Baz.Builder setAnInt(int anInt) {",
          "    this.anInt = anInt;",
          "    return this;",
          "  }",
          "",
          "  @Override public Baz.Builder setAString(String aString) {",
          "    if (aString == null) {",
          "      throw new NullPointerException(\"Null aString\");",
          "    }",
          "    this.aString = aString;",
          "    return this;",
          "  }",
          "",
          "  @Override",
          "  public Baz build() {",
          "    String missing = \"\";",
          "    if (this.anInt == null) {",
          "      missing += \" anInt\";",
          "    }",
          "    if (this.aString == null) {",
          "      missing += \" aString\";",
          "    }",
          "    if (!missing.isEmpty()) {",
          "      throw new IllegalStateException(\"Missing required properties:\" + missing);",
          "    }",
          "    return new Baz(",
          "        this.anInt,",
          "        this.aString);",
          "  }",
          "}");

  @Test
  public void simpleSuccess() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public class Baz {",
            "  private final int anInt;",
            "  private final String aString;",
            "",
            "  public Baz(int anInt, String aString) {",
            "    this.anInt = anInt;",
            "    this.aString = aString;",
            "  }",
            "",
            "  public int anInt() {",
            "    return anInt;",
            "  }",
            "",
            "  public String aString() {",
            "    return aString;",
            "  }",
            "",
            "  public static Builder builder() {",
            "    return new AutoBuilder_Baz_Builder();",
            "  }",
            "",
            "  @AutoBuilder",
            "  public interface Builder {",
            "    Builder setAnInt(int x);",
            "    Builder setAString(String x);",
            "    Baz build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation)
        .generatedSourceFile("foo.bar.AutoBuilder_Baz_Builder")
        .hasSourceEquivalentTo(EXPECTED_SIMPLE_OUTPUT);
  }

  @Test
  public void simpleRecord() {
    double version = Double.parseDouble(JAVA_SPECIFICATION_VERSION.value());
    assume().that(version).isAtLeast(16.0);
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public record Baz(int anInt, String aString) {",
            "  public static Builder builder() {",
            "    return new AutoBuilder_Baz_Builder();",
            "  }",
            "",
            "  @AutoBuilder",
            "  public interface Builder {",
            "    Builder setAnInt(int x);",
            "    Builder setAString(String x);",
            "    Baz build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation)
        .generatedSourceFile("foo.bar.AutoBuilder_Baz_Builder")
        .hasSourceEquivalentTo(EXPECTED_SIMPLE_OUTPUT);
  }

  @Test
  public void autoBuilderOnEnum() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "@AutoBuilder",
            "public enum Baz {",
            "  ZIG, ZAG, DUSTIN,",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining(
            "[AutoBuilderWrongType] @AutoBuilder only applies to classes and interfaces")
        .inFile(javaFileObject)
        .onLineContaining("enum Baz");
  }

  @Test
  public void autoBuilderPrivate() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public class Baz {",
            "  @AutoBuilder",
            "  private interface Builder {",
            "    Baz build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining("[AutoBuilderPrivate] @AutoBuilder class must not be private")
        .inFile(javaFileObject)
        .onLineContaining("interface Builder");
  }

  @Test
  public void autoBuilderNestedInPrivate() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public class Baz {",
            "  private static class Private {",
            "    @AutoBuilder",
            "    public interface Builder {",
            "      Baz build();",
            "    }",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining(
            "[AutoBuilderInPrivate] @AutoBuilder class must not be nested in a private class")
        .inFile(javaFileObject)
        .onLineContaining("interface Builder");
  }

  @Test
  public void autoBuilderInner() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public class Baz {",
            "  @AutoBuilder",
            "  abstract class Builder {",
            "    abstract Baz build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining("[AutoBuilderInner] Nested @AutoBuilder class must be static")
        .inFile(javaFileObject)
        .onLineContaining("class Builder");
  }

  @Test
  public void innerConstructor() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public class Baz {",
            "  class Inner {}",
            "",
            "  @AutoBuilder(ofClass = Inner.class)",
            "  interface Builder {",
            "    abstract Inner build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining("[AutoBuilderInner] Nested @AutoBuilder ofClass class must be static")
        .inFile(javaFileObject)
        .onLineContaining("interface Builder");
  }

  @Test
  public void noVisibleConstructor() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public class Baz {",
            "  @AutoBuilder(ofClass = System.class)",
            "  interface Builder {",
            "    abstract System build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining("[AutoBuilderNoVisible] No visible constructor for java.lang.System")
        .inFile(javaFileObject)
        .onLineContaining("interface Builder");
  }

  @Test
  public void noVisibleMethod() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public class Baz {",
            "  private static Baz of() {",
            "    return new Baz();",
            "  }",
            "",
            "  @AutoBuilder(callMethod = \"of\")",
            "  interface Builder {",
            "    abstract Baz build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining(
            "[AutoBuilderNoVisible] No visible static method named \"of\" for foo.bar.Baz")
        .inFile(javaFileObject)
        .onLineContaining("interface Builder");
  }

  @Test
  public void methodNotStatic() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public class Baz {",
            "  Baz of() {",
            "    return this;",
            "  }",
            "",
            "  @AutoBuilder(callMethod = \"of\")",
            "  interface Builder {",
            "    abstract Baz build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining(
            "[AutoBuilderNoVisible] No visible static method named \"of\" for foo.bar.Baz")
        .inFile(javaFileObject)
        .onLineContaining("interface Builder");
  }

  @Test
  public void noMatchingConstructor() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public class Baz {",
            "  public Baz(int notMe) {}",
            "",
            "  public Baz(String notMeEither) {}",
            "",
            "  @AutoBuilder",
            "  interface Builder {",
            "    Builder setBuh(String x);",
            "    abstract Baz build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining(
            "[AutoBuilderNoMatch] Property names do not correspond to the parameter names of any"
                + " constructor:\n"
                + "    Baz(int notMe)\n"
                + "    Baz(java.lang.String notMeEither)")
        .inFile(javaFileObject)
        .onLineContaining("interface Builder");
  }

  @Test
  public void twoMatchingConstructors() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public class Baz {",
            "  public Baz() {}",
            "",
            "  public Baz(int buh) {}",
            "",
            "  public Baz(String buh) {}",
            "",
            "  @AutoBuilder",
            "  interface Builder {",
            "    Builder setBuh(String x);",
            "    abstract Baz build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining(
            "[AutoBuilderAmbiguous] Property names correspond to more than one constructor:\n"
                + "    Baz(int buh)\n"
                + "    Baz(java.lang.String buh)")
        .inFile(javaFileObject)
        .onLineContaining("interface Builder");
  }

  @Test
  public void constructInterface() {
    JavaFileObject javaFileObject =
        JavaFileObjects.forSourceLines(
            "foo.bar.Baz",
            "package foo.bar;",
            "",
            "import com.google.auto.value.AutoBuilder;",
            "",
            "public interface Baz {",
            "  @AutoBuilder",
            "  interface Builder {",
            "    abstract Baz build();",
            "  }",
            "}");
    Compilation compilation =
        javac()
            .withProcessors(new AutoBuilderProcessor())
            .withOptions("-Acom.google.auto.value.AutoBuilderIsUnstable")
            .compile(javaFileObject);
    assertThat(compilation).failed();
    assertThat(compilation)
        .hadErrorContaining(
            "[AutoBuilderEnclosing] @AutoBuilder must specify ofClass=Something.class or it must"
                + " be nested inside the class to be built; actually nested inside interface"
                + " foo.bar.Baz")
        .inFile(javaFileObject)
        .onLineContaining("interface Builder");
  }
}