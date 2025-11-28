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

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.api.mqtt.utils.GeneralMqttClient;
import ai.aitia.arrowhead.it2genericmqtt.service.model.BridgeStore;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.mqtt.MqttQoS;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.dto.MqttRequestTemplate;
import eu.arrowhead.dto.MqttResponseTemplate;
import jakarta.annotation.Resource;

@Service
public class ProviderDriver {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private GeneralMqttClient client;

	@Resource(name = InterfaceTranslatorToGenericMQTTConstants.PROVIDER_RESPONSE_MAP)
	private Map<String, Optional<MqttResponseTemplate>> providerResponseMap;

	@Autowired
	private BridgeStore bridgeStore;

	@Value(InterfaceTranslatorToGenericMQTTConstants.$PROVIDER_SERVICE_GET_RESULT_TRIES_WD)
	private int defaultGetResultRetries;

	@Value(InterfaceTranslatorToGenericMQTTConstants.$PROVIDER_SERVICE_GET_RESULT_WAIT_WD)
	private long defaultGetResultWait;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<Integer, Optional<byte[]>> callOperation(
			final UUID bridgeId,
			final String operation,
			final String targetInterface,
			final Map<String, Object> targetInterfaceProperties,
			final byte[] payload,
			final String contentType,
			final String authorizationToken,
			final Map<String, Object> settings) {
		logger.debug("callOperation started...");
		Assert.isTrue(!Utilities.isEmpty(operation), "operation is missing");
		Assert.isTrue(!Utilities.isEmpty(targetInterfaceProperties), "Interface properties is missing");

		final String traceId = UUID.randomUUID().toString();
		providerResponseMap.put(traceId, Optional.empty());

		final String baseTopic = targetInterfaceProperties.get(MqttInterfaceModel.PROP_NAME_BASE_TOPIC).toString();
		if (Utilities.isEmpty(baseTopic)) {
			throw new InvalidParameterException("Essential information about the target operation is missing");
		}

		try {
			// sending request
			final MqttRequestTemplate template = new MqttRequestTemplate(
					traceId,
					authorizationToken,
					InterfaceTranslatorToGenericMQTTConstants.MQTT_RESPONSE_TOPIC,
					MqttQoS.EXACTLY_ONCE.value(),
					Map.of(),
					payload == null ? null : mapper.readValue(payload, Object.class));
			final MqttMessage msg = new MqttMessage(mapper.writeValueAsBytes(template));
			client.publish(baseTopic + operation, msg);
		} catch (final MqttException ex) {
			throw new ExternalServerError(ex.getMessage(), ex);
		} catch (final IOException ex) {
			throw new InternalServerError(ex.getMessage(), ex);
		}

		// waiting for answer
		final int tries = getSettingValue(settings, Integer.class, Constants.SETTING_KEY_PROVIDER_SERVICE_GET_RESULT_TRIES, defaultGetResultRetries);
		final long wait = getSettingValue(settings, Long.class, Constants.SETTING_KEY_PROVIDER_SERVICE_GET_RESULT_WAIT, defaultGetResultWait);

		for (int i = 0; i < tries; ++i) {
			if (!bridgeStore.containsBridgeId(bridgeId)) {
				// abort happened during service calling
				providerResponseMap.remove(traceId);
				throw new ExternalServerError("Translation bridge is aborted");
			}

			final boolean hasResult = providerResponseMap.containsKey(traceId) && providerResponseMap.get(traceId).isPresent();
			if (hasResult) {
				// get the result
				final MqttResponseTemplate responseTemplate = providerResponseMap.get(traceId).get();
				providerResponseMap.remove(traceId);

				try {
					return Pair.of(
							responseTemplate.status(),
							responseTemplate.payload() == null ? Optional.empty() : Optional.of(mapper.writeValueAsBytes(responseTemplate.payload())));
				} catch (final JsonProcessingException ex) {
					throw new InternalServerError(ex.getMessage(), ex);
				}
			} else {
				// wait
				try {
					Thread.sleep(wait);
				} catch (final InterruptedException __) {
					// nothing to do
				}
			}
		}

		// not waiting anymore
		providerResponseMap.remove(traceId);
		throw new ExternalServerError("Provider did not respond in time");
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private <T> T getSettingValue(final Map<String, Object> settings, final Class<T> type, final String key, final T defaultValue) {
		logger.debug("ProviderDriver.getSettingValue started...");

		final Object value = settings.get(key);

		return (type.isInstance(value) ? (T) value : defaultValue);
	}
}