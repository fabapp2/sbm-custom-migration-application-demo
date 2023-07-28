/*
 * Copyright 2021 - 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acme.seclib.upgrade5to6;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.UpgradeDependencyVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.Resource;
import org.springframework.sbm.RewriteParserConfig;
import org.springframework.sbm.parsers.ProjectScanner;
import org.springframework.sbm.parsers.RewriteMavenProjectParser;
import org.springframework.sbm.parsers.RewriteProjectParser;
import org.springframework.sbm.parsers.RewriteProjectParsingResult;
import org.springframework.sbm.recipes.RewriteRecipeDiscovery;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author Fabian Krüger
 */
@SpringBootApplication
@ComponentScan(basePackageClasses = RewriteParserConfig.class)
public class Seclib5to6Migration implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Seclib5to6Migration.class);
        springApplication.setBannerMode(Banner.Mode.OFF);
        springApplication.run(args);
    }

    @Autowired
    private ProjectScanner scanner;
    @Autowired
    private RewriteMavenProjectParser parser;
    @Autowired
    private RewriteRecipeDiscovery discovery;

    @Override
    public void run(String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Path must be provided");
        }
        Path baseDir = Path.of(args[0]).toAbsolutePath().normalize();
        List<Resource> resources = scanner.scan(baseDir, Set.of("**/.git/**", "**/.idea/**"));
        ExecutionContext executionContext = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        RewriteProjectParsingResult result = parser.parse(baseDir, executionContext);
        List<SourceFile> ast = result.sourceFiles();


        ast = addDependency(baseDir, ast, executionContext);
        Recipe migrateRecipe = new Recipe() {
            @Override
            public List<Recipe> getRecipeList() {
                return List.of(
                        new Recipe() {

                            @Override
                            public String getDisplayName() {
                                return null;
                            }

                            @Override
                            public String getDescription() {
                                return null;
                            }

                            @Override
                            public TreeVisitor<?, ExecutionContext> getVisitor() {
                                return new JavaIsoVisitor<>() {

                                    private List<J.MethodDeclaration> affectedMethod;

                                    @Override
                                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                                        J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
                                        J.ClassDeclaration cd = getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).getValue();
                                        UsesType usesType = new UsesType("com.acme.seclib.SecurityCheck", true);
                                        SourceFile sourceFile = getCursor().dropParentUntil(SourceFile.class::isInstance).getValue();
                                        if (usesType.isAcceptable(sourceFile, executionContext) && usesType.visit(sourceFile, executionContext) != sourceFile) {
                                            List<Statement> statementsBefore = md.getBody().getStatements();
                                            List<Statement> statements = statementsBefore.stream()
                                                    .filter(this::isNotSecurityCheckCall)
                                                    .map(Statement.class::cast)
                                                    .toList();
                                            if (statementsBefore.size() != statements.size()) {
                                                J.Block body = md.getBody().withStatements(statements);
                                                System.out.println(body.print(getCursor()));
                                                md = md.withBody(body);
                                                System.out.println(md.print(getCursor()));
                                                if (md.getAllAnnotations().stream().noneMatch(a -> a.getSimpleName().equals("Secured"))) {
                                                    JavaTemplate javaTemplate = JavaTemplate.builder("@Secured").imports("com.acme.seclib.Secured").build();
                                                    md = javaTemplate.apply(getCursor(), md.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                                                    System.out.println(md.print(getCursor()));
                                                }
                                            }
                                        }
                                        return md;
                                    }

                                    private boolean isNotSecurityCheckCall(Statement statement) {
                                        if (J.MethodInvocation.class.isInstance(statement)) {
                                            J.MethodInvocation methodInvocation = J.MethodInvocation.class.cast(statement);
                                            if (methodInvocation.getMethodType() != null && methodInvocation.getMethodType().getDeclaringType().getFullyQualifiedName().equals("com.acme.seclib.SecurityCheck")) {
                                                return false;
                                            }
                                        }
                                        return true;
                                    }

                                    @Override
                                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                                        J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
                                        if (mi.getMethodType() != null && mi.getMethodType().getDeclaringType().getFullyQualifiedName().equals("org.acme.seclib.SecurityCheck")) {
                                            Object value = getCursor().dropParentUntil(J.MethodDeclaration.class::isInstance).getValue();
                                            List<Statement> list = ((J.MethodDeclaration) getCursor().dropParentUntil(J.MethodDeclaration.class::isInstance).getValue()).getBody().getStatements().stream()
                                                    .filter(s -> s != mi)
                                                    .toList();
                                            System.out.println(list);
                                        }
                                        return mi;
                                    }

                                    @Override
                                    public Statement visitStatement(Statement statement, ExecutionContext executionContext) {
                                        Statement s = super.visitStatement(statement, executionContext);
                                        //                        J.CompilationUnit cd = getCursor().dropParentUntil(J.CompilationUnit.class::isInstance).getValue();
                                        //                        System.out.println("Statement: (" + s.getClass().getName() + ") \n" + s.print(getCursor()));
                                        //                        if(usesType(cd)) {
                                        //                            System.out.println("Checking class " + cd.getType());
                                        //                        }
                                        return s;
                                    }

                                    //                    @Override
                                    //                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                                    //                        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                                    //                        if (usesType(cd)) {
                                    //                            return classDecl;
                                    //                        }
                                    //                        else return null;
                                    //                    }

                                    private boolean usesType(J.ClassDeclaration cd) {
                                        return !new UsesType("com.acme.business.PersonalDataService", true).visitClassDeclaration(cd, executionContext).getMarkers().findFirst(SearchResult.class).isEmpty();
                                    }
                                };
                            }
                        }
                );
            }

            @Override
            public String getDisplayName() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }
        };
        ast = runRecipe(ast, migrateRecipe, executionContext);


        RecipeRun recipeRun = migrateRecipe.run(new InMemoryLargeSourceSet(ast), executionContext);
        recipeRun.getChangeset().getAllResults().stream().forEach(r -> {
            String s = r.getAfter().printAll();
            System.out.println(s);
        });
    }


    private List<SourceFile> addDependency(Path baseDir, List<SourceFile> ast, ExecutionContext ctx) {
        UpgradeDependencyVersion recipe = new UpgradeDependencyVersion("org.acme.seclib", "seclib", "6.0.0", null, null, null);
        List<SourceFile> newAst = runRecipe(ast, recipe, ctx);
        return newAst;
    }

    @NotNull
    private List<SourceFile> runRecipe(List<SourceFile> ast, Recipe recipe, ExecutionContext ctx) {
        RecipeRun recipeRun = recipe.run(new InMemoryLargeSourceSet(ast), ctx);
        List<SourceFile> newAst = new ArrayList<>(ast);
        printResult(recipeRun);
        mergeResult(recipeRun, newAst);
        return newAst;
    }

    private void printResult(RecipeRun recipeRun) {
        recipeRun.getChangeset().getAllResults().stream().map(Result::getAfter).forEach(r -> System.out.println(r.printAll()));
    }

    private void mergeResult(RecipeRun recipeRun, List<SourceFile> newAst) {
        recipeRun.getChangeset().getAllResults().forEach(r ->{
            if(r.getAfter() != null) {
                SourceFile sourceFile = newAst.stream()
                        .filter(sf -> sf.getSourcePath().toString().equals(r.getAfter().getSourcePath().toString()))
                        .findFirst()
                        .get();
                int i = newAst.indexOf(sourceFile);
                newAst.add(i, r.getAfter());
            }
        });
    }
}
