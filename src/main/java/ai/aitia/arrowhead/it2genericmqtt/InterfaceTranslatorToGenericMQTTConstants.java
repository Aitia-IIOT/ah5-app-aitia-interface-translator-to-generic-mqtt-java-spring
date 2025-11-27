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

import java.util.UUID;

public final class InterfaceTranslatorToGenericMQTTConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_VERSION = "1.0.0";

	public static final String VERSION_INTERFACE_BRIDGE_MANAGEMENT = "1.0.0";

	public static final String AES_CBC_ALGORITHM_IV_BASED = "AES/CBC/PKCS5Padding"; // With initialization vector
	public static final String KEY_INITIALIZATION_VECTOR = "authorization-initialization-vector";

	public static final String ENABLE_AUTHORIZATION = "enable.authorization";
	public static final String $ENABLE_AUTHORIZATION_WD = "${" + ENABLE_AUTHORIZATION + ":" + InterfaceTranslatorToGenericMQTTDefaults.ENABLE_AUTHORIZATION_DEFAULT + "}";
	public static final String TOKEN_ENCRYPTION_KEY = "token.encryption.key";
	public static final String $TOKEN_ENCRYPTION_KEY = "${" + TOKEN_ENCRYPTION_KEY + ":}";
	public static final String DATA_MODEL_TRANSLATOR_GET_RESULT_TRIES = "data.model.translator.get.result.tries";
	public static final String $DATA_MODEL_TRANSLATOR_GET_RESULT_TRIES_WD = "${" + DATA_MODEL_TRANSLATOR_GET_RESULT_TRIES + ":"
			+ InterfaceTranslatorToGenericMQTTDefaults.DATA_MODEL_TRANSLATOR_GET_RESULT_TRIES_DEFAULT + "}";
	public static final String DATA_MODEL_TRANSLATOR_GET_RESULT_WAIT = "data.model.translator.get.result.wait";
	public static final String $DATA_MODEL_TRANSLATOR_GET_RESULT_WAIT_WD = "${" + DATA_MODEL_TRANSLATOR_GET_RESULT_WAIT + ":"
			+ InterfaceTranslatorToGenericMQTTDefaults.DATA_MODEL_TRANSLATOR_GET_RESULT_WAIT_DEFAULT + "}";
	public static final String BRIDGE_CLOSING_INTERVAL = "bridge.closing.interval";
	public static final String $BRIDGE_CLOSING_INTERVAL_WD = "${" + BRIDGE_CLOSING_INTERVAL + ":" + InterfaceTranslatorToGenericMQTTDefaults.BRIDGE_CLOSING_INTERVAL_DEFAULT + "}";
	public static final String BRIDGE_INACTIVITY_THRESHOLD = "bridge.inactivity.threshold";
	public static final String $BRIDGE_INACTIVITY_THRESHOLD_WD = "${" + BRIDGE_INACTIVITY_THRESHOLD + ":" + InterfaceTranslatorToGenericMQTTDefaults.BRIDGE_INACTIVITY_THRESHOLD_DEFAULT + "}";
	public static final String MQTT_HANDLER_THREADS = "mqtt.handler.threads";
	public static final String $MQTT_HANDLER_THREADS_WD = "${" + MQTT_HANDLER_THREADS + ":" + InterfaceTranslatorToGenericMQTTDefaults.MQTT_HANDLER_THREADS_DEFAULT + "}";

	public static final String HTTP_API_BASE_PATH = "/interface/translator";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_BRIDGE_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/bridge/mgmt";
	public static final String HTTP_API_OP_CHECK_TARGETS_PATH = "/check-targets";
	public static final String HTTP_API_OP_INIT_BRIDGE_PATH = "/initialize-bridge";
	public static final String HTTP_API_OP_ABORT_BRIDGE_PATH = "/abort-bridge";
	public static final String HTTP_PARAM_BRIDGE_ID = "{bridgeId}";
	public static final String HTTP_API_OP_ABORT_BRIDGE_PATH_WITH_PARAM = HTTP_API_OP_ABORT_BRIDGE_PATH + "/" + HTTP_PARAM_BRIDGE_ID;
	public static final String HTTP_PARAM_PATH_ID = "{pathId}";
	public static final String HTTP_API_DYNAMIC_PATH = HTTP_API_BASE_PATH + "/dynamic";
	public static final String HTTP_API_DYNAMIC_PATH_WITH_PARAM = HTTP_API_DYNAMIC_PATH + "/" + HTTP_PARAM_PATH_ID;

	public static final String MQTT_BRIDGE_BROKER_CONNECT_ID = "TRANSLATION-BRIDGE-" + UUID.randomUUID().toString();
	public static final String MQTT_DYNAMIC_BASE_TOPIC_PREFIX = "arrowhead/interface/translator/dynamic/";
	public static final String MQTT_RESPONSE_TOPIC = "arrowhead/interface/provider/response/" + UUID.randomUUID().toString();
	public static final String MQTT_BRIDGE_QUEUE = "mqttBridgeQueue";
	public static final String MQTT_ORIGINAL_MIME_TYPE = "application/json";

	public static final String POLICY_TRANSLATION_BRIDGE_TOKEN_AUTH = "TRANSLATION_BRIDGE_TOKEN_AUTH";

	public static final String REPORT_QUEUE = "reportQueue";
	public static final String BRIDGE_CLOSING_TRIGGER = "bridgeClosingTrigger";
	public static final String BRIDGE_CLOSING_JOB_FACTORY = "bridgeClosingJobFactory";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private InterfaceTranslatorToGenericMQTTConstants() {
		throw new UnsupportedOperationException();
	}
}