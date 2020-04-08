/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.jersey.resource;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.ExtendedConverters;
import org.apache.ofbiz.jersey.core.HttpResponseStatus;
import org.apache.ofbiz.jersey.response.Error;
import org.apache.ofbiz.jersey.response.Success;
import org.apache.ofbiz.jersey.util.QueryParamStringConverter;
import org.apache.ofbiz.service.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Path("/v1/entities")
@Provider
//@Secured
public class EntityResource {

	public static final String MODULE = EntityResource.class.getName();

	@Context
	private HttpServletRequest httpRequest;

	@Context
	private ServletContext servletContext;
	
	@Path("/import")
	@PUT
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON)
	public Response entityImport(String importXml) throws IOException {
		ResponseBuilder builder = null;
		LocalDispatcher dispatcher = (LocalDispatcher) servletContext.getAttribute("dispatcher");
		String fullText = null;
		if (importXml != null && importXml.startsWith("<entity-engine-xml>") && importXml.endsWith("</entity-engine-xml>")) {
			fullText = importXml;
		} else {
			fullText = "<entity-engine-xml>" + importXml + "</entity-engine-xml>";
		}
		GenericValue userLogin = (GenericValue)httpRequest.getAttribute("userLogin");
		Map<String, Object> result = null;
		try {
			result = dispatcher.runSync("entityImport", UtilMisc.toMap("fulltext", fullText, "userLogin", userLogin));
		} catch (GenericServiceException e) {
			Debug.logError(e, "Exception thrown while running entityImport service: ", MODULE);
			String errMsg = UtilProperties.getMessage("JerseyUiLabels", "api.error.import_entity", httpRequest.getLocale());
			throw new RuntimeException(errMsg);
		}
		
		if (ServiceUtil.isError(result)) {
			Error error = new Error(HttpResponseStatus.UNPROCESSABLE_ENTITY.getStatusCode(), HttpResponseStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(), (String)result.get(ModelService.ERROR_MESSAGE));
			builder = Response.status(HttpResponseStatus.UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON).entity(error);
		} else {
			String msg = UtilProperties.getMessage("JerseyUiLabels", "api.success.import_entity", httpRequest.getLocale());
			Success success = new Success(Response.Status.OK.getStatusCode(), Response.Status.OK.getReasonPhrase(), msg);
			builder = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(success);
		}
		return builder.build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listEntities() throws IOException, GenericEntityException {
		ResponseBuilder builder = null;
		Delegator delegator = (Delegator) servletContext.getAttribute("delegator");
		ModelReader reader = delegator.getModelReader();
		TreeSet<String> entities = new TreeSet<String>(reader.getEntityNames());
		builder = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(entities);
		return builder.build();
	}
	
	@GET
	@Path("/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEntity(@PathParam(value = "name") String entityName, @Context UriInfo allUri) throws IOException, GenericEntityException {
		ResponseBuilder builder = null;
		MultivaluedMap<String, String> mpAllQueParams = allUri.getQueryParameters();
		Delegator delegator = (Delegator) servletContext.getAttribute("delegator");
		ModelEntity model = delegator.getModelReader().getModelEntity(entityName);
		Map<String, Object> secondary = mpAllQueParams.entrySet().stream().map(x -> new AbstractMap.SimpleEntry<>(x.getKey(), QueryParamStringConverter.convert(x.getValue().get(0), model.getField(x.getKey()).getType()))).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
//		model.getField("placeholder").getType()
		List<GenericValue> allEntities = EntityQuery.use(delegator).from(entityName).where(secondary).queryList();
		builder = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(allEntities);
		return builder.build();
	}

}
