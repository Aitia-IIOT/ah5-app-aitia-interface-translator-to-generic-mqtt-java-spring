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
package ai.aitia.arrowhead.it2genericmqtt.api.mqtt.utils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTSystemInfo;
import ai.aitia.arrowhead.it2genericmqtt.api.mqtt.DynamicMqttCallback;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;

@Component
public class DynamicMqttClient {

	//=================================================================================================
	// members

	private static final String SSL_PREFIX = Constants.SSL + "://";
	private static final String TCP_PREFIX = Constants.TCP + "://";
	private static final String SSL_KEY_MANAGER_FACTORY_ALGORITHM = "ssl.KeyManagerFactory.algorithm";
	private static final String SSL_TRUST_MANAGER_FACTORY_ALGORITHM = "ssl.TrustManagerFactory.algorithm";
	private static final String TLS_VERSION = "TLSv1.2";

	private final Logger logger = LogManager.getLogger(getClass());

	private MqttClient client = null;

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private InterfaceTranslatorToGenericMQTTSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void initialize() throws MqttException {
		logger.debug("DynamicMqttClient.initialize started...");

		final MqttClient client = createAndConnect();
		final DynamicMqttCallback callback = appContext.getBean(DynamicMqttCallback.class);
		client.setCallback(callback);
		this.client = client;
	}

	//-------------------------------------------------------------------------------------------------
	public void destroy() throws MqttException {
		logger.debug("DynamicMqttClient.destroy started...");

		client.close();
		client = null;
	}

	//-------------------------------------------------------------------------------------------------
	public void subscribe(final String topic) throws MqttException {
		logger.debug("DynamicMqttClient.subscribe started...");

		if (client != null
				&& !Utilities.isEmpty(topic)) {
			client.subscribe(topic.trim());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void unsubscribe(final String topic) throws MqttException {
		logger.debug("DynamicMqttClient.unsubscribe started...");

		if (client != null
				&& !Utilities.isEmpty(topic)) {
			client.unsubscribe(topic.trim());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public String getServerURI() {
		logger.debug("DynamicMqttClient.getServerURI started...");

		return client != null ? client.getServerURI() : "";
	}

	//-------------------------------------------------------------------------------------------------
	public void publish(final String topic, final MqttMessage msg) throws MqttPersistenceException, MqttException {
		logger.debug("DynamicMqttClient.publish started...");

		if (client != null
				&& !Utilities.isEmpty(topic)
				&& msg != null) {
			client.publish(topic.trim(), msg);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private MqttClient createAndConnect() throws MqttException {
		logger.debug("createAndConnect started...");

		final String serverURI = (sysInfo.isSslEnabled() ? SSL_PREFIX : TCP_PREFIX)
				+ sysInfo.getMqttBrokerAddress()
				+ ":"
				+ sysInfo.getMqttBrokerPort();

		final MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		options.setUserName(sysInfo.getSystemName());
		if (!Utilities.isEmpty(sysInfo.getMqttClientPassword())) {
			options.setPassword(sysInfo.getMqttClientPassword().toCharArray());
		}

		if (sysInfo.isSslEnabled()) {
			try {
				options.setSocketFactory(sslSettings());
			} catch (final Exception ex) {
				logger.debug(ex);
				logger.error("Creating SSL context is failed. Reason: " + ex.getMessage());
				throw new MqttException(MqttException.REASON_CODE_SSL_CONFIG_ERROR, ex);
			}
		}

		final MqttClient client = new MqttClient(serverURI, InterfaceTranslatorToGenericMQTTConstants.MQTT_BRIDGE_BROKER_CONNECT_ID);
		client.connect(options);

		return client;
	}

	//-------------------------------------------------------------------------------------------------
	private SSLSocketFactory sslSettings() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {
		logger.debug("sslSettings started");

		final SSLProperties sslProperties = sysInfo.getSslProperties();
		final String messageNotDefined = " is not defined";
		Assert.isTrue(!Utilities.isEmpty(sslProperties.getKeyStoreType()), Constants.SERVER_SSL_KEY__STORE__TYPE + messageNotDefined);
		Assert.notNull(sslProperties.getKeyStore(), Constants.SERVER_SSL_KEY__STORE + messageNotDefined);
		Assert.isTrue(sslProperties.getKeyStore().exists(), Constants.SERVER_SSL_KEY__STORE + " file is not found");
		Assert.notNull(sslProperties.getKeyStorePassword(), Constants.SERVER_SSL_KEY__STORE__PASSWORD + messageNotDefined);
		Assert.notNull(sslProperties.getKeyPassword(), Constants.SERVER_SSL_KEY__PASSWORD + messageNotDefined);
		Assert.notNull(sslProperties.getTrustStore(), Constants.SERVER_SSL_TRUST__STORE + messageNotDefined);
		Assert.isTrue(sslProperties.getTrustStore().exists(), Constants.SERVER_SSL_TRUST__STORE + " file is not found");
		Assert.notNull(sslProperties.getTrustStorePassword(), Constants.SERVER_SSL_TRUST__STORE__PASSWORD + messageNotDefined);

		final KeyStore keyStore = KeyStore.getInstance(sslProperties.getKeyStoreType());
		keyStore.load(sslProperties.getKeyStore().getInputStream(), sslProperties.getKeyStorePassword().toCharArray());
		final String kmfAlgorithm = System.getProperty(SSL_KEY_MANAGER_FACTORY_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());
		final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(kmfAlgorithm);
		keyManagerFactory.init(keyStore, sslProperties.getKeyStorePassword().toCharArray());

		final KeyStore trustStore = KeyStore.getInstance(sslProperties.getKeyStoreType());
		trustStore.load(sslProperties.getTrustStore().getInputStream(), sslProperties.getTrustStorePassword().toCharArray());
		final String tmfAlgorithm = System.getProperty(SSL_TRUST_MANAGER_FACTORY_ALGORITHM, TrustManagerFactory.getDefaultAlgorithm());
		final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
		trustManagerFactory.init(trustStore);

		final SSLContext sslContext = SSLContext.getInstance(TLS_VERSION);
		sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

		return sslContext.getSocketFactory();
	}
}