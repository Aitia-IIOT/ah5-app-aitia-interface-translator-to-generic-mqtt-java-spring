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
package ai.aitia.arrowhead.it2generichttp.service.engine;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;

import ai.aitia.arrowhead.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;

@Service
public class ProviderDriver {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private HttpService httpService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public Pair<Integer, Optional<byte[]>> callOperation(
			final String operation,
			final String targetInterface,
			final Map<String, Object> targetInterfaceProperties,
			final byte[] payload,
			final String contentType,
			final String authorizationToken) {
		logger.debug("callOperation started...");
		Assert.isTrue(!Utilities.isEmpty(operation), "operation is missing");
		Assert.isTrue(!Utilities.isEmpty(targetInterfaceProperties), "Interface properties is missing");

		HttpMethod method = null;
		String operationPath = null;
		final String scheme = targetInterface.equals(Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME) ? Constants.HTTPS : Constants.HTTP;
		final String host = ((List<String>) targetInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)).get(0);
		final int port = (int) targetInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
		final String basePath = targetInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH).toString();
		if (targetInterfaceProperties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)
				&& (targetInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS) instanceof final Map operationsMap)
				&& operationsMap.containsKey(operation)) {
			final Object value = operationsMap.get(operation);
			try {
				final HttpOperationModel model = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
				method = HttpMethod.valueOf(model.method());
				operationPath = model.path();
			} catch (final ArrowheadException ex) {
				throw new InvalidParameterException("Essential information about the target operation is missing");
			}
		}

		if (method == null || Utilities.isEmpty(operationPath)) {
			throw new InvalidParameterException("Essential information about the target operation is missing");
		}

		final UriComponents uri = HttpUtilities.createURI(scheme, host, port, basePath + operationPath);

		final ByteArrayResource actualPayload = payload == null ? null : new ByteArrayResource(payload);

		final Map<String, String> headers = new HashMap<>(2);
		headers.put(HttpHeaders.CONTENT_TYPE, contentType);
		if (!Utilities.isEmpty(authorizationToken)) {
			headers.put(HttpHeaders.AUTHORIZATION, Constants.AUTHORIZATION_SCHEMA + " " + authorizationToken);
		}

		final Pair<Integer, Optional<ByteArrayResource>> response = httpService.sendRequestAndReturnStatus(
				uri,
				method,
				ByteArrayResource.class,
				actualPayload,
				null,
				headers);

		final Integer status = response.getFirst();
		final Optional<ByteArrayResource> optBody = response.getSecond();

		return Pair.of(
				status,
				optBody.isEmpty() || optBody.get().contentLength() == 0 ? Optional.empty() : Optional.of(optBody.get().getByteArray()));
	}
}