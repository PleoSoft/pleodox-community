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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
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

import com.pleosoft.pleodox.boot.data.DataRoot;
import com.pleosoft.pleodox.boot.data.TemplateOutputFormat;
import com.pleosoft.pleodox.boot.service.TemplateFailedException;
import com.pleosoft.pleodox.boot.service.TemplatesService;
import com.pleosoft.pleodox.boot.storage.StorageFileNotFoundException;
import com.pleosoft.pleodox.boot.storage.StorageService;

@RestController
@RequestMapping("/api/templates")
public class ApiTemplatingController {

	private static final Logger LOG = LoggerFactory.getLogger(ApiTemplatingController.class);

	private final TemplatesService templatesService;
	private final StorageService storageService;

	@Autowired
	public ApiTemplatingController(TemplatesService templatesService, StorageService storageService) {
		Assert.notNull(templatesService, "templatesService must not be null");
		Assert.notNull(storageService, "storageService must not be null");

		this.templatesService = templatesService;
		this.storageService = storageService;
	}

	@PostMapping(consumes = { "application/json" })
	public ResponseEntity<?> generateDocument(@RequestBody Map<String, Object> request,
			@RequestParam(defaultValue = "DOCX") TemplateOutputFormat format,
			@RequestParam(defaultValue = "false") Boolean readOnly,
			@RequestParam(required = false) String protectionPass,
			@RequestParam(name = "template") List<String> templates, @RequestParam(required = false) String moveTo,
			@RequestParam(defaultValue = "false") Boolean mergePdf) throws FileNotFoundException, IOException {

		if (!StringUtils.hasText(moveTo)) {
			moveTo = UUID.randomUUID().toString();
		}

		DataRoot dataRoot = new DataRoot();
		dataRoot.putAll(request);
		Path resource = templatesService.generateDocument(dataRoot, format, readOnly, protectionPass, templates, moveTo,
				null, mergePdf);

		return ResponseEntity.ok().header("location", getFileWithParentFolder(resource)).build();
	}
	
	@GetMapping(value = "/file/{folder}/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> downloadGeneratedFile(@PathVariable String folder, @PathVariable String filename) {
		Path temporary = storageService.rsolveTemporary(folder + File.separator + filename);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + temporary.getFileName().toString() + "\"")
				.body(new FileSystemResource(temporary));
	}

	@ExceptionHandler({ TemplateFailedException.class, StorageFileNotFoundException.class, FileNotFoundException.class,
			NoSuchFileException.class })
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

	private static final String getFileWithParentFolder(final Path resource) throws IOException {
		return getFileWithParentFolder(new FileSystemResource(resource));
	}
	
	private static final String getFileWithParentFolder(final Resource resource) throws IOException {
		return StringUtils.cleanPath(
				Paths.get(resource.getURI()).getParent().getFileName().resolve(resource.getFilename()).toString());
	}
}
