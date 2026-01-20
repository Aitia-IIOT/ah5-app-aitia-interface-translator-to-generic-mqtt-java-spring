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
package ai.aitia.arrowhead.it2genericmqtt.mqtt.filter.authorization;

import java.security.InvalidParameterException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.service.model.BridgeStore;
import ai.aitia.arrowhead.it2genericmqtt.service.model.NormalizedTranslationBridgeModel;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;

@Component
public class MqttBridgeFilter implements ArrowheadMqttFilter {

	//=================================================================================================
	// members

	private static final int ORDER = 15;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private BridgeStore bridgeStore;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public int order() {
		return ORDER;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void doFilter(final String authInfo, final MqttRequestModel request) {
		logger.debug("MqttBridgeFilter.doFilter started...");

		if (request.getBaseTopic().startsWith(InterfaceTranslatorToGenericMQTTConstants.MQTT_DYNAMIC_BASE_TOPIC_PREFIX)
				&& !isBridgeAllowed(authInfo, request)) {
			throw new ForbiddenException("Requester has no permission to use this topic", request.getBaseTopic() + "/" + request.getOperation());
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	private boolean isBridgeAllowed(final String authInfo, final MqttRequestModel request) {
		logger.debug("MqttBridgeFilter.isBridgeAllowed started...");

		final String baseTopic = Utilities.stripEndSlash(request.getBaseTopic());
		final String[] baseTopicParts = baseTopic.split("/");

		if (baseTopicParts.length < 5) {
			// not a valid dynamic topic
			throw new InvalidParameterException("Request topic is invalid");

		}

		final String endpointIdStr = baseTopicParts[4].trim();

		if (!Utilities.isUUID(endpointIdStr)) {
			throw new InvalidParameterException("Request topic is invalid");
		}

		final NormalizedTranslationBridgeModel model = bridgeStore.getByEndpointId(UUID.fromString(endpointIdStr));
		if (model == null) {
			throw new InvalidParameterException("Request topic is invalid");
		}

		if (Utilities.isEmpty(authInfo)) {
			throw new AuthException("No authorization info has been provided");
		}

		final String bridgeIdStr = authInfo.trim();
		if (!Utilities.isUUID(bridgeIdStr)) {
			throw new AuthException("Invalid authorization info");
		}

		return model.bridgeId().equals(UUID.fromString(bridgeIdStr));
	}
}