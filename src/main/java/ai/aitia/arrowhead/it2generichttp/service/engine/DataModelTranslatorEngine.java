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

import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.it2generichttp.service.model.BridgeStore;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.dto.DataModelTranslationResultResponseDTO;
import eu.arrowhead.dto.TranslationDataModelTranslationDataDescriptorDTO;

@Service
public class DataModelTranslatorEngine {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Value(InterfaceTranslatorToGenericMQTTConstants.$DATA_MODEL_TRANSLATOR_GET_RESULT_TRIES_WD)
	private int defaultGetResultRetries;

	@Value(InterfaceTranslatorToGenericMQTTConstants.$DATA_MODEL_TRANSLATOR_GET_RESULT_WAIT_WD)
	private long defaultGetResultWait;

	@Autowired
	private DataModelTranslatorDriver dmDriver;

	@Autowired
	private BridgeStore bridgeStore;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<String, String> translate(final UUID bridgeId, final TranslationDataModelTranslationDataDescriptorDTO translator, final String input, final Map<String, Object> settings) {
		logger.debug("DataModelTranslatorEngine.translate started...");
		Assert.notNull(bridgeId, "bridgeId is null");
		Assert.notNull(translator, "translator is null");
		Assert.isTrue(!Utilities.isEmpty(input), "input is missing");

		final int tries = getSettingValue(settings, Integer.class, Constants.SETTING_KEY_DATA_MODEL_TRANSLATOR_GET_RESULT_TRIES, defaultGetResultRetries);
		final long wait = getSettingValue(settings, Long.class, Constants.SETTING_KEY_DATA_MODEL_TRANSLATOR_GET_RESULT_WAIT, defaultGetResultWait);

		final String taskId = dmDriver.initTranslation(translator, input);

		for (int i = 0; i < tries; ++i) {
			if (!bridgeStore.containsBridgeId(bridgeId)) {
				// abort happened during the translation
				dmDriver.abortTranslation(taskId, translator.interfaceProperties());
				throw new ExternalServerError("Translation bridge is aborted");
			}

			final DataModelTranslationResultResponseDTO response = dmDriver.getTranslationResult(taskId, translator.interfaceProperties());

			switch (response.status()) {
			case PENDING:
			case IN_PROGRESS:
				try {
					Thread.sleep(wait);
				} catch (final InterruptedException __) {
					// nothing to do
				}
				break;
			case DONE:
				return Pair.of(
						response.result(),
						response.mimeType());
			case ERROR:
				throw new ExternalServerError(response.result());
			default:
				throw new IllegalArgumentException("Unknown status: " + response.status().toString());
			}
		}

		// it takes too much time
		dmDriver.abortTranslation(taskId, translator.interfaceProperties());
		throw new ExternalServerError("Data model translator did not respond in time");
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private <T> T getSettingValue(final Map<String, Object> settings, final Class<T> type, final String key, final T defaultValue) {
		logger.debug("DataModelTranslatorEngine.getSettingValue started...");

		final Object value = settings.get(key);

		return (type.isInstance(value) ? (T) value : defaultValue);
	}
}