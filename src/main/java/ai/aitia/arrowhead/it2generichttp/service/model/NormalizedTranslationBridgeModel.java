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
package ai.aitia.arrowhead.it2generichttp.service.model;

import java.util.Map;
import java.util.UUID;

import eu.arrowhead.dto.TranslationDataModelTranslationDataDescriptorDTO;

public record NormalizedTranslationBridgeModel(
		UUID endpointId,
		UUID bridgeId,
		String inputInterface,
		TranslationDataModelTranslationDataDescriptorDTO inputDataModelTranslator,
		TranslationDataModelTranslationDataDescriptorDTO resultDataModelTranslator,
		String inputDataModelRequirement,
		String resultDataModelRequirement,
		String targetInterface,
		Map<String, Object> targetInterfaceProperties,
		String operation,
		String authorizationToken,
		Map<String, Object> interfaceTranslatorSettings) {
}