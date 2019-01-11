/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.initializr.generator.project.build.gradle;

import java.util.List;
import java.util.stream.Collectors;

import io.spring.initializr.generator.ResolvedProjectDescription;
import io.spring.initializr.generator.buildsystem.BuildItemResolver;
import io.spring.initializr.generator.buildsystem.gradle.ConditionalOnGradle;
import io.spring.initializr.generator.buildsystem.gradle.GradleBuild;
import io.spring.initializr.generator.condition.ConditionalOnPlatformVersion;
import io.spring.initializr.generator.io.IndentingWriterFactory;
import io.spring.initializr.generator.language.java.ConditionalOnJavaLanguage;
import io.spring.initializr.generator.packaging.war.ConditionalOnWarPackaging;
import io.spring.initializr.generator.project.build.BuildCustomizer;
import io.spring.initializr.generator.project.scm.git.GitIgnoreContributor;
import io.spring.initializr.generator.util.LambdaSafe;
import io.spring.initializr.generator.util.resource.ResourceResolver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for contributions specific to the generation of a project that will use
 * Gradle as its build system.
 *
 * @author Andy Wilkinson
 */
@Configuration
@ConditionalOnGradle
public class GradleProjectGenerationConfiguration {

	private final IndentingWriterFactory indentingWriterFactory;

	public GradleProjectGenerationConfiguration(
			IndentingWriterFactory indentingWriterFactory) {
		this.indentingWriterFactory = indentingWriterFactory;
	}

	@Bean
	public GitIgnoreContributor gradleGitIgnoreContributor(
			ResourceResolver resourceResolver) {
		return new GitIgnoreContributor(resourceResolver, "classpath:gradle/gitignore");
	}

	@Bean
	public GradleBuild gradleBuild(ObjectProvider<BuildItemResolver> buildItemResolver,
			ObjectProvider<BuildCustomizer<?>> buildCustomizers) {
		return createGradleBuild(buildItemResolver.getIfAvailable(),
				buildCustomizers.orderedStream().collect(Collectors.toList()));
	}

	@SuppressWarnings("unchecked")
	private GradleBuild createGradleBuild(BuildItemResolver buildItemResolver,
			List<BuildCustomizer<?>> buildCustomizers) {
		GradleBuild build = (buildItemResolver != null)
				? new GradleBuild(buildItemResolver) : new GradleBuild();
		LambdaSafe.callbacks(BuildCustomizer.class, buildCustomizers, build)
				.invoke((customizer) -> customizer.customize(build));
		return build;
	}

	@Bean
	public BuildCustomizer<GradleBuild> defaultGradleBuildCustomizer(
			ResolvedProjectDescription projectDescription) {
		return (build) -> build
				.setSourceCompatibility(projectDescription.getJavaVersion());
	}

	@Bean
	@ConditionalOnJavaLanguage
	public BuildCustomizer<GradleBuild> javaPluginContributor() {
		return (build) -> build.addPlugin("java");
	}

	@Bean
	@ConditionalOnWarPackaging
	public BuildCustomizer<GradleBuild> warPluginContributor() {
		return (build) -> build.addPlugin("war");
	}

	@Bean
	@ConditionalOnPlatformVersion("2.0.0.M1")
	public BuildCustomizer<GradleBuild> applyDependencyManagementPluginContributor() {
		return (build) -> build.applyPlugin("io.spring.dependency-management");
	}

	@Bean
	public GradleBuildProjectContributor gradleBuildProjectContributor(
			GradleBuild build) {
		return new GradleBuildProjectContributor(build, this.indentingWriterFactory);
	}

	/**
	 * Configuration specific to projects using Gradle 3.
	 */
	@Configuration
	@ConditionalOnGradleVersion("3")
	static class Gradle3ProjectGenerationConfiguration {

		@Bean
		public GradleWrapperContributor gradle3WrapperContributor(
				ResourceResolver resourceResolver) {
			return new GradleWrapperContributor(resourceResolver, "3");
		}

		@Bean
		public Gradle3SettingsGradleProjectContributor settingsGradleProjectContributor(
				GradleBuild build) {
			return new Gradle3SettingsGradleProjectContributor(build);
		}

		@Bean
		public BuildCustomizer<GradleBuild> springBootPluginContributor(
				ResolvedProjectDescription projectDescription) {
			return (build) -> {
				build.buildscript((buildscript) -> buildscript
						.dependency("org.springframework.boot:spring-boot-gradle-plugin:"
								+ projectDescription.getPlatformVersion()));
				build.applyPlugin("org.springframework.boot");
			};
		}

	}

	/**
	 * Configuration specific to projects using Gradle 4.
	 */
	@Configuration
	@ConditionalOnGradleVersion("4")
	static class Gradle4ProjectGenerationConfiguration {

		@Bean
		public GradleWrapperContributor gradle4WrapperContributor(
				ResourceResolver resourceResolver) {
			return new GradleWrapperContributor(resourceResolver, "4");
		}

		@Bean
		public SettingsGradleProjectContributor settingsGradleProjectContributor(
				GradleBuild build, IndentingWriterFactory indentingWriterFactory) {
			return new SettingsGradleProjectContributor(build, indentingWriterFactory);
		}

		@Bean
		public BuildCustomizer<GradleBuild> springBootPluginContributor(
				ResolvedProjectDescription projectDescription) {
			return (build) -> build.addPlugin("org.springframework.boot",
					projectDescription.getPlatformVersion().toString());
		}

	}

}
