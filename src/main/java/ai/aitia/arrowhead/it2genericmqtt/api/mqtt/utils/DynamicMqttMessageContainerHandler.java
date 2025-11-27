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
package ai.aitia.arrowhead.it2genericmqtt.api.mqtt.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.service.DynamicService;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttQoS;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.dto.MqttRequestTemplate;
import eu.arrowhead.dto.MqttResponseTemplate;
import eu.arrowhead.dto.enums.ExceptionType;

public class DynamicMqttMessageContainerHandler implements Runnable {

	//=================================================================================================
	// members

	@Autowired
	private GeneralMqttClient client;

	@Autowired
	private DynamicService service;

	@Autowired
	private List<ArrowheadMqttFilter> filters;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ServiceOperationNameNormalizer operationNameNormalizer;

	private final Logger logger = LogManager.getLogger(getClass());

	private final MqttMessageContainer msgContainer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public DynamicMqttMessageContainerHandler(final MqttMessageContainer msgContainer) {
		this.msgContainer = msgContainer;
	}

	//-------------------------------------------------------------------------------------------------
	public void run() {
		logger.debug("DynamicMqttMessageContainerHandler.run started...");
		Assert.notNull(msgContainer, "msgContainer is null");

		MqttRequestModel request = null;
		try {
			final Entry<String, MqttRequestModel> parsed = parseMqttMessage(msgContainer);
			request = parsed.getValue();

			// Filter chain
			for (final ArrowheadMqttFilter filter : filters) {
				filter.doFilter(parsed.getKey(), request);
			}

			// API call
			handle(request);
		} catch (final Exception ex) {
			errorResponse(ex, request);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private org.apache.commons.lang3.tuple.Pair<String, MqttRequestModel> parseMqttMessage(final MqttMessageContainer msgContainer) {
		logger.debug("parseMqttMessage started...");

		if (msgContainer.getMessage() == null) {
			throw new InvalidParameterException("Invalid message template: null message");
		}

		try {
			final MqttRequestTemplate template = mapper.readValue(msgContainer.getMessage().getPayload(), MqttRequestTemplate.class);
			return new ImmutablePair<>(
					template.authentication(),
					new MqttRequestModel(msgContainer.getBaseTopic(), operationNameNormalizer.normalize(msgContainer.getOperation()), template));
		} catch (final IOException ex) {
			throw new InvalidParameterException("Invalid message template. Reason: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void errorResponse(final Exception ex, final MqttRequestModel request) {
		logger.debug("errorResponse started...");

		if (request == null) {
			logger.error("MQTT error occured, without request model being parsed");
			logger.debug(ex);
			return;
		}

		if (Utilities.isEmpty(request.getResponseTopic())) {
			logger.error("MQTT request error occured, but no response topic has been defined");
			logger.debug(ex);
			return;
		}

		final ExceptionType exType = calculateExceptionType(ex);
		final String payload = exType.getErrorCode() + " " + ex.getMessage();

		response(
				request.getRequester(),
				request.getResponseTopic(),
				request.getTraceId(),
				request.getQosRequirement(),
				calculateStatusFromExceptionType(exType).value(),
				payload);
	}

	//-------------------------------------------------------------------------------------------------
	private ExceptionType calculateExceptionType(final Exception ex) {
		logger.debug("calculateExceptionType started...");

		if (!(ex instanceof ArrowheadException)) {
			return ExceptionType.INTERNAL_SERVER_ERROR;
		}

		return ((ArrowheadException) ex).getExceptionType();
	}

	//-------------------------------------------------------------------------------------------------
	private MqttStatus calculateStatusFromExceptionType(final ExceptionType exType) {
		logger.debug("calculateStatusFromExceptionType started...");

		switch (exType) {
		case AUTH:
			return MqttStatus.UNAUTHORIZED;
		case FORBIDDEN:
			return MqttStatus.FORBIDDEN;
		case INVALID_PARAMETER:
			return MqttStatus.BAD_REQUEST;
		case DATA_NOT_FOUND:
			return MqttStatus.NOT_FOUND;
		case EXTERNAL_SERVER_ERROR:
			return MqttStatus.EXTERNAL_SERVER_ERROR;
		case TIMEOUT:
			return MqttStatus.TIMEOUT;
		case LOCKED:
			return MqttStatus.LOCKED;
		default:
			return MqttStatus.INTERNAL_SERVER_ERROR;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void response(
			final String receiver,
			final String topic,
			final String traceId,
			final MqttQoS qos,
			final int status,
			final Object payload) {
		logger.debug("response started");
		Assert.isTrue(!Utilities.isEmpty(topic), "topic is empty");

		try {
			final MqttResponseTemplate template = new MqttResponseTemplate(status, traceId, receiver, payload == null ? "" : payload);
			final MqttMessage msg = new MqttMessage(mapper.writeValueAsBytes(template));
			msg.setQos(qos == null ? Constants.MQTT_DEFAULT_QOS : qos.value());
			client.publish(topic, msg);
		} catch (final JsonProcessingException ex) {
			logger.debug(ex);
			throw new InternalServerError("MQTT service response message creation failed: " + ex.getMessage());
		} catch (final MqttException ex) {
			logger.debug(ex);
			throw new ExternalServerError("MQTT service response failed: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void handle(final MqttRequestModel request) throws IOException {
		logger.debug("DynamicMqttMessageContainerHandler.handle started...");

		final String endpointId = extractEndpointId(request.getBaseTopic());
		final String payloadBase64 = extractPayload(request);
		final Pair<Integer, Optional<String>> result = service.doBridgeOperation(
				endpointId,
				payloadBase64,
				InterfaceTranslatorToGenericMQTTConstants.MQTT_ORIGINAL_MIME_TYPE,
				request.getBaseTopic() + request.getOperation());

		final int statusCode = result.getFirst();
		Object resultPayload = null;
		if (result.getSecond().isPresent()) {
			byte[] resultBytes = extractResult(result.getSecond().get());
			resultPayload = mapper.readValue(resultBytes, Object.class);
		}

		if (!Utilities.isEmpty(request.getResponseTopic())) {
			response(
					request.getRequester(),
					request.getResponseTopic(),
					request.getTraceId(),
					request.getQosRequirement(),
					statusCode,
					resultPayload);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private String extractPayload(final MqttRequestModel request) throws IOException {
		logger.debug("extractPayload started...");

		if (request.getPayload() == null) {
			return null;
		}

		byte[] payloadBytes = null;
		if (request.getPayload() instanceof final String strPayload) {
			if (Utilities.isEmpty(strPayload)) {
				return null;
			}

			payloadBytes = strPayload.getBytes();
		} else {
			payloadBytes = mapper.writeValueAsBytes(request.getPayload());
		}

		return new String(Base64.getEncoder().encode(payloadBytes), StandardCharsets.UTF_8);
	}

	//-------------------------------------------------------------------------------------------------
	private byte[] extractResult(final String resultBase64) {
		logger.debug("extractResult started...");

		if (Utilities.isEmpty(resultBase64)) {
			return null;
		}

		return Base64.getDecoder().decode(resultBase64.getBytes(StandardCharsets.UTF_8));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	private String extractEndpointId(final String baseTopic) {
		logger.debug("extractPayload started...");

		return baseTopic.split("/")[4].trim();
	}
}