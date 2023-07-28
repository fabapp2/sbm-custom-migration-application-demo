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
import org.springframework.sbm.engine.context.ProjectContext;
import org.springframework.sbm.project.resource.TestProjectContext;
import org.springframework.sbm.test.ActionTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Fabian Krüger
 */
public class UpgradeDependencyTest {
    @Test
    @DisplayName("should upgrade dependency")
    void shouldUpgradeDependency() {
        ActionTest.withProjectContext(
                TestProjectContext.buildProjectContext()
                        .withBuildFileHavingDependencies("com.acme.seclib:seclib-core:5.0.0")
        )
        .actionUnderTest(new UpradeDependencyAction("com.acme.seclib", "seclib-core", "6.0.0"))
        .verify(pc -> {
            String result = pc.getApplicationModules().getRootModule().getBuildFile().print();
            assertThat(result)
                    .isEqualTo("""
                            <?xml version="1.0" encoding="UTF-8"?>
                            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                                <modelVersion>4.0.0</modelVersion>
                                                        
                                <groupId>com.example</groupId>
                                <artifactId>dummy-root</artifactId>
                                <version>0.1.0-SNAPSHOT</version>
                                <packaging>jar</packaging>
                                <dependencies>
                                    <dependency>
                                        <groupId>com.acme.seclib</groupId>
                                        <artifactId>seclib-core</artifactId>
                                        <version>6.0.0</version>
                                    </dependency>
                                </dependencies>
                                
                            </project>
                            """);
        });

    }
}
