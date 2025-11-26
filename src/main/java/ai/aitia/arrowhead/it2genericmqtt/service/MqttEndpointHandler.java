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
package ai.aitia.arrowhead.it2genericmqtt.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.api.mqtt.utils.DynamicMqttClient;
import ai.aitia.arrowhead.it2genericmqtt.service.model.NormalizedTranslationBridgeModel;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class MqttEndpointHandler implements EndpointHandler {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private DynamicMqttClient mqttClient;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void initializeBridge(final NormalizedTranslationBridgeModel model) throws InternalServerError, ExternalServerError {
		logger.debug("MqttEndpointHandler.initializeBridge started...");

		final String topic = InterfaceTranslatorToGenericMQTTConstants.MQTT_DYNAMIC_BASE_TOPIC_PREFIX
				+ model.endpointId().toString()
				+ "/"
				+ model.operation();
		try {
			mqttClient.subscribe(topic);
		} catch (final MqttException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new ExternalServerError("Unable to subscribe translation bridge topic for bridge: " + model.bridgeId().toString());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void abortBridge(final NormalizedTranslationBridgeModel model) throws InternalServerError, ExternalServerError {
		logger.debug("MqttEndpointHandler.abortBridge started...");

		final String topic = InterfaceTranslatorToGenericMQTTConstants.MQTT_DYNAMIC_BASE_TOPIC_PREFIX
				+ model.endpointId().toString()
				+ "/"
				+ model.operation();
		try {
			mqttClient.unsubscribe(topic);
		} catch (final MqttException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
		}
	}
}