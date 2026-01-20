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
package ai.aitia.arrowhead.it2genericmqtt;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import ai.aitia.arrowhead.it2genericmqtt.api.mqtt.utils.DynamicMqttMessageContainerHandler;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;
import eu.arrowhead.dto.MqttResponseTemplate;
import eu.arrowhead.dto.TranslationReportRequestDTO;

@Configuration
public class BeanConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean(InterfaceTranslatorToGenericMQTTConstants.REPORT_QUEUE)
	BlockingQueue<TranslationReportRequestDTO> getReportQueue() {
		return new LinkedBlockingQueue<>();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean(InterfaceTranslatorToGenericMQTTConstants.MQTT_GENERAL_QUEUE)
	BlockingQueue<MqttMessageContainer> getMqttBridgeQueue() {
		return new LinkedBlockingQueue<>();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean(InterfaceTranslatorToGenericMQTTConstants.PROVIDER_RESPONSE_MAP)
	Map<String, Optional<MqttResponseTemplate>> getProviderResponseMap() {
		return new ConcurrentHashMap<>();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	DynamicMqttMessageContainerHandler createDynamicMqttMessageContainerHandler(final MqttMessageContainer msgContainer) {
		return new DynamicMqttMessageContainerHandler(msgContainer);
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	Function<MqttMessageContainer, DynamicMqttMessageContainerHandler> dynamicMqttMessageContainerHandlerFactory() {
		return container -> createDynamicMqttMessageContainerHandler(container);
	}
}