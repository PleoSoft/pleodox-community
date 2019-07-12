/**
 * Copyright 2019 Pleo Soft d.o.o. (pleosoft.com)

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pleosoft.pleodox.community;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.zip.Deflater;

import org.jodconverter.DocumentConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.zip.transformer.ZipResultType;
import org.springframework.integration.zip.transformer.ZipTransformer;

import com.pleosoft.pleodox.boot.service.DocumentGenerateService;
import com.pleosoft.pleodox.boot.service.DocumentGenerationHandler;
import com.pleosoft.pleodox.boot.service.NoopDocumentGenerationHandler;
import com.pleosoft.pleodox.boot.service.TemplatesService;
import com.pleosoft.pleodox.boot.service.TransformationService;
import com.pleosoft.pleodox.boot.storage.DefaultStorageService;
import com.pleosoft.pleodox.boot.storage.StorageService;
import com.pleosoft.pleodox.community.storage.PleodoxStorageConfigurationProperties;

@Configuration
public class PleodoxConfiguration {

	@Bean
	public StorageService storageService(PleodoxStorageConfigurationProperties properties) throws IOException {
		final String templates = properties.getTemplatesDir();
		final String generated = properties.getGeneratedDir();

		return new DefaultStorageService(
				generated != null ? Paths.get(generated) : Paths.get(System.getProperty("java.io.tmpdir")),
				templates != null ? Paths.get(templates) : null);
	}

	@Bean
	public DocumentGenerateService templatingService() {
		final DocumentGenerateService templatingService = new DocumentGenerateService();
		return templatingService;
	}

	@Bean
	public TemplatesService templatesService(DocumentGenerateService templatingService, StorageService storageService,
			TransformationService transformationService, ZipTransformer zipTransformer,
			DocumentGenerationHandler documentGenerationHandler) {

		return new TemplatesService(templatingService, storageService, transformationService, zipTransformer,
				documentGenerationHandler);
	}

	@Bean
	public TransformationService transformationService(final DocumentConverter converter) {
		return new TransformationService(converter);
	}

	@Bean
	public ZipTransformer zipTransformer() {
		final ZipTransformer zipTransformer = new ZipTransformer();
		zipTransformer.setCompressionLevel(Deflater.BEST_COMPRESSION);
		zipTransformer.setZipResultType(ZipResultType.FILE);
		zipTransformer.setDeleteFiles(true);
		return zipTransformer;
	}

	@Bean
	public NoopDocumentGenerationHandler documentGenerationHandler() {
		return new NoopDocumentGenerationHandler();
	}

}
