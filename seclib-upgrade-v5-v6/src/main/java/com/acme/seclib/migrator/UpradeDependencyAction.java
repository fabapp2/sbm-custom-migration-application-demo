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


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.sbm.engine.context.ProjectContext;
import org.springframework.sbm.engine.recipe.AbstractAction;
import org.springframework.sbm.engine.recipe.OpenRewriteDeclarativeRecipeAdapter;
import org.springframework.sbm.engine.recipe.RewriteRecipeLoader;
import org.springframework.sbm.engine.recipe.RewriteRecipeRunner;

/**
 * @author Fabian Krüger
 */
public class UpradeDependencyAction extends AbstractAction {
    private final String artifactId;
    private final String groupId;
    private final String version;

    @JsonIgnore
    @Autowired
    private RewriteRecipeLoader rewriteRecipeLoader;
    @JsonIgnore
    @Autowired
    private RewriteRecipeRunner rewriteRecipeRunner;

    public UpradeDependencyAction(String groupId, String artifactId, String version) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.version = version;
    }


    @Override
    public void apply(ProjectContext projectContext) {
        String declarativeOpenrewriteRecipe = """
                type: specs.openrewrite.org/v1beta/recipe
                name: com.acme.migration.UpdateSecLib5To6
                displayName: Upgrade SecLib from v5 to v6
                description: 'Upgrades SecLib from v5 to v6.'
                recipeList:
                  - org.openrewrite.maven.UpgradeDependencyVersion:
                      groupId: %s
                      artifactId: %s
                      newVersion: %s
                """.formatted(groupId, artifactId, version);
        new OpenRewriteDeclarativeRecipeAdapter(declarativeOpenrewriteRecipe, rewriteRecipeLoader, rewriteRecipeRunner).apply(projectContext);
    }
}
