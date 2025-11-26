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
package ai.aitia.arrowhead.it2genericmqtt.quartz;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.service.model.BridgeStore;
import ai.aitia.arrowhead.it2genericmqtt.service.model.NormalizedTranslationBridgeModel;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.TranslationReportRequestDTO;
import eu.arrowhead.dto.enums.TranslationBridgeEventState;
import jakarta.annotation.Resource;

@Component
@DisallowConcurrentExecution
public class BridgeClosingJob implements Job {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Value(InterfaceTranslatorToGenericMQTTConstants.$BRIDGE_INACTIVITY_THRESHOLD_WD)
	private int threshold;

	@Autowired
	private BridgeStore bridgeStore;

	@Resource(name = InterfaceTranslatorToGenericMQTTConstants.REPORT_QUEUE)
	private BlockingQueue<TranslationReportRequestDTO> reportQueue;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		logger.debug("Login job called...");

		try {
			final ZonedDateTime now = Utilities.utcNow();
			final ZonedDateTime thresholdTime = now.minusMinutes(threshold);
			final List<NormalizedTranslationBridgeModel> list = bridgeStore.getBridgeModelsWithOlderActivityThan(thresholdTime);
			list.forEach(model -> {
				bridgeStore.removeByBridgeId(model.bridgeId());
				sendClosedReport(model);
			});
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void sendClosedReport(final NormalizedTranslationBridgeModel model) {
		logger.debug("sendClosedReport started...");

		final TranslationReportRequestDTO report = new TranslationReportRequestDTO(
				model.bridgeId().toString(),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow()),
				TranslationBridgeEventState.INTERNAL_CLOSED.name(),
				null);

		reportQueue.add(report);
	}
}