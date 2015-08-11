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
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.AttributesAccessor;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.PredefinedAttributeInfos;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.Uid;

import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.OrgUnit;

/**
 * OrgunitsHandler is a util class to cover all Organizations Unit related
 * operations.
 *
 * @author Laszlo Hordos
 */
public class OrgunitsHandler {

    /**
     * Setup logging for the {@link OrgunitsHandler}.
     */
    private static final Log logger = Log.getLog(OrgunitsHandler.class);

    // /////////////
    //
    // ORGUNIT
    // https://developers.google.com/admin-sdk/directory/v1/reference/orgunits
    //
    // /////////////

    public static ObjectClassInfo getOrgunitClassInfo() {
        // @formatter:off
            /*
            {
			  "kind": "admin#directory#orgUnit",
			  "etag": etag,
			  "name": string,
			  "description": string,
			  "orgUnitPath": string,
			  "parentOrgUnitPath": string,
			  "blockInheritance": boolean
			}
            */
        // @formatter:on
        ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
        builder.setType(ORG_UNIT.getObjectClassValue());
        builder.setContainer(true);
        // primaryEmail
        builder.addAttributeInfo(Name.INFO);
        // parentOrgUnitPath
        builder.addAttributeInfo(AttributeInfoBuilder.define(PARENT_ORG_UNIT_PATH_ATTR)
                .setRequired(true).build());

        // optional
        builder.addAttributeInfo(PredefinedAttributeInfos.DESCRIPTION);
        builder.addAttributeInfo(AttributeInfoBuilder.build(ORG_UNIT_PATH_ATTR));
        builder.addAttributeInfo(AttributeInfoBuilder.build(BLOCK_INHERITANCE_ATTR, Boolean.class));

        return builder.build();
    }

    public static Directory.Orgunits.Insert createOrgunit(Directory.Orgunits service,
            AttributesAccessor attributes) {

        OrgUnit resource = new OrgUnit();

        resource.setName(GoogleAppsUtil.getName(attributes.getName()));

        // parentOrgUnitPath The organization unit's parent path. For example,
        // /corp/sales is the parent path for /corp/sales/sales_support
        // organization unit.
        String parentOrgUnitPath = attributes.findString(PARENT_ORG_UNIT_PATH_ATTR);
        if (StringUtil.isNotBlank(parentOrgUnitPath)) {
            if (parentOrgUnitPath.charAt(0) != '/') {
                parentOrgUnitPath = "/" + parentOrgUnitPath;
            }
            resource.setParentOrgUnitPath(parentOrgUnitPath);
        } else {
            throw new InvalidAttributeValueException(
                    "Missing required attribute 'parentOrgUnitPath'. The organization unit's parent path. Required when creating an orgunit.");
        }

        // Optional
        resource.setBlockInheritance(attributes.findBoolean(BLOCK_INHERITANCE_ATTR));
        resource.setDescription(attributes.findString(PredefinedAttributes.DESCRIPTION));

        try {
            return service.insert(MY_CUSTOMER_ID, resource).setFields(ORG_UNIT_PATH_ETAG);
            // } catch (HttpResponseException e){
        } catch (IOException e) {
            logger.warn(e, "Failed to initialize Groups#Insert");
            throw ConnectorException.wrap(e);
        }
    }

    public static Directory.Orgunits.Patch updateOrgunit(Directory.Orgunits service,
            String orgUnitPath, AttributesAccessor attributes) {
        OrgUnit resource = null;

        Name name = attributes.getName();
        if (null != name) {
            resource = new OrgUnit();
            // Rename the object
            resource.setName(name.getNameValue());
        }

        Attribute parentOrgUnitPath = attributes.find(PARENT_ORG_UNIT_PATH_ATTR);
        if (null != parentOrgUnitPath) {
            if (null == resource) {
                resource = new OrgUnit();
            }
            String stringValue = AttributeUtil.getStringValue(parentOrgUnitPath);
            if (StringUtil.isBlank(stringValue)) {
                throw new InvalidAttributeValueException(
                        "Invalid attribute 'parentOrgUnitPath'. The organization unit's parent path. Can not be blank when updating an orgunit.");

            }
            if (stringValue.charAt(0) != '/') {
                stringValue = "/" + stringValue;
            }
            resource.setParentOrgUnitPath(stringValue);
        }

        Attribute description = attributes.find(PredefinedAttributes.DESCRIPTION);
        if (null != description) {
            if (null == resource) {
                resource = new OrgUnit();
            }
            String stringValue = AttributeUtil.getStringValue(description);
            if (null == stringValue) {
                stringValue = EMPTY_STRING;
            }
            resource.setDescription(stringValue);
        }

        Attribute blockInheritance = attributes.find(BLOCK_INHERITANCE_ATTR);
        if (null != blockInheritance) {
            if (null == resource) {
                resource = new OrgUnit();
            }
            Boolean booleanValue = AttributeUtil.getBooleanValue(blockInheritance);
            if (null == booleanValue) {
                // The default value is false
                booleanValue = Boolean.FALSE;
            }
            resource.setBlockInheritance(booleanValue);
        }

        if (null == resource) {
            return null;
        }
        try {
            // Full path of the organization unit
            return service.patch(MY_CUSTOMER_ID, CollectionUtil.newList(orgUnitPath), resource)
                    .setFields(ORG_UNIT_PATH_ETAG);
            // } catch (HttpResponseException e){
        } catch (IOException e) {
            logger.warn(e, "Failed to initialize Orgunits#Patch");
            throw ConnectorException.wrap(e);
        }
    }

    public static ConnectorObject fromOrgunit(OrgUnit content, Set<String> attributesToGet) {
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(ORG_UNIT);

        builder.setUid(generateOrgUnitId(content));
        builder.setName(content.getName());

        // Optional

        if (null == attributesToGet || attributesToGet.contains(PredefinedAttributes.DESCRIPTION)) {
            builder.addAttribute(AttributeBuilder.build(PredefinedAttributes.DESCRIPTION, content
                    .getDescription()));
        }
        if (null == attributesToGet || attributesToGet.contains(ORG_UNIT_PATH_ATTR)) {
            builder.addAttribute(AttributeBuilder.build(ORG_UNIT_PATH_ATTR, content
                    .getOrgUnitPath()));
        }
        if (null == attributesToGet || attributesToGet.contains(PARENT_ORG_UNIT_PATH_ATTR)) {
            builder.addAttribute(AttributeBuilder.build(PARENT_ORG_UNIT_PATH_ATTR, content
                    .getParentOrgUnitPath()));
        }
        if (null == attributesToGet || attributesToGet.contains(BLOCK_INHERITANCE_ATTR)) {
            builder.addAttribute(AttributeBuilder.build(BLOCK_INHERITANCE_ATTR, content
                    .getBlockInheritance()));
        }

        return builder.build();
    }

    public static Uid generateOrgUnitId(OrgUnit content) {
        Uid uid = null;
        String orgUnitPath = content.getOrgUnitPath();
        if (orgUnitPath.startsWith("/")) {
            orgUnitPath = orgUnitPath.substring(1);
        }

        if (null != content.getEtag()) {
            uid = new Uid(orgUnitPath, content.getEtag());
        } else {
            uid = new Uid(orgUnitPath);
        }
        return uid;
    }

}
