/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.rest;

import javax.ejb.EJBException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.rhq.enterprise.server.resource.ResourceNotFoundException;

/**
 * Map a NotFoundException to a HTTP response with respective error message
 * @author Heiko W. Rupp
 */
@Provider
public class CustomExceptionMapper implements ExceptionMapper<EJBException> {


    @Override
    public Response toResponse(EJBException e) {

        Throwable cause = e.getCause();
        if (cause !=null) {
            if (cause instanceof StuffNotFoundException)
                return Response.status(Response.Status.NOT_FOUND)
                .entity(cause.getMessage())
                .build();
            else if (cause instanceof ResourceNotFoundException)
                return Response.status(Response.Status.NOT_FOUND)
                .entity(cause.getMessage())
                .build();
            else if (cause instanceof ParameterMissingException)
                return Response.status(Response.Status.NOT_ACCEPTABLE)
                .entity(cause.getMessage())
                .build();
            else
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(cause.getMessage())
                        .build();
        }
        throw e;
    }
}
