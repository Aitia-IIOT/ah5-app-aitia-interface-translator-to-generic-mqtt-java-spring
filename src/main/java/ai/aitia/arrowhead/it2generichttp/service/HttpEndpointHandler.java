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
package ai.aitia.arrowhead.it2generichttp.service;

import org.springframework.stereotype.Service;

import ai.aitia.arrowhead.it2generichttp.service.model.NormalizedTranslationBridgeModel;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class HttpEndpointHandler implements EndpointHandler {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void initializeBridge(final NormalizedTranslationBridgeModel model) throws InternalServerError, ExternalServerError {
		// intentionally do nothing
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void abortBridge(final NormalizedTranslationBridgeModel model) throws InternalServerError, ExternalServerError {
		// intentionally do nothing
	}
}