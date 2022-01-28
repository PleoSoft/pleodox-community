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

package com.pleosoft.pleodox.community.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.pleosoft.pleodox.DocxGenerator;
import com.pleosoft.pleodox.TemplateFailedException;
import com.pleosoft.pleodox.data.PleodoxRoot.PleodoxRequest;
import com.pleosoft.pleodox.data.TemplateOptions;
import com.pleosoft.pleodox.storage.StorageService;

@RestController
@RequestMapping("/api/templates")
public class ApiTemplatingController {

	private static final Logger LOG = LoggerFactory.getLogger(ApiTemplatingController.class);

	private final DocxGenerator docxGenerator;
	private final StorageService storageService;

	@Autowired
	public ApiTemplatingController(DocxGenerator docxGenerator, StorageService storageService) {
		Assert.notNull(docxGenerator, "docxGenerator must not be null");
		Assert.notNull(storageService, "storageService must not be null");

		this.docxGenerator = docxGenerator;
		this.storageService = storageService;
	}

	@PostMapping(consumes = { "application/json" })
	public ResponseEntity<?> generateDocument(@RequestBody PleodoxRequest request,
			@RequestParam(defaultValue = "false") Boolean readOnly,
			@RequestParam(required = false) String protectionPass, @RequestParam(name = "template") String template)
			throws FileNotFoundException, IOException {

		String cleanTemplatePath = StringUtils.cleanPath(template);
		TemplateOptions templateOptions = new TemplateOptions().addOption("readOnly", readOnly)
				.addOption("protectionPass", protectionPass).addOption("templatename", cleanTemplatePath);

		try (InputStream is = asResource(storageService.resolveTemplate(cleanTemplatePath)).getInputStream()) {
			final String finalName = UUID.randomUUID().toString() + ".docx";
			Path tempResource = storageService.storeTemporary(is, finalName);

			try (OutputStream os = Files.newOutputStream(tempResource)) {
				try (InputStream templateStream = asResource(storageService.resolveTemplate(cleanTemplatePath))
						.getInputStream()) {
					try {
						docxGenerator.generate(templateStream, os, request, templateOptions);
					} catch (Throwable e) {
						if (tempResource != null) {
							try {
								Files.deleteIfExists(tempResource);
							} catch (Exception e1) {
								;
							}
						}
						throw new TemplateFailedException(e);
					}
				}
			}
			return ResponseEntity.ok().header("location", tempResource.getFileName().toString()).build();
		}
	}

	private Resource asResource(Path path) {
		return new FileSystemResource(path.toAbsolutePath());
	}

	@GetMapping(value = "/file/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> downloadGeneratedFile(@PathVariable String filename) {
		Path temporary = storageService.resolveTemporary(filename);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + temporary.getFileName().toString() + "\"")
				.body(new FileSystemResource(temporary));
	}

	@ExceptionHandler({ TemplateFailedException.class, FileNotFoundException.class, NoSuchFileException.class })
	public ResponseEntity<?> handleFileSystemException(Exception exc) {
		LOG.error("Error while executing request", exc);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).header("error", "file not found").build();
	}

	@ExceptionHandler({ Docx4JException.class })
	public ResponseEntity<?> handleTemplateFailedException(Docx4JException exc) {
		LOG.error("Error while executing request", exc);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.header("error", "check your template: " + exc.getMessage()).build();
	}

	@ExceptionHandler({ IOException.class })
	public ResponseEntity<?> handleTemplateFailedException(Exception exc) {
		LOG.error("Error while executing request", exc);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).header("error", "internal server error").build();
	}
}
