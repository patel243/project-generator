/*
 * Copyright 2012-2018 the original author or authors.
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

package io.spring.initializr.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.spring.initializr.generator.util.resource.ResourceMapper;
import io.spring.initializr.generator.util.resource.ResourceResolver;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.FileCopyUtils;

/**
 * {@link ProjectContributor} that contributes a single file, identified by a resource
 * pattern, to a generated project.
 *
 * @author Andy Wilkinson
 * @see PathMatchingResourcePatternResolver
 */
public class SingleResourceProjectContributor implements ProjectContributor {

	private final ResourceResolver resourceResolver;

	private final String filename;

	private final String resourcePattern;

	public SingleResourceProjectContributor(ResourceResolver resourceResolver,
			String filename, String resourcePattern) {
		this.resourceResolver = resourceResolver;
		this.filename = filename;
		this.resourcePattern = resourcePattern;
	}

	@Override
	public void contribute(Path projectRoot) throws IOException {
		Path output = projectRoot.resolve(this.filename);
		if (!Files.exists(output)) {
			Files.createDirectories(output.getParent());
			Files.createFile(output);
		}
		byte[] content = this.resourceResolver.resolveResource(getClass().getName(),
				this.resourcePattern, ResourceMapper.toBytes());
		FileCopyUtils.copy(content,
				Files.newOutputStream(output, StandardOpenOption.APPEND));
	}

}
