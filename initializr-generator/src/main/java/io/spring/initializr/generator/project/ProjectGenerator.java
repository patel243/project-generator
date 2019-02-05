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

package io.spring.initializr.generator.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.spring.initializr.generator.buildsystem.Build;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Main entry point for project generation.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class ProjectGenerator {

	private final Consumer<ProjectGenerationContext> projectGenerationContext;

	/**
	 * Create an instance with a customizer for the project generator application context.
	 * @param projectGenerationContext a consumer of the project generation context before
	 * it is refreshed.
	 */
	public ProjectGenerator(Consumer<ProjectGenerationContext> projectGenerationContext) {
		this.projectGenerationContext = projectGenerationContext;
	}

	/**
	 * Generate a project structure based on the specified {@link ProjectDescription}.
	 * @param description the description of the project to generate
	 * @return the root directory of the generated project structure
	 * @throws ProjectGenerationException if an error occurs while generating the project
	 */
	public ProjectGenerationResult<Path> generate(ProjectDescription description)
			throws ProjectGenerationException {
		return generate(description, (context) -> {
			ResolvedProjectDescription resolvedProjectDescription = context
					.getBean(ResolvedProjectDescription.class);
			Path projectRoot = context.getBean(ProjectDirectoryFactory.class)
					.createProjectDirectory(resolvedProjectDescription);
			Path projectDirectory = initializerProjectDirectory(projectRoot,
					resolvedProjectDescription);
			context.getBean(ProjectContributors.class).contribute(projectDirectory);
			return projectRoot;
		});
	}

	/**
	 * Generate a project structure using the specified
	 * {@link ProjectGenerationContextProcessor}.
	 * @param description the description of the project to generate
	 * @param projectGenerationContextProcessor the
	 * {@link ProjectGenerationContextProcessor} to invoke
	 * @param <T> the return type of the generation
	 * @return a {@link ProjectGenerationResult} with the generated content and the
	 * corresponding {@link Build}
	 * @throws ProjectGenerationException if an error occurs while generating the project
	 */
	public <T> ProjectGenerationResult<T> generate(ProjectDescription description,
			ProjectGenerationContextProcessor<T> projectGenerationContextProcessor)
			throws ProjectGenerationException {
		try (ProjectGenerationContext context = new ProjectGenerationContext()) {
			context.registerBean(ResolvedProjectDescription.class,
					resolve(description, context));
			context.register(CoreConfiguration.class);
			this.projectGenerationContext.accept(context);
			context.refresh();
			try {
				T process = projectGenerationContextProcessor.process(context);
				Build build = getBuild(context);
				return new ProjectGenerationResult<>(process, build);
			}
			catch (IOException ex) {
				throw new ProjectGenerationException("Failed to generate project", ex);
			}
		}
	}

	private Build getBuild(ProjectGenerationContext context) {
		try {
			return context.getBean(Build.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	private Supplier<ResolvedProjectDescription> resolve(ProjectDescription description,
			ProjectGenerationContext context) {
		return () -> {
			context.getBeanProvider(ProjectDescriptionCustomizer.class).orderedStream()
					.forEach((customizer) -> customizer.customize(description));
			return new ResolvedProjectDescription(description);
		};
	}

	private Path initializerProjectDirectory(Path rootDir,
			ResolvedProjectDescription description) throws IOException {
		if (description.getBaseDirectory() != null) {
			Path dir = rootDir.resolve(description.getBaseDirectory());
			Files.createDirectories(dir);
			return dir;
		}
		else {
			return rootDir;
		}
	}

	/**
	 * Configuration used to bootstrap the application context used for project
	 * generation.
	 */
	@Configuration
	@Import(ProjectGenerationImportSelector.class)
	static class CoreConfiguration {

		@Bean
		public ProjectContributors projectContributors(
				List<ProjectContributor> projectContributors) {
			return new ProjectContributors(projectContributors);
		}

	}

	/**
	 * {@link ImportSelector} for loading classes configured in {@code spring.factories}
	 * using the
	 * {@code io.spring.initializr.generator.project.ProjectGenerationConfiguration} key.
	 */
	static class ProjectGenerationImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			List<String> factories = SpringFactoriesLoader.loadFactoryNames(
					ProjectGenerationConfiguration.class, getClass().getClassLoader());
			return factories.toArray(new String[0]);
		}

	}

}
