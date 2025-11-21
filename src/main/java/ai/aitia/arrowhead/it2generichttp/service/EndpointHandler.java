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

import ai.aitia.arrowhead.it2generichttp.service.model.NormalizedTranslationBridgeModel;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;

public interface EndpointHandler {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void initializeBridge(final NormalizedTranslationBridgeModel model) throws InternalServerError, ExternalServerError;

	//-------------------------------------------------------------------------------------------------
	public void abortBridge(final NormalizedTranslationBridgeModel model) throws InternalServerError, ExternalServerError;
}