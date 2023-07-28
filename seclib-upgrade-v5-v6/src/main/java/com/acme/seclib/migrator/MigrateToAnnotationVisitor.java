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
package com.acme.seclib.migrator;

import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Comparator;
import java.util.List;

/**
 * @author Fabian Krüger
 */
public class MigrateToAnnotationVisitor extends JavaIsoVisitor<ExecutionContext> {


    public static final String SECURITY_CHECK = "com.acme.seclib.SecurityCheck";
    public static final String SECURED_ANNOTATION = "com.acme.seclib.Secured";


    private List<J.MethodDeclaration> affectedMethod;

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
        J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
        J.ClassDeclaration cd = getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).getValue();
        UsesType usesType = new UsesType(SECURITY_CHECK);
        SourceFile sourceFile = getCursor().dropParentUntil(SourceFile.class::isInstance).getValue();
        if (usesType.isAcceptable(sourceFile, executionContext) && usesType.visit(sourceFile, executionContext) != sourceFile) {
            List<Statement> statementsBefore = md.getBody().getStatements();
            List<Statement> statements = statementsBefore.stream()
                    .filter(this::isNotSecurityCheckCall)
                    .map(Statement.class::cast)
                    .toList();
            if (statementsBefore.size() != statements.size()) {
                J.Block body = md.getBody().withStatements(statements);
                md = md.withBody(body);
                if (md.getAllAnnotations().stream().noneMatch(a -> a.getSimpleName().equals("Secured"))) {

                    this.maybeAddImport(SECURED_ANNOTATION, null, false);
                    JavaTemplate javaTemplate = JavaTemplate.builder(() -> getCursor(), "@Secured").imports(SECURED_ANNOTATION).build();
                    md = md.withTemplate(javaTemplate, md.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                    maybeRemoveImport(SECURITY_CHECK);
                }
            }
        }
        return md;
    }

    private boolean isNotSecurityCheckCall(Statement statement) {
        if (J.MethodInvocation.class.isInstance(statement)) {
            J.MethodInvocation methodInvocation = J.MethodInvocation.class.cast(statement);
            if (methodInvocation.getMethodType() != null && methodInvocation.getMethodType().getDeclaringType().getFullyQualifiedName().equals(SECURITY_CHECK)) {
                return false;
            }
        }
        return true;
    }
}