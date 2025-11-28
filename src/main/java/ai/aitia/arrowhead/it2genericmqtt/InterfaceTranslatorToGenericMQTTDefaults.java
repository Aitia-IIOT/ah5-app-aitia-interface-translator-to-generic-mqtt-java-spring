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

public final class InterfaceTranslatorToGenericMQTTDefaults {

	//=================================================================================================
	// members

	public static final String ENABLE_AUTHORIZATION_DEFAULT = "false";
	public static final String DATA_MODEL_TRANSLATOR_GET_RESULT_TRIES_DEFAULT = "10";
	public static final String DATA_MODEL_TRANSLATOR_GET_RESULT_WAIT_DEFAULT = "1000";
	public static final String BRIDGE_CLOSING_INTERVAL_DEFAULT = "60000";
	public static final String BRIDGE_INACTIVITY_THRESHOLD_DEFAULT = "60";
	public static final String MQTT_HANDLER_THREADS_DEFAULT = "5";
	public static final String PROVIDER_SERVICE_GET_RESULT_TRIES_DEFAULT = "10";
	public static final String PROVIDER_SERVICE_GET_RESULT_WAIT_DEFAULT = "1000";


	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private InterfaceTranslatorToGenericMQTTDefaults() {
		throw new UnsupportedOperationException();
	}
}