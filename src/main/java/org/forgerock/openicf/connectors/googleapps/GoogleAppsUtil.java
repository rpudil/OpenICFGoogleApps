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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import com.google.api.client.util.ArrayMap;
import com.google.api.client.util.Joiner;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class GoogleAppsUtil {

    public static String getName(Name name) {
        if (name != null) {
            String email = name.getNameValue();
            if (StringUtil.isBlank(email)) {
                throw new InvalidAttributeValueException("Required attribute __NAME__ is blank");
            }
            return email;
        }
        throw new InvalidAttributeValueException("Required attribute __NAME__ is missing");
    }

    public static String getStringValueWithDefault(Attribute source, String defaultTo) {
        Object value = AttributeUtil.getSingleValue(source);
        if (value instanceof String) {
            return (String) value;
        } else if (null != value) {
            throw new InvalidAttributeValueException("The " + source.getName()
                    + " attribute is not String value attribute. It has value with type "
                    + value.getClass().getSimpleName());
        }
        return defaultTo;
    }

    public static Boolean getBooleanValueWithDefault(Attribute source, Boolean defaultTo) {
        Object value = AttributeUtil.getSingleValue(source);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String && (value.toString().equalsIgnoreCase("true") || value.toString().equalsIgnoreCase("false"))) {
            return Boolean.valueOf(value.toString());
        } else if (null != value) {
            throw new InvalidAttributeValueException("The " + source.getName()
                    + " attribute is not Boolean value attribute. It has value with type "
                    + value.getClass().getSimpleName());
        }
        return defaultTo;
    }

    private static ArrayMap<String, Object> parseJson(String json) {
        JsonObject object = (JsonObject) new com.google.gson.JsonParser().parse(json);
        Set<Map.Entry<String, JsonElement>> set = object.entrySet();
        Iterator<Map.Entry<String, JsonElement>> iterator = set.iterator();
        ArrayMap<String, Object> map = new ArrayMap<String, Object>();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> entry = iterator.next();
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive()) {
                map.put(key, parseJson(value.toString()));
            } else {
                map.put(key, value.getAsString());
            }
        }
        return map;
    }

    public static Object getStructAttr(Attribute attr) {
        if (attr != null && attr.getValue() != null && !attr.getValue().toString().equals("null") && !attr.getValue().isEmpty() && attr.getValue().size() > 0) {
            ArrayList<Object> attrItems = new ArrayList();
            for (Object item : attr.getValue()) {
                if (item instanceof String) {
                    ArrayMap sit = new ArrayMap();
                    if (((String) item).startsWith("{") && ((String) item).endsWith("}")) {
                        sit = parseJson(item.toString());
                        if (!(sit.containsKey("value") && sit.get("value").equals("null"))) {
                            attrItems.add(sit);
                        }
                    } else {
                        sit.add("value", item.toString());
                    }
                    attrItems.add(sit);
                } else {
                    attrItems.add((ArrayMap) item);
                }
            }
            if (attrItems.size() > 1) {
                // remove duplicates
                Set<Object> attrItemsWithoutDups;
                attrItemsWithoutDups = new HashSet();
                attrItemsWithoutDups.addAll(attrItems);
                attrItems.clear();
                attrItems.addAll(attrItemsWithoutDups);
            }
            return attrItems;
        }
        return null;
    }
    
    public static Object structAttrToString(Collection values) {
        if (values != null && values.size() > 0) {
            ArrayList<Object> attrItems = new ArrayList();
            for (Object item : values.toArray()) {
                if (item instanceof ArrayMap) {
                    if (((ArrayMap) item).containsKey("value") && (((ArrayMap) item).get("value").equals("null") || ((ArrayMap) item).get("value").equals(""))) {
                        continue;
                    }
                    ArrayList<String> entryItems = new ArrayList();
                    for (Object entryAM : ((ArrayMap) item).entrySet()) {
                        Entry<Object, Object> entry = (Entry<Object, Object>) entryAM;
                        entryItems.add("\"" + entry.getKey().toString() + "\"=\"" + entry.getValue().toString() + "\"");
                    }
                    attrItems.add('{' + Joiner.on(',').join(entryItems) + '}');
                } else {
                    attrItems.add(item.toString());
                } 
            }
            return attrItems;
        }
        return null;
    }
    
}
