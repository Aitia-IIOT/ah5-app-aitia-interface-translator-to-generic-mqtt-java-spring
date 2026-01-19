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
package ai.aitia.arrowhead.it2genericmqtt.api.http.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.filter.thirdparty.MultiReadRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class PayloadProcessor {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String extractPayload(final HttpServletRequest request) throws IOException {
		logger.debug("extractPayload started...");

		final MultiReadRequestWrapper wrapper = (request instanceof MultiReadRequestWrapper)
				? (MultiReadRequestWrapper) request
				: new MultiReadRequestWrapper(request);

		if (Utilities.isEmpty(wrapper.getCachedBody())) {
			return null;
		}

		return new String(Base64.getEncoder().encode(wrapper.getCachedBody().getBytes()), StandardCharsets.UTF_8);
	}

	//-------------------------------------------------------------------------------------------------
	public byte[] extractResult(final String resultBase64) {
		logger.debug("extractResult started...");

		if (Utilities.isEmpty(resultBase64)) {
			return null;
		}

		return Base64.getDecoder().decode(resultBase64.getBytes(StandardCharsets.UTF_8));
	}
}