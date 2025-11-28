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
package ai.aitia.arrowhead.it2genericmqtt.init;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import javax.naming.ConfigurationException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.Defaults;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTSystemInfo;
import ai.aitia.arrowhead.it2genericmqtt.api.mqtt.utils.GeneralMqttClient;
import ai.aitia.arrowhead.it2genericmqtt.api.mqtt.utils.GenericMqttTopicHandler;
import ai.aitia.arrowhead.it2genericmqtt.report.ReportThread;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.init.ApplicationInitListener;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;
import eu.arrowhead.dto.TranslationReportRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import jakarta.annotation.Resource;

@Component
public class InterfaceTranslatorToGenericMQTTApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	@Autowired
	private ReportThread reportThread;

	@Resource(name = InterfaceTranslatorToGenericMQTTConstants.REPORT_QUEUE)
	private BlockingQueue<TranslationReportRequestDTO> reportQueue;

	@Autowired
	private GeneralMqttClient mqttClient;

	@Autowired
	private GenericMqttTopicHandler mqttTopicHandler;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException, ConfigurationException {
		logger.debug("customInit started...");

		final InterfaceTranslatorToGenericMQTTSystemInfo info = (InterfaceTranslatorToGenericMQTTSystemInfo) sysInfo;

		try {
			mqttClient.initialize();
			mqttClient.subscribe(InterfaceTranslatorToGenericMQTTConstants.MQTT_RESPONSE_TOPIC);
		} catch (final MqttException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new ConfigurationException("Can't access the MQTT broker: " + ex.getMessage());
		}

		mqttTopicHandler.start();

		if (info.isAuthorizationEnabled()) {
			specifyAuthorizationPolicy();
		}

		if (info.shouldTokenUsed() && !Utilities.isEmpty(info.getTokenEncryptionKey())) {
			registerTokenEncryptionKey();
		}

		reportThread.start();

	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customDestroy() {
		logger.debug("customDestroy started...");

		try {
			// send empty message to stop reporting thread gracefully
			reportQueue.put(new TranslationReportRequestDTO(null, null, null, null));
		} catch (final InterruptedException __) {
			// intentionally blank
		}

		try {
			mqttClient.unsubscribe(InterfaceTranslatorToGenericMQTTConstants.MQTT_RESPONSE_TOPIC);
			mqttTopicHandler.interrupt();
			mqttClient.destroy();
		} catch (final MqttException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void specifyAuthorizationPolicy() {
		logger.debug("specifyAuthorizationPolicy started...");

		final AuthorizationGrantRequestDTO payload = new AuthorizationGrantRequestDTO(
				Defaults.DEFAULT_CLOUD,
				AuthorizationTargetType.SERVICE_DEF.name(),
				Constants.SERVICE_DEF_INTERFACE_BRIDGE_MANAGEMENT,
				"Only the Translation Manager should use this service",
				new AuthorizationPolicyRequestDTO(AuthorizationPolicyType.WHITELIST.name(), List.of(Constants.SYS_NAME_TRANSLATION_MANAGER), null),
				null);

		arrowheadHttpService.consumeService(
				Constants.SERVICE_DEF_AUTHORIZATION,
				Constants.SERVICE_OP_GRANT,
				AuthorizationPolicyResponseDTO.class,
				payload);
	}

	//-------------------------------------------------------------------------------------------------
	private void registerTokenEncryptionKey() {
		logger.debug("registerTokenEncryptionKey started...");

		final InterfaceTranslatorToGenericMQTTSystemInfo info = (InterfaceTranslatorToGenericMQTTSystemInfo) sysInfo;
		final AuthorizationEncryptionKeyRegistrationRequestDTO payload = new AuthorizationEncryptionKeyRegistrationRequestDTO(
				info.getTokenEncryptionKey(),
				InterfaceTranslatorToGenericMQTTConstants.AES_CBC_ALGORITHM_IV_BASED);
		final String initVector = arrowheadHttpService.consumeService(
				Constants.SERVICE_DEF_AUTHORIZATION_TOKEN,
				Constants.SERVICE_OP_AUTHORIZATION_TOKEN_REGISTER_ENCRYPTION_KEY,
				Constants.SYS_NAME_CONSUMER_AUTHORIZATION,
				String.class,
				payload);

		arrowheadContext.put(InterfaceTranslatorToGenericMQTTConstants.KEY_INITIALIZATION_VECTOR, initVector);
	}
}