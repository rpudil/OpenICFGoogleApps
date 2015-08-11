/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openicf.connectors.googleapps;

import static org.forgerock.openicf.connectors.googleapps.GoogleAppsConnector.*;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.Uid;

import com.google.api.services.licensing.Licensing;
import com.google.api.services.licensing.model.LicenseAssignment;
import com.google.api.services.licensing.model.LicenseAssignmentInsert;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class LicenseAssignmentsHandler {

    /**
     * Setup logging for the {@link LicenseAssignmentsHandler}.
     */
    private static final Log logger = Log.getLog(LicenseAssignmentsHandler.class);

    // /////////////
    //
    // LicenseAssignment
    // https://developers.google.com/admin-sdk/licensing/v1/reference/licenseAssignments
    //
    // /////////////

    public static ObjectClassInfo getLicenseAssignmentClassInfo() {
        // @formatter:off
            /*
            {
			  "kind": "licensing#licenseAssignment",
			  "etags": etag,
			  "selfLink": string,
			  "userId": string,
			  "productId": string,
			  "skuId": string
			}
            */
        // @formatter:on
        ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
        builder.setType(LICENSE_ASSIGNMENT.getObjectClassValue());
        // productId
        builder.addAttributeInfo(AttributeInfoBuilder.define(PRODUCT_ID_ATTR).setRequired(true)
                .build());
        // skuId
        builder.addAttributeInfo(AttributeInfoBuilder.define(SKU_ID_ATTR).setRequired(true).build());
        // userId
        builder.addAttributeInfo(AttributeInfoBuilder.define(USER_ID_ATTR).setRequired(true)
                .build());

        // optional
        builder.addAttributeInfo(AttributeInfoBuilder.define(SELF_LINK_ATTR).setCreateable(false)
                .setUpdateable(false).build());

        return builder.build();
    }

    public static Licensing.LicenseAssignments.Insert createLicenseAssignment(
            Licensing.LicenseAssignments service, AttributesAccessor attributes) {

        String productId = attributes.findString(PRODUCT_ID_ATTR);
        if (StringUtil.isBlank(productId)) {
            throw new InvalidAttributeValueException(
                    "Missing required attribute 'productId'. A product's unique identifier. Required when creating a LicenseAssignment.");
        }

        String skuId = attributes.findString(SKU_ID_ATTR);
        if (StringUtil.isBlank(skuId)) {
            throw new InvalidAttributeValueException(
                    "Missing required attribute 'skuId'. A product SKU's unique identifier. Required when creating a LicenseAssignment.");
        }

        String userId = attributes.findString(USER_ID_ATTR);
        if (StringUtil.isBlank(userId)) {
            throw new InvalidAttributeValueException(
                    "Missing required attribute 'userId'. The user's current primary email address. Required when creating a LicenseAssignment.");
        }

        return createLicenseAssignment(service, productId, skuId, userId);
    }

    public static Licensing.LicenseAssignments.Insert createLicenseAssignment(
            Licensing.LicenseAssignments service, String productId, String skuId, String userId) {
        try {
            LicenseAssignmentInsert resource = new LicenseAssignmentInsert();
            resource.setUserId(userId);
            return service.insert(productId, skuId, resource).setFields(PRODUCT_ID_SKU_ID_USER_ID);
            // } catch (HttpResponseException e){
        } catch (IOException e) {
            logger.warn(e, "Failed to initialize LicenseAssignments#Insert");
            throw ConnectorException.wrap(e);
        }
    }

    public static final Pattern LICENSE_NAME_PATTERN =
            Pattern.compile("(?i)(Google-Coordinate|Google-Drive-storage|Google-Vault)\\/sku\\/(Google-Coordinate|Google-Drive-storage-20GB|Google-Drive-storage-50GB|Google-Drive-storage-200GB|Google-Drive-storage-400GB|Google-Drive-storage-1TB|Google-Drive-storage-2TB|Google-Drive-storage-4TB|Google-Drive-storage-8TB|Google-Drive-storage-16TB|Google-Vault)\\/user\\/(.+)");

    public static Licensing.LicenseAssignments.Patch updateLicenseAssignment(
            Licensing.LicenseAssignments service, String groupKey, AttributesAccessor attributes) {
        LicenseAssignment content = null;

        Matcher name = LICENSE_NAME_PATTERN.matcher(groupKey);
        if (!name.matches()) {
            throw new UnknownUidException("Unrecognised id");
        }

        String productId = name.group(0);
        String oldSkuId = name.group(1);
        String userId = name.group(2);

        Attribute skuId = attributes.find(SKU_ID_ATTR);
        if (null != skuId) {
            content = new LicenseAssignment();
            content.setSkuId(AttributeUtil.getStringValue(skuId));
        }

        if (null == content) {
            return null;
        }
        try {
            if (oldSkuId.equalsIgnoreCase(content.getSkuId())) {
                // There is nothing to change
                return null;
            } else {
                return service.patch(productId, oldSkuId, userId, content);
            }
            // } catch (HttpResponseException e){
        } catch (IOException e) {
            logger.warn(e, "Failed to initialize LicenseAssignments#Patch");
            throw ConnectorException.wrap(e);
        }
    }

    public static Licensing.LicenseAssignments.Delete deleteLicenseAssignment(
            Licensing.LicenseAssignments service, String groupKey) {

        Matcher name = LICENSE_NAME_PATTERN.matcher(groupKey);
        if (!name.matches()) {
            throw new UnknownUidException("Unrecognised id");
        }

        String productId = name.group(0);
        String skuId = name.group(1);
        String userId = name.group(2);

        try {
            return service.delete(productId, skuId, userId);
            // } catch (HttpResponseException e){
        } catch (IOException e) {
            logger.warn(e, "Failed to initialize LicenseAssignments#Delete");
            throw ConnectorException.wrap(e);
        }
    }
 
    public static ConnectorObject fromLicenseAssignment(LicenseAssignment content) {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(LICENSE_ASSIGNMENT);
        Uid uid = generateLicenseAssignmentId(content);
        builder.setUid(uid);
        builder.setName(uid.getUidValue());

        builder.addAttribute(AttributeBuilder.build(SELF_LINK_ATTR, content.getSelfLink()));
        builder.addAttribute(AttributeBuilder.build(USER_ID_ATTR, content.getUserId()));
        builder.addAttribute(AttributeBuilder.build(PRODUCT_ID_ATTR, content.getProductId()));
        builder.addAttribute(AttributeBuilder.build(SKU_ID_ATTR, content.getSkuId()));

        return builder.build();
    }

    public static Uid generateLicenseAssignmentId(LicenseAssignment content) {
        String id =
                content.getProductId() + "/sku/" + content.getSkuId() + "/user/"
                        + content.getUserId();
        if (null != content.getEtags()) {
            return new Uid(id, content.getEtags());
        } else {
            return new Uid(id);
        }
    }

}
