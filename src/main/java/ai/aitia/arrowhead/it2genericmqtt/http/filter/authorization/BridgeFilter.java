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
package ai.aitia.arrowhead.it2genericmqtt.http.filter.authorization;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.service.model.BridgeStore;
import ai.aitia.arrowhead.it2genericmqtt.service.model.NormalizedTranslationBridgeModel;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.dto.ErrorMessageDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BridgeFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private BridgeStore bridgeStore;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		logger.debug("BridgeFilter.doFilterInternal started...");

		try {
			final String requestTarget = request.getRequestURL().toString();
			if (requestTarget.contains(InterfaceTranslatorToGenericMQTTConstants.HTTP_API_DYNAMIC_PATH)
					&& !isBridgeAllowed(request)) {
				throw new ForbiddenException("Requester has no permission to use this operation", requestTarget);
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	private boolean isBridgeAllowed(final HttpServletRequest request) {
		logger.debug("BridgeFilter.isBridgeAllowed started...");

		final String requestTarget = Utilities.stripEndSlash(request.getRequestURI().toString());
		final String[] targetParts = requestTarget.split("/");

		if (targetParts.length < 5) {
			// not a valid dynamic endpoint
			throw new InvalidParameterException("Request target is invalid");

		}

		final String endpointIdStr = targetParts[4].trim();

		if (!Utilities.isUUID(endpointIdStr)) {
			throw new InvalidParameterException("Request target is invalid");
		}

		final NormalizedTranslationBridgeModel model = bridgeStore.getByEndpointId(UUID.fromString(endpointIdStr));
		if (model == null) {
			throw new InvalidParameterException("Request target is invalid");
		}

		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (Utilities.isEmpty(authHeader)) {
			throw new AuthException("No authorization header has been provided");
		}

		String[] split = authHeader.trim().split(" ");
		if (split.length != 2 || !split[0].equals(Constants.AUTHENTICATION_SCHEMA)) {
			throw new AuthException("Invalid authorization header");
		}

		final String bridgeIdStr = split[1].trim();
		if (!Utilities.isUUID(bridgeIdStr)) {
			throw new AuthException("Invalid authorization header");
		}

		return model.bridgeId().equals(UUID.fromString(bridgeIdStr));
	}

	//-------------------------------------------------------------------------------------------------
	protected void handleException(final ArrowheadException ex, final HttpServletResponse response) throws IOException {
		final String origin = ex.getOrigin() == null ? Constants.UNKNOWN : ex.getOrigin();
		log.debug("{} at {}: {}", ex.getClass().getName(), origin, ex.getMessage());
		log.debug("Exception", ex);

		final HttpStatus status = HttpUtilities.calculateHttpStatusFromArrowheadException(ex);
		final ErrorMessageDTO dto = HttpUtilities.createErrorMessageDTO(ex);

		response.setContentType(MediaType.TEXT_PLAIN_VALUE);
		response.setStatus(status.value());
		response.getWriter().print(status + " " + dto.errorMessage());
		response.getWriter().flush();
	}
}