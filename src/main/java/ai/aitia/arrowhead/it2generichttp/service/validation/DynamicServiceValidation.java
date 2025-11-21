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
package ai.aitia.arrowhead.it2generichttp.service.validation;

import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import ai.aitia.arrowhead.it2generichttp.service.model.NormalizedTranslationBridgeModel;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;

@Service
public class DynamicServiceValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public UUID validateAndNormalizeEndpointId(final String endpointId, final String origin) {
		logger.debug("validateAndNormalizeEndpointId started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(endpointId)) {
			throw new InvalidParameterException("Endpoint identifier is missing", origin);
		}

		try {
			return UUID.fromString(endpointId.trim());
		} catch (final IllegalArgumentException __) {
			throw new InvalidParameterException("Endpoint identifier is invalid", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void crossCheckModelAndPayload(final NormalizedTranslationBridgeModel model, final String payloadBase64, final String origin) {
		logger.debug("crossCheckModelAndPayload started...");
		Assert.notNull(model, "model is missing");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// error if there is an input translator but no payload
		if (model.inputDataModelTranslator() != null && payloadBase64 == null) {
			throw new InvalidParameterException("Payload is missing", origin);
		}

		// it is not an error if there is no input translator but there is a payload => it just means that the payload is already in the appropriate data model
	}

	//-------------------------------------------------------------------------------------------------
	public void crossCheckModelAndResult(final NormalizedTranslationBridgeModel model, final Optional<byte[]> result, final String origin) {
		logger.debug("crossCheckModelAndResult started...");
		Assert.notNull(model, "model is missing");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// error if there is an result translator but no result
		if (model.resultDataModelTranslator() != null && result.isEmpty()) {
			throw new InvalidParameterException("Result is missing", origin);
		}

		// it is not an error if there is no result translator but there is a result => it just means that the result is already in the appropriate data model
	}
}