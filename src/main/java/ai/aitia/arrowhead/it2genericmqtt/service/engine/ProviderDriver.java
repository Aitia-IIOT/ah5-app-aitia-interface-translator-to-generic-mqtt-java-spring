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
package ai.aitia.arrowhead.it2genericmqtt.service.engine;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;

@Service
public class ProviderDriver {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
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

		// TODO: implement
		
		return null;
	}
}