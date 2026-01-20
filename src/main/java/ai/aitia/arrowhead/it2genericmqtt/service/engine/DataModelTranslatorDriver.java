/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA
 *
 *******************************************************************************/
package ai.aitia.arrowhead.it2genericmqtt.service.engine;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTSystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.dto.DataModelTranslationInitRequestDTO;
import eu.arrowhead.dto.DataModelTranslationResultResponseDTO;
import eu.arrowhead.dto.TranslationDataModelTranslationDataDescriptorDTO;

@Service
public class DataModelTranslatorDriver {

	//=================================================================================================
	// members

	private static final String QUERY_PARAM_TASK_ID = "taskId";

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private HttpService httpService;

	@Autowired
	private InterfaceTranslatorToGenericMQTTSystemInfo sysInfo;

	//=================================================================================================
	// members

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public String initTranslation(final TranslationDataModelTranslationDataDescriptorDTO translator, final String input) {
		logger.debug("initTranslation started...");
		Assert.notNull(translator, "translator is null");
		Assert.isTrue(!Utilities.isEmpty(translator.interfaceProperties()), "Interface properties is missing");
		Assert.isTrue(!Utilities.isEmpty(input), "input is missing");

		HttpMethod method = HttpMethod.POST; // default method
		String operationPath = "/init-translation"; // default path
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final Map<String, Object> interfaceProperties = translator.interfaceProperties();
		final String host = ((List<String>) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)).get(0);
		final int port = (int) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
		final String basePath = interfaceProperties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH).toString();
		if (interfaceProperties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)
				&& (interfaceProperties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS) instanceof final Map operationsMap)
				&& operationsMap.containsKey(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_INIT_TRANSLATION)) {
			final Object value = operationsMap.get(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_INIT_TRANSLATION);
			try {
				final HttpOperationModel model = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
				method = HttpMethod.valueOf(model.method());
				operationPath = model.path();
			} catch (final ArrowheadException ex) {
				logger.warn("Invalid operations property for data model translator at {}:{}", host, port);
			}
		}

		final UriComponents uri = HttpUtilities.createURI(scheme, host, port, basePath + operationPath);
		final DataModelTranslationInitRequestDTO translationPayload = createTranslationPayload(translator, input);

		final String taskId = httpService.sendRequest(
				uri,
				method,
				String.class,
				translationPayload);

		return taskId;
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public void abortTranslation(final String taskId, final Map<String, Object> interfaceProperties) {
		logger.debug("abortTranslation started...");
		Assert.isTrue(!Utilities.isEmpty(taskId), "Task identifier is missing");
		Assert.isTrue(!Utilities.isEmpty(interfaceProperties), "Interface properties is missing");

		HttpMethod method = HttpMethod.DELETE; // default method
		String operationPath = "/abort-translation"; // default path
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final String host = ((List<String>) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)).get(0);
		final int port = (int) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
		final String basePath = interfaceProperties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH).toString();
		if (interfaceProperties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)
				&& (interfaceProperties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS) instanceof final Map operationsMap)
				&& operationsMap.containsKey(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_ABORT_TRANSLATION)) {
			final Object value = operationsMap.get(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_ABORT_TRANSLATION);
			try {
				final HttpOperationModel model = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
				method = HttpMethod.valueOf(model.method());
				operationPath = model.path();
			} catch (final ArrowheadException ex) {
				logger.warn("Invalid operations property for data model translator at {}:{}", host, port);
			}
		}

		final UriComponents uri = HttpUtilities.createURI(scheme, host, port, basePath + operationPath, QUERY_PARAM_TASK_ID, taskId);
		try {
			httpService.sendRequest(uri, method, Void.TYPE);
		} catch (final Exception ex) {
			logger.error("Error during data model translation abort: {}", ex.getMessage());
			logger.debug(ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public DataModelTranslationResultResponseDTO getTranslationResult(final String taskId, final Map<String, Object> interfaceProperties) {
		logger.debug("getTranslationResult started...");
		Assert.isTrue(!Utilities.isEmpty(taskId), "Task identifier is missing");
		Assert.isTrue(!Utilities.isEmpty(interfaceProperties), "Interface properties is missing");

		HttpMethod method = HttpMethod.GET; // default method
		String operationPath = "/get-translation-result"; // default path
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final String host = ((List<String>) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)).get(0);
		final int port = (int) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
		final String basePath = interfaceProperties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH).toString();
		if (interfaceProperties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)
				&& (interfaceProperties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS) instanceof final Map operationsMap)
				&& operationsMap.containsKey(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_GET_TRANSLATION_RESULT)) {
			final Object value = operationsMap.get(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_GET_TRANSLATION_RESULT);
			try {
				final HttpOperationModel model = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
				method = HttpMethod.valueOf(model.method());
				operationPath = model.path();
			} catch (final ArrowheadException ex) {
				logger.warn("Invalid operations property for data model translator at {}:{}", host, port);
			}
		}

		final UriComponents uri = HttpUtilities.createURI(scheme, host, port, basePath + operationPath, QUERY_PARAM_TASK_ID, taskId);

		final DataModelTranslationResultResponseDTO response = httpService.sendRequest(
				uri,
				method,
				DataModelTranslationResultResponseDTO.class);

		return response;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private DataModelTranslationInitRequestDTO createTranslationPayload(final TranslationDataModelTranslationDataDescriptorDTO translator, final String input) {
		logger.debug("createTranslationPayload started...");
		Assert.isTrue(!Utilities.isEmpty(translator.fromModelId()), "Input model id is missing");
		Assert.isTrue(!Utilities.isEmpty(translator.toModelId()), "Output model id is missing");

		return new DataModelTranslationInitRequestDTO(
				translator.fromModelId(),
				translator.toModelId(),
				input,
				translator.configurationSettings());
	}
}