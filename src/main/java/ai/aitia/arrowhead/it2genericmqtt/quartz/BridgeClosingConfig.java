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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import jakarta.annotation.PostConstruct;

@Configuration
@EnableAutoConfiguration
public class BridgeClosingConfig {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Value(InterfaceTranslatorToGenericMQTTConstants.$BRIDGE_CLOSING_INTERVAL_WD)
	private long interval;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean(InterfaceTranslatorToGenericMQTTConstants.BRIDGE_CLOSING_JOB_FACTORY)
	JobDetailFactoryBean bridgeClosingJobDetail() {
		final JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
		jobDetailFactory.setJobClass(BridgeClosingJob.class);
		jobDetailFactory.setDescription("Searches for old bridges and closing them");
		jobDetailFactory.setDurability(true);
		return jobDetailFactory;
	}

	//-------------------------------------------------------------------------------------------------
	@Bean(InterfaceTranslatorToGenericMQTTConstants.BRIDGE_CLOSING_TRIGGER)
	SimpleTriggerFactoryBean bridgeClosingTrigger(@Qualifier(InterfaceTranslatorToGenericMQTTConstants.BRIDGE_CLOSING_JOB_FACTORY) final JobDetail job) {
		final SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setJobDetail(job);
		trigger.setRepeatInterval(interval);
		trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
		return trigger;
	}

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	public void init() {
		logger.info("Inactive bridge closer job is initialized.");
	}
}