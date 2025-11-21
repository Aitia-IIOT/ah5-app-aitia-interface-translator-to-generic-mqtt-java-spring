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

import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import ai.aitia.arrowhead.it2generichttp.service.model.NormalizedTranslationBridgeModel;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameValidator;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.TranslationBridgeInitializationRequestDTO;
import eu.arrowhead.dto.TranslationCheckTargetsRequestDTO;
import eu.arrowhead.dto.TranslationDataModelTranslationDataDescriptorDTO;
import eu.arrowhead.dto.TranslationTargetDTO;

@Service
public class ManagementServiceValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ServiceOperationNameNormalizer serviceOpNormalizer;

	@Autowired
	private ServiceOperationNameValidator serviceOpValidator;

	@Autowired
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Autowired
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Autowired
	private DataModelIdentifierNormalizer dataModelIdNormalizer;

	@Autowired
	private DataModelIdentifierValidator dataModelIdValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public TranslationCheckTargetsRequestDTO validateAndNormalizeTranslationCheckTargetsRequest(final TranslationCheckTargetsRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeTranslationCheckTargetsRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is missing");

		validateTranslationCheckTargetsRequest(dto, origin);
		final TranslationCheckTargetsRequestDTO normalized = normalizeTranslationCheckTargetsRequest(dto);
		validateNormalizedTranslationCheckTargetsRequest(normalized, origin);

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedTranslationBridgeModel validateAndNormalizeTranslationBridgeInitializationRequest(final TranslationBridgeInitializationRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeTranslationBridgeInitializationRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is missing");

		validateTranslationBridgeInitializationRequest(dto, origin);
		final NormalizedTranslationBridgeModel normalized = normalizeTranslationBridgeInitializationRequest(dto);
		validateNormalizedTranslationBridgeInitializationRequest(normalized, origin);

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public UUID validateAndNormalizeBridgeId(final String bridgeId, final String origin) {
		logger.debug("validateAndNormalizeBridgeId started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(bridgeId)) {
			throw new InvalidParameterException("Bridge identifier is missing", origin);
		}

		try {
			return UUID.fromString(bridgeId.trim());
		} catch (final IllegalArgumentException __) {
			throw new InvalidParameterException("Bridge identifier is invalid", origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateTranslationCheckTargetsRequest(final TranslationCheckTargetsRequestDTO dto, final String origin) {
		logger.debug("validateTranslationCheckTargetsRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("Request is missing", origin);
		}

		if (Utilities.isEmpty(dto.targetOperation())) {
			throw new InvalidParameterException("Target operation is missing", origin);
		}

		if (Utilities.isEmpty(dto.targets())) {
			throw new InvalidParameterException("targets list is missing", origin);
		}

		if (Utilities.containsNull(dto.targets())) {
			throw new InvalidParameterException("targets list contains null element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedTranslationCheckTargetsRequest(final TranslationCheckTargetsRequestDTO normalized, final String origin) {
		logger.debug("validateNormalizedTranslationCheckTargetsRequest started...");

		try {
			serviceOpValidator.validateServiceOperationName(normalized.targetOperation());
			normalized.targets()
					.forEach(t -> {
						t.interfaces()
								.forEach(i -> {
									interfaceTemplateNameValidator.validateInterfaceTemplateName(i.templateName());
								});
					});
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateTranslationBridgeInitializationRequest(final TranslationBridgeInitializationRequestDTO dto, final String origin) {
		logger.debug("validateTranslationBridgeInitializationRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("Request is missing", origin);
		}

		if (Utilities.isEmpty(dto.bridgeId())) {
			throw new InvalidParameterException("Bridge id is missing", origin);
		}

		if (!Utilities.isUUID(dto.bridgeId().trim())) {
			throw new InvalidParameterException("Bridge id is invalid: " + dto.bridgeId(), origin);
		}

		if (Utilities.isEmpty(dto.inputInterface())) {
			throw new InvalidParameterException("Input interface name is missing", origin);
		}

		if (dto.inputDataModelTranslator() != null) {
			validateDataModelTranslator(dto.inputDataModelTranslator(), origin);
		}

		if (dto.resultDataModelTranslator() != null) {
			validateDataModelTranslator(dto.resultDataModelTranslator(), origin);
		}

		if (Utilities.isEmpty(dto.targetInterface())) {
			throw new InvalidParameterException("Target interface name is missing", origin);
		}

		if (Utilities.isEmpty(dto.targetInterfaceProperties())) {
			throw new InvalidParameterException("targetInterfaceProperties is missing", origin);
		}

		if (Utilities.isEmpty(dto.operation())) {
			throw new InvalidParameterException("Operation is missing", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedTranslationBridgeInitializationRequest(final NormalizedTranslationBridgeModel normalized, final String origin) {
		logger.debug("validateNormalizedTranslationBridgeInitializationRequest started...");

		try {
			interfaceTemplateNameValidator.validateInterfaceTemplateName(normalized.inputInterface());

			if (normalized.inputDataModelTranslator() != null) {
				dataModelIdValidator.validateDataModelIdentifier(normalized.inputDataModelTranslator().fromModelId());
				dataModelIdValidator.validateDataModelIdentifier(normalized.inputDataModelTranslator().toModelId());
			}

			if (normalized.resultDataModelTranslator() != null) {
				dataModelIdValidator.validateDataModelIdentifier(normalized.resultDataModelTranslator().fromModelId());
				dataModelIdValidator.validateDataModelIdentifier(normalized.resultDataModelTranslator().toModelId());
			}

			if (normalized.inputDataModelRequirement() != null) {
				dataModelIdValidator.validateDataModelIdentifier(normalized.inputDataModelRequirement());
			}

			if (normalized.resultDataModelRequirement() != null) {
				dataModelIdValidator.validateDataModelIdentifier(normalized.resultDataModelRequirement());
			}

			interfaceTemplateNameValidator.validateInterfaceTemplateName(normalized.targetInterface());
			serviceOpValidator.validateServiceOperationName(normalized.operation());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateDataModelTranslator(final TranslationDataModelTranslationDataDescriptorDTO dmTranslator, final String origin) {
		logger.debug("validateDataModelTranslator started...");

		if (Utilities.isEmpty(dmTranslator.fromModelId())) {
			throw new InvalidParameterException("fromModelId is missing", origin);
		}

		if (Utilities.isEmpty(dmTranslator.toModelId())) {
			throw new InvalidParameterException("toModelId is missing", origin);
		}

		if (Utilities.isEmpty(dmTranslator.interfaceProperties())) {
			throw new InvalidParameterException("interfaceProperties is missing", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	private TranslationCheckTargetsRequestDTO normalizeTranslationCheckTargetsRequest(final TranslationCheckTargetsRequestDTO dto) {
		logger.debug("normalizeTranslationCheckTargetsRequest started...");

		return new TranslationCheckTargetsRequestDTO(
				serviceOpNormalizer.normalize(dto.targetOperation()),
				dto.targets()
						.stream()
						.map(t -> normalizeTarget(t))
						.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private TranslationTargetDTO normalizeTarget(final TranslationTargetDTO target) {
		logger.debug("normalizeTarget started...");

		return new TranslationTargetDTO(
				target.instanceId(),
				target.interfaces()
						.stream()
						.map(i -> new ServiceInstanceInterfaceResponseDTO(
								interfaceTemplateNameNormalizer.normalize(i.templateName()),
								i.protocol(),
								i.policy(),
								i.properties()))
						.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedTranslationBridgeModel normalizeTranslationBridgeInitializationRequest(final TranslationBridgeInitializationRequestDTO dto) {
		logger.debug("normalizeTranslationBridgeInitializationRequest started...");

		return new NormalizedTranslationBridgeModel(
				UUID.randomUUID(),
				UUID.fromString(dto.bridgeId().trim()),
				interfaceTemplateNameNormalizer.normalize(dto.inputInterface()),
				normalizeDataModelTranslator(dto.inputDataModelTranslator()),
				normalizeDataModelTranslator(dto.resultDataModelTranslator()),
				Utilities.isEmpty(dto.inputDataModelRequirement()) ? null : dataModelIdNormalizer.normalize(dto.inputDataModelRequirement()),
				Utilities.isEmpty(dto.resultDataModelRequirement()) ? null : dataModelIdNormalizer.normalize(dto.resultDataModelRequirement()),
				interfaceTemplateNameNormalizer.normalize(dto.targetInterface()),
				dto.targetInterfaceProperties(),
				serviceOpNormalizer.normalize(dto.operation()),
				Utilities.isEmpty(dto.authorizationToken()) ? null : dto.authorizationToken().trim(),
				Utilities.isEmpty(dto.interfaceTranslatorSettings()) ? Map.of() : dto.interfaceTranslatorSettings());
	}

	//-------------------------------------------------------------------------------------------------
	private TranslationDataModelTranslationDataDescriptorDTO normalizeDataModelTranslator(final TranslationDataModelTranslationDataDescriptorDTO dmTranslator) {
		logger.debug("normalizeDataModelTranslator started...");

		if (dmTranslator == null) {
			return null;
		}

		return new TranslationDataModelTranslationDataDescriptorDTO(
				dataModelIdNormalizer.normalize(dmTranslator.fromModelId()),
				dataModelIdNormalizer.normalize(dmTranslator.toModelId()),
				dmTranslator.interfaceProperties(),
				Utilities.isEmpty(dmTranslator.configurationSettings()) ? Map.of() : dmTranslator.configurationSettings());
	}
}