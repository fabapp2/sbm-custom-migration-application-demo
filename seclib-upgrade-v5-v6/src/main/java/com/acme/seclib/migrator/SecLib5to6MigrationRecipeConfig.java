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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.sbm.build.migration.conditions.AnyDeclaredDependencyExistMatchingRegex;
import org.springframework.sbm.engine.recipe.*;
import org.springframework.sbm.java.migration.conditions.HasAnyTypeReference;
import org.springframework.sbm.support.openrewrite.GenericOpenRewriteRecipe;

import java.util.List;

/**
 * @author Fabian Krüger
 */
@Configuration
public class SecLib5to6MigrationRecipeConfig {

    @Bean
    Recipe secLib5to6MigrationRecipe(RewriteRecipeLoader rewriteRecipeLoader, RewriteRecipeRunner rewriteRecipeRunner, RewriteMigrationResultMerger rewriteResultMerger) {
        return Recipe.builder()
                .name("migrate-seclib-5-to-6")
                .description("Add dependency and migrate deprecated code")
                .condition(Condition.TRUE)
                .action(
                        OpenRewriteDeclarativeRecipeAdapter.builder()
                                .rewriteRecipeLoader(rewriteRecipeLoader)
                                .rewriteRecipeRunner(rewriteRecipeRunner)
                                .openRewriteRecipe(
                                    """
                                    type: specs.openrewrite.org/v1beta/recipe
                                    name: com.acme.migration.UpdateSecLib5To6
                                    displayName: Upgrade SecLib from v5 to v6
                                    description: 'Upgrades SecLib from v5 to v6.'
                                    recipeList:
                                      - org.openrewrite.maven.UpgradeDependencyVersion:
                                          groupId: com.acme.seclib
                                          artifactId: seclib-core
                                          newVersion: 6.0.0               
                                    """
                                )
                                .condition(AnyDeclaredDependencyExistMatchingRegex.builder()
                                        .dependencies(List.of("com.acme.seclib:seclib-core:5.0.0"))
                                        .build())
                                .build()
                )
                .action(OpenRewriteRecipeAdapterAction.builder()
                        .resultMerger(rewriteResultMerger)
                        .recipe(new GenericOpenRewriteRecipe<>(() -> new MigrateToAnnotationVisitor()))
                        .condition(new HasAnyTypeReference(List.of("com.acme.libsec.SecurityCheck")))
                        .build()
                )
                .build();
    }

}
