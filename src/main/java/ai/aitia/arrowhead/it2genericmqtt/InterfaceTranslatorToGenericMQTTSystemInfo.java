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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Component
public class InterfaceTranslatorToGenericMQTTSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	@Value(InterfaceTranslatorToGenericMQTTConstants.$ENABLE_AUTHORIZATION_WD)
	private boolean authorizationEnabled;

	@Value(InterfaceTranslatorToGenericMQTTConstants.$TOKEN_ENCRYPTION_KEY)
	private String tokenEncryptionKey;

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(InterfaceTranslatorToGenericMQTTConstants.SYSTEM_VERSION);

			if (AuthenticationPolicy.CERTIFICATE == this.getAuthenticationPolicy()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {
		final List<String> fromInterfaces = new ArrayList<>(2);
		fromInterfaces.add(isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME);
		fromInterfaces.add(isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME);
		final String toInterface = getTargetInterface();

		final ServiceModel interfaceBridgeManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_INTERFACE_BRIDGE_MANAGEMENT)
				.version(InterfaceTranslatorToGenericMQTTConstants.VERSION_INTERFACE_BRIDGE_MANAGEMENT)
				.metadata(Constants.METADATA_KEY_INTERFACE_BRIDGE, Map.of(
						Constants.METADATA_KEY_FROM, fromInterfaces,
						Constants.METADATA_KEY_TO, toInterface))
				.serviceInterface(getHTTPInterfaceForInterfaceBridgeManagement())
				.build();

		return List.of(interfaceBridgeManagement);
	}

	//-------------------------------------------------------------------------------------------------
	public String getTargetInterface() {
		return isSslEnabled()
				? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME
				: Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean shouldTokenUsed() {
		return AuthenticationPolicy.CERTIFICATE != getAuthenticationPolicy() && authorizationEnabled;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTokenEncryptionKey() {
		return tokenEncryptionKey;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isAuthorizationEnabled() {
		return authorizationEnabled;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHTTPInterfaceForInterfaceBridgeManagement() {
		final String templateName = getSslProperties().isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel check = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(InterfaceTranslatorToGenericMQTTConstants.HTTP_API_OP_CHECK_TARGETS_PATH)
				.build();

		final HttpOperationModel init = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(InterfaceTranslatorToGenericMQTTConstants.HTTP_API_OP_INIT_BRIDGE_PATH)
				.build();

		final HttpOperationModel abort = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(InterfaceTranslatorToGenericMQTTConstants.HTTP_API_OP_ABORT_BRIDGE_PATH)
				.build();

		final ServiceInterfacePolicy policy = getAuthenticationPolicy() == AuthenticationPolicy.CERTIFICATE
				? ServiceInterfacePolicy.CERT_AUTH
				: ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH;

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.policy(policy)
				.basePath(InterfaceTranslatorToGenericMQTTConstants.HTTP_API_BRIDGE_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_CHECK_TARGETS, check)
				.operation(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_INIT_BRIDGE, init)
				.operation(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_ABORT_BRIDGE, abort)
				.build();
	}
}