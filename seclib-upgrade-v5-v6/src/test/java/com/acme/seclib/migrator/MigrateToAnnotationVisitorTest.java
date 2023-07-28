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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.sbm.engine.recipe.OpenRewriteRecipeAdapterAction;
import org.springframework.sbm.project.resource.TestProjectContext;
import org.springframework.sbm.support.openrewrite.GenericOpenRewriteRecipe;
import org.springframework.sbm.test.ActionTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fabian Krüger
 */
public class MigrateToAnnotationVisitorTest {

    @Test
    @DisplayName("Migrate from method call to annotation")
    void migrateFromMethodCallToAnnotation() {
        ActionTest.withProjectContext(TestProjectContext.buildProjectContext()
                        .withJavaSources("""
                                package com.acme.business;
                                                        
                                import com.acme.seclib.SecurityCheck;
                                import lombok.RequiredArgsConstructor;
                                import org.springframework.stereotype.Component;
                                import java.util.UUID;
                                                        
                                @Component
                                @RequiredArgsConstructor
                                public class BusinessService {
                                                        
                                    private final PersonalDataService personalDataService;
                                                        
                                    public String getPersonalData(UUID userId) {
                                        String userData = personalDataService.getUserData(userId);
                                        SecurityCheck.verifyResult(userData);
                                        return userData;
                                    }
                                }
                                """)
                        .withBuildFileHavingDependencies(
                                "org.projectlombok:lombok:1.18.24",
                                "org.springframework.boot:spring-boot-starter:2.7.5",
                                "com.acme.seclib:seclib-core:6.0.0")
                )
                .actionUnderTest(new OpenRewriteRecipeAdapterAction(new GenericOpenRewriteRecipe<>(() -> new MigrateToAnnotationVisitor())))
                .verify(pc -> {
                    assertThat(pc.getProjectJavaSources().list().get(0).print()).isEqualTo(
                            """
                            package com.acme.business;
                                    
                            import com.acme.seclib.Secured;
                            import lombok.RequiredArgsConstructor;
                            import org.springframework.stereotype.Component;
                            import java.util.UUID;
                            
                            @Component
                            @RequiredArgsConstructor
                            public class BusinessService {
                            
                                private final PersonalDataService personalDataService;
                            
                                @Secured
                                public String getPersonalData(UUID userId) {
                                    String userData = personalDataService.getUserData(userId);
                                    return userData;
                                }
                            }
                            """
                    );
                });

    }

}
