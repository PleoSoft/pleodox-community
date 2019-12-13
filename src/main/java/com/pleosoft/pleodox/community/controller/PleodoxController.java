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

import static org.alfresco.transformer.fs.FileManager.createAttachment;
import static org.alfresco.transformer.fs.FileManager.createSourceFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFile;
import static org.alfresco.transformer.fs.FileManager.createTargetFileName;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.alfresco.transform.client.model.TransformRequest;
import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.AbstractTransformerController;
import org.alfresco.transformer.logging.LogEntry;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pleosoft.pleodox.boot.data.TemplateOutputFormat;
import com.pleosoft.pleodox.community.transformer.PleodoxTransformer;

@RestController
public class PleodoxController extends AbstractTransformerController {

	@Autowired
	private PleodoxTransformer transformer;

	@PostMapping(value = "/transform", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Resource> transform(HttpServletRequest request,
			@RequestParam("file") MultipartFile sourceMultipartFile,
			@RequestParam(value = "timeout", required = false) Long timeout,
			@RequestParam(defaultValue = "DOCX") TemplateOutputFormat format,
			@RequestParam(defaultValue = "false") Boolean readOnly,
			@RequestParam(required = false) String protectionPass,
			@RequestParam(name = "template") List<String> templates, @RequestParam(required = false) String moveTo,
			@RequestParam(defaultValue = "false") Boolean mergePdf, TransformRequest transformRequest) {

		String targetFilename = createTargetFileName(sourceMultipartFile.getOriginalFilename(),
				transformRequest.getTargetExtension());
		getProbeTestTransform().incrementTransformerCount();
		File sourceFile = createSourceFile(request, sourceMultipartFile);
		File targetFile = createTargetFile(request, targetFilename);

		// Both files are deleted by TransformInterceptor.afterCompletion
		processTransform(sourceFile, targetFile, "", "", transformRequest.getTransformRequestOptions(), timeout);
		
		// TODO Consider streaming the request and response rather than using temporary
		// files
		// https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/streaming-response-body.html

		final ResponseEntity<Resource> body = createAttachment(targetFilename, targetFile);
		LogEntry.setTargetSize(targetFile.length());
		long time = LogEntry.setStatusCodeAndMessage(OK.value(), "Success");
		// time += LogEntry.addDelay(testDelay);
		getProbeTestTransform().recordTransformTime(time);
		return body;
	}

	@Override
	public void processTransform(File sourceFile, File targetFile, String sourceMimetype, String targetMimetype,
			Map<String, String> transformOptions, Long timeout) {

		// TODO Consider streaming the request and response rather than using temporary
		// files
		// https://www.logicbig.com/tutorials/spring-framework/spring-web-mvc/streaming-response-body.html
		try {
			transformer.transform(sourceFile, targetFile, transformOptions);
		} catch (Exception e) {
			throw new TransformException(INTERNAL_SERVER_ERROR.value(), getMessage(e));
		}

		if (!targetFile.exists()) {
			throw new TransformException(INTERNAL_SERVER_ERROR.value(),
					"Transformer failed to create an output file. Target file does not exist.");
		}
		if (sourceFile.length() > 0 && targetFile.length() == 0) {
			throw new TransformException(INTERNAL_SERVER_ERROR.value(),
					"Transformer failed to create an output file. Target file is empty but source file was not empty.");
		}
	}

	@Override
	public String getTransformerName() {
		// TODO Auto-generated method stub
		return "pleodox";
	}

	@Override
	public ProbeTestTransform getProbeTestTransform() {
		return new ProbeTestTransform(this, "quick.html", "quick.txt", 119, 30, 150, 1024, 60 * 2 + 1, 60 * 2) {
			@Override
			protected void executeTransformCommand(File sourceFile, File targetFile) {
				Map<String, String> parameters = new HashMap<>();
				parameters.put("sourceEncoding", "UTF-8");
				parameters.put("targetEncoding", "UTF-8");
				try {
					transformer.transform(sourceFile, targetFile, parameters);
				} catch (Exception e) {
					throw new TransformException(INTERNAL_SERVER_ERROR.value(), getMessage(e));
				}
			}
		};
	}

	@Override
	public String version() {
		return getTransformerName() + " available";
	}

	private static String getMessage(Exception e) {
		return e.getMessage() == null || e.getMessage().isEmpty() ? e.getClass().getSimpleName() : e.getMessage();
	}
}
