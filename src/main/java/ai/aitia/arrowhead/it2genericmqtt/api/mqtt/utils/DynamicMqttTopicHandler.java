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

import java.util.List;
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

import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Component
public class DynamicMqttTopicHandler extends Thread {

	//=================================================================================================
	// members

	@Value(InterfaceTranslatorToGenericMQTTConstants.$MQTT_HANDLER_THREADS_WD)
	private int numThreads;

	@Autowired
	private Function<MqttMessageContainer, DynamicMqttMessageContainerHandler> messageHandlerFactory;

	@Autowired
	private List<ArrowheadMqttFilter> filters;

	@Resource(name = InterfaceTranslatorToGenericMQTTConstants.MQTT_BRIDGE_QUEUE)
	private BlockingQueue<MqttMessageContainer> queue;

	private boolean doWork = false;

	private ThreadPoolExecutor threadpool = null;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void run() {
		logger.debug("DynamicMqttTopicHandler.run started...");

		doWork = true;
		while (doWork) {
			try {
				final MqttMessageContainer msgContainer = queue.take();
				threadpool.execute(messageHandlerFactory.apply(msgContainer));
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
		logger.debug("DynamicMqttTopicHandler.interrupt started...");

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
}