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
package com.acme.migrator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.sbm.engine.recipe.OpenRewriteDeclarativeRecipeAdapter;
import org.springframework.sbm.engine.recipe.Recipe;
import org.springframework.sbm.engine.recipe.RewriteRecipeLoader;
import org.springframework.sbm.engine.recipe.RewriteRecipeRunner;
import org.springframework.sbm.java.migration.conditions.HasAnyTypeReference;

import java.util.List;

/**
 * @author Fabian Krüger
 */
@Configuration
public class SecLib5to6MigrationRecipeConfig {

    @Bean
    Recipe secLib5to6MigrationRecipe(RewriteRecipeLoader rewriteRecipeLoader, RewriteRecipeRunner rewriteRecipeRunner) {
        return Recipe.builder()
                .name("migrate-seclib-5-to-6")
                .description("Add dependency and migrate deprecated code")
                .condition(new HasAnyTypeReference(List.of("com.acme.libsec.SecurityCheck")))
                .action(
                        new OpenRewriteDeclarativeRecipeAdapter("""
                                type: specs.openrewrite.org/v1beta/recipe
                                name: com.acme.migration.UpdateSecLib5To6
                                displayName: Upgrade SecLib from v5 to v6
                                description: 'Upgrades SecLib from v5 to v6.'
                                recipeList:
                                  - org.openrewrite.maven.UpgradeDependencyVersion:
                                      groupId: com.acme.seclib
                                      artifactId: seclib-core
                                      newVersion: 6.0.0                
                                """, rewriteRecipeLoader, rewriteRecipeRunner)
                )
                .build();
    }

}
