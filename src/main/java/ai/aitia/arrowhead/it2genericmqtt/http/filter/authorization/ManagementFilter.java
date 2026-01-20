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
package ai.aitia.arrowhead.it2genericmqtt.http.filter.authorization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import ai.aitia.arrowhead.Constants;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTConstants;
import ai.aitia.arrowhead.it2genericmqtt.InterfaceTranslatorToGenericMQTTSystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.common.security.SecurityUtilities.CommonNameAndType;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ManagementFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	private static final int AES_KEY_MIN_SIZE = 16; // 128 bits
	private static final String AES_KEY_ALGORITHM = "AES";
	private static final String BASE64_SELF_CONTAINED_TOKEN_DELIMITER_REGEXP = Constants.COMPOSITE_ID_DELIMITER_REGEXP;
	private static final int TOKEN_CONTENT_PARTS_NUM = 7;

	@Autowired
	private InterfaceTranslatorToGenericMQTTSystemInfo sysInfo;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNormalizer;

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	private Map<String, Object> arrowheadContext;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		logger.debug("ManagementFilter.doFilterInternal started...");

		try {
			final String requestTarget = request.getRequestURL().toString();
			if (requestTarget.contains(InterfaceTranslatorToGenericMQTTConstants.HTTP_API_BRIDGE_MANAGEMENT_PATH)) {
				boolean allowed = false;

				if (sysInfo.getAuthenticationPolicy() == AuthenticationPolicy.CERTIFICATE) {
					allowed = checkCertificate(request);
				} else if (sysInfo.shouldTokenUsed()) {
					allowed = checkToken(request);
				} else {
					// system does not care about security
					allowed = true;
				}

				if (!allowed) {
					throw new ForbiddenException("Requester has no management permission", requestTarget);
				}
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private boolean checkCertificate(final HttpServletRequest request) {
		logger.debug("ManagementFilter.checkCertificate started...");

		final String requestTarget = Utilities.stripEndSlash(request.getRequestURL().toString());
		final CommonNameAndType requesterData = SecurityUtilities.getIdentificationDataFromRequest(request);
		if (requesterData == null) {
			log.error("Unauthenticated access attempt: {}", requestTarget);
			throw new AuthException("Unauthenticated access attempt: " + requestTarget);
		}

		final String clientName = systemNameNormalizer.normalize(SecurityUtilities.getClientNameFromClientCN(requesterData.commonName()));

		return Constants.SYS_NAME_TRANSLATION_MANAGER.equals(clientName);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	private boolean checkToken(final HttpServletRequest request) {
		logger.debug("ManagementFilter.checkToken started...");

		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (Utilities.isEmpty(authHeader)) {
			throw new AuthException("No authorization header has been provided");
		}

		String[] split = authHeader.trim().split(" ");
		if (split.length != 2 || !split[0].equals(Constants.AUTHENTICATION_SCHEMA)) {
			throw new AuthException("Invalid authorization header");
		}

		final String token = split[1].trim();
		final String initVector = arrowheadContext.get(InterfaceTranslatorToGenericMQTTConstants.KEY_INITIALIZATION_VECTOR).toString();
		try {
			final String rawToken = Utilities.isEmpty(initVector)
					? token
					: decrypt_AES_CBC_PKCS5P_IV(token, initVector, sysInfo.getTokenEncryptionKey());

			final String content = new String(Base64.getUrlDecoder().decode(rawToken), StandardCharsets.ISO_8859_1);
			final String[] contentSplit = content.trim().split(BASE64_SELF_CONTAINED_TOKEN_DELIMITER_REGEXP);
			if (contentSplit.length < TOKEN_CONTENT_PARTS_NUM) {
				throw new AuthException("Invalid authorization header");
			}

			final String expiryStr = contentSplit[6];
			if (!Utilities.isEmpty(expiryStr)) {
				final ZonedDateTime expiry = Utilities.parseUTCStringToZonedDateTime(expiryStr);
				if (expiry.isBefore(Utilities.utcNow())) {
					// expired token
					return false;
				}
			}

			final String consumer = systemNameNormalizer.normalize(contentSplit[1]);
			final String provider = systemNameNormalizer.normalize(contentSplit[2]);
			final String target = serviceDefNormalizer.normalize(contentSplit[3]);
			final String targetType = contentSplit[5].trim().toUpperCase();

			return Constants.SYS_NAME_TRANSLATION_MANAGER.equals(consumer)
					&& sysInfo.getSystemName().equals(provider)
					&& Constants.SERVICE_DEF_INTERFACE_BRIDGE_MANAGEMENT.equals(target)
					&& AuthorizationTargetType.SERVICE_DEF.name().equals(targetType);
		} catch (final InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new AuthException("Invalid authorization header");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodName")
	private String decrypt_AES_CBC_PKCS5P_IV(final String encryptedDataBase64, final String ivBase64, final String key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(encryptedDataBase64), "encryptedDataBase64 is empty");
		Assert.isTrue(!Utilities.isEmpty(ivBase64), "ivBase64 is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");

		final SecretKeySpec keySpec = getAESKeySpecFromString(key);
		final byte[] iv = Base64.getDecoder().decode(ivBase64);
		final IvParameterSpec ivSpec = new IvParameterSpec(iv);
		final Cipher cipher = Cipher.getInstance(InterfaceTranslatorToGenericMQTTConstants.AES_CBC_ALGORITHM_IV_BASED);

		cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
		final byte[] encryptedBytes = Base64.getDecoder().decode(encryptedDataBase64);
		final byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

		return new String(decryptedBytes, StandardCharsets.ISO_8859_1);
	}

	//-------------------------------------------------------------------------------------------------
	private SecretKeySpec getAESKeySpecFromString(final String key) {
		final byte[] keyBytes = key.getBytes();
		Assert.isTrue(keyBytes.length >= AES_KEY_MIN_SIZE, "Key must be minimum " + AES_KEY_MIN_SIZE + " bytes long");

		return new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);
	}
}