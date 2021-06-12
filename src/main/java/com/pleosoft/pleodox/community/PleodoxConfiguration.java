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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pleosoft.pleodox.DocxGenerator;
import com.pleosoft.pleodox.NoopDocumentGenerationHandler;
import com.pleosoft.pleodox.community.storage.PleodoxStorageConfigurationProperties;
import com.pleosoft.pleodoxstorage.DefaultStorageService;
import com.pleosoft.pleodoxstorage.StorageService;

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
	public NoopDocumentGenerationHandler documentGenerationHandler() {
		return new NoopDocumentGenerationHandler();
	}
	
	@Bean
	public DocxGenerator docxGenerator() {
		return new DocxGenerator();
	}
}
