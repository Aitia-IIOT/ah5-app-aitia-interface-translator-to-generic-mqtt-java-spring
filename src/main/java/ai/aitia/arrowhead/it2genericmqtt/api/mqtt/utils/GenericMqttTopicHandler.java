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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;
import eu.arrowhead.dto.MqttResponseTemplate;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Component
public class GenericMqttTopicHandler extends Thread {

	//=================================================================================================
	// members

	@Value(InterfaceTranslatorToGenericMQTTConstants.$MQTT_HANDLER_THREADS_WD)
	private int numThreads;

	@Autowired
	private Function<MqttMessageContainer, DynamicMqttMessageContainerHandler> messageHandlerFactory;

	@Autowired
	private List<ArrowheadMqttFilter> filters;

	@Resource(name = InterfaceTranslatorToGenericMQTTConstants.MQTT_GENERAL_QUEUE)
	private BlockingQueue<MqttMessageContainer> queue;
	
	@Resource(name = InterfaceTranslatorToGenericMQTTConstants.PROVIDER_RESPONSE_MAP)
	private Map<String, Optional<MqttResponseTemplate>> providerResponseMap;
	
	@Autowired
	private ObjectMapper mapper;

	private boolean doWork = false;

	private ThreadPoolExecutor threadpool = null;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void run() {
		logger.debug("GeneralMqttTopicHandler.run started...");

		doWork = true;
		while (doWork) {
			try {
				final MqttMessageContainer msgContainer = queue.take();
				if (msgContainer.getTopic().startsWith(InterfaceTranslatorToGenericMQTTConstants.MQTT_DYNAMIC_BASE_TOPIC_PREFIX)) {
					threadpool.execute(messageHandlerFactory.apply(msgContainer));
				} else if (msgContainer.getTopic().equals(InterfaceTranslatorToGenericMQTTConstants.MQTT_RESPONSE_TOPIC)) {
					try {
						final MqttResponseTemplate template = parseMqttMessage(msgContainer);
						if (providerResponseMap.containsKey(template.traceId())) {
							providerResponseMap.put(template.traceId(), Optional.of(template));
						} else {
							logger.warn("Unexpected or late response with traceId: {}", template.traceId());
						}
					} catch (final Exception ex) {
						logger.error(ex.getMessage());
						logger.debug(ex);
					}
				} else {
					logger.warn("Unexpected message to topic: {}", msgContainer.getTopic());
				}
			} catch (final RejectedExecutionException ex) {
				// no rejections
				logger.debug(ex.getMessage());
				logger.debug(ex);
			} catch (final InterruptedException ex) {
				logger.debug(ex.getMessage());
				logger.debug(ex);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void interrupt() {
		logger.debug("GeneralMqttTopicHandler.interrupt started...");

		doWork = false;
		super.interrupt();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		logger.debug("init started...");

		this.threadpool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
		filters.sort((a, b) -> a.order() - b.order());
	}
	
	//-------------------------------------------------------------------------------------------------
	private MqttResponseTemplate parseMqttMessage(final MqttMessageContainer msgContainer) {
		logger.debug("parseMqttMessage started...");

		if (msgContainer.getMessage() == null) {
			throw new InvalidParameterException("Invalid message template: null message");
		}

		try {
			return mapper.readValue(msgContainer.getMessage().getPayload(), MqttResponseTemplate.class);
		} catch (final IOException ex) {
			throw new InvalidParameterException("Invalid message template. Reason: " + ex.getMessage());
		}
	}
}