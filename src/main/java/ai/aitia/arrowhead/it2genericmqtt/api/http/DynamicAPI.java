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
package ai.aitia.arrowhead.it2genericmqtt.api.http;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.api.http.utils.PayloadProcessor;
import ai.aitia.arrowhead.it2genericmqtt.service.DynamicService;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpUtilities;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@Hidden
public class DynamicAPI {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private PayloadProcessor processor;

	@Autowired
	private DynamicService service;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = InterfaceTranslatorToGenericMQTTConstants.HTTP_API_DYNAMIC_PATH_WITH_PARAM)
	public void doBridge(@PathVariable(required = true) final String pathId, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {
		logger.debug("doBridge started...");

		final String origin = HttpMethod.POST.name() + " " + InterfaceTranslatorToGenericMQTTConstants.HTTP_API_DYNAMIC_PATH_WITH_PARAM
				.replace(InterfaceTranslatorToGenericMQTTConstants.HTTP_PARAM_PATH_ID, pathId);
		final String originalContentType = httpServletRequest.getHeader(HttpHeaders.CONTENT_TYPE);

		try {
			final String payloadBase64 = processor.extractPayload(httpServletRequest);
			final Triple<Integer, Optional<String>, Optional<Boolean>> result = service.doBridgeOperation(pathId, payloadBase64, originalContentType, origin);
			handleResponse(httpServletRequest, httpServletResponse, result, origin);
		} catch (final Throwable t) {
			handleException(t, httpServletResponse, origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void handleResponse(
			final HttpServletRequest httpServletRequest,
			final HttpServletResponse httpServletResponse,
			final Triple<Integer, Optional<String>, Optional<Boolean>> result,
			final String origin) {
		logger.debug("handleResponse started...");

		try {
			final String acceptedContentType = httpServletRequest.getHeader(HttpHeaders.ACCEPT);
			if (!Utilities.isEmpty(acceptedContentType)) {
				httpServletResponse.setContentType(acceptedContentType);
			}
			httpServletResponse.setStatus(result.getLeft());
			if (result.getMiddle().isPresent()) {
				final byte[] resultBytes = processor.extractResult(result.getMiddle().get());
				httpServletResponse.getOutputStream().write(resultBytes);
				httpServletResponse.getOutputStream().flush();
			}
			httpServletResponse.getOutputStream().close();
		} catch (final IOException ex) {
			// nothing we can do
			logger.error("{} at {}: {}", ex.getClass().getName(), origin, ex.getMessage());
			logger.debug("Exception", ex);
		}

	}

	//-------------------------------------------------------------------------------------------------
	private void handleException(final Throwable t, final HttpServletResponse response, final String origin) {
		logger.debug("handleException started...");
		logger.debug("{} at {}: {}", t.getClass().getName(), origin, t.getMessage());
		logger.debug("Exception", t);

		final HttpStatus status = (t instanceof ArrowheadException)
				? HttpUtilities.calculateHttpStatusFromArrowheadException((ArrowheadException) t)
				: HttpStatus.INTERNAL_SERVER_ERROR;

		try {
			response.setContentType(MediaType.TEXT_PLAIN_VALUE);
			response.setStatus(status.value());
			response.getWriter().print(status + " " + t.getMessage());
			response.getWriter().flush();
			response.getWriter().close();
		} catch (final IOException ex) {
			// nothing we can do
			logger.error("{} at {}: {}", ex.getClass().getName(), origin, ex.getMessage());
			logger.debug("Exception", ex);
		}
	}
}