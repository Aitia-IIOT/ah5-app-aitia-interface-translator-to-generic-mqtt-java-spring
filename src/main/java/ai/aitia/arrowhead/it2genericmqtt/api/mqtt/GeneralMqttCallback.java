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
package ai.aitia.arrowhead.it2genericmqtt.api.mqtt;

import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.api.mqtt.utils.GeneralMqttClient;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;
import jakarta.annotation.Resource;

@Service
public class GeneralMqttCallback implements MqttCallback {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private GeneralMqttClient client;

	@Resource(name = InterfaceTranslatorToGenericMQTTConstants.MQTT_BRIDGE_QUEUE)
	private BlockingQueue<MqttMessageContainer> queue;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void messageArrived(final String topic, final MqttMessage message) throws Exception {
		logger.debug("messageArrived started...");
		
		if (topic.startsWith(InterfaceTranslatorToGenericMQTTConstants.MQTT_DYNAMIC_BASE_TOPIC_PREFIX)) {
			queue.add(new MqttMessageContainer(topic, message));
		} else if (topic.equals(InterfaceTranslatorToGenericMQTTConstants.MQTT_RESPONSE_TOPIC)) {
			// TODO: handle responses from providers
		} else {
			logger.warn("Unexpected message on topic: {}", topic);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void deliveryComplete(final IMqttDeliveryToken token) {
		logger.debug("MQTT message delivered to broker " + client.getServerURI() + ". Topic(s): " + String.join(", ", token.getTopics()));
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void connectionLost(final Throwable cause) {
		logger.error("MQTT Broker connection lost: " + client.getServerURI() + ". Reason: " + cause.getMessage());
	}
}
