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

import java.io.IOException;

import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;

import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public abstract class RequestResultHandler<G extends AbstractGoogleJsonClientRequest<T>, T, R> {

    public abstract R handleResult(G request, T value);

    public R handleNotFound(IOException e) {
        throw new UnknownUidException(e.getMessage(), e);
    }

    public R handleDuplicate(IOException e) {
        throw new AlreadyExistsException(e.getMessage(), e);
    }

    public R handleError(Throwable e) {
        throw ConnectorException.wrap(e);
    }
}
