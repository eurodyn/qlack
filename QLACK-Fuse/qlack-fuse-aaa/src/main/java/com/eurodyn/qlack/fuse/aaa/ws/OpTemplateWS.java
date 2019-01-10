package com.eurodyn.qlack.fuse.aaa.ws;

import com.eurodyn.qlack.fuse.aaa.dto.OpTemplateDTO;
import com.eurodyn.qlack.fuse.aaa.service.OpTemplateService;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author European Dynamics
 */
@Path("/template")
@Api(value = "Operation Template API")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Component
public class OpTemplateWS {

    @Context
    private HttpHeaders httpHeaders;

    private final OpTemplateService opTemplateService;

    @Autowired
    public OpTemplateWS(OpTemplateService opTemplateService) {
        this.opTemplateService = opTemplateService;
    }

    @POST
    @Path("/create")
    public String create(OpTemplateDTO opTemplate) throws ServiceException {
        return opTemplateService.createTemplate(opTemplate);
    }

    @GET
    @Path("/read/{id:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}}")
    public OpTemplateDTO read(@PathParam("id") String id) throws ServiceException {
        return opTemplateService.getTemplateByID(id);
    }

    @PUT
    @Path("/update")
    public void update(OpTemplateDTO opTemplate) throws ServiceException {
        opTemplateService.updateTemplate(opTemplate);
    }

    @DELETE
    @Path("/delete/{id:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}}")
    public void delete(@PathParam("id") String id) throws ServiceException {
        opTemplateService.deleteTemplateByID(id);
    }

    @GET
    @Path("/read/name/{templateName}")
    public OpTemplateDTO getOpTemplateByName(@PathParam("templateName") String templateName) throws ServiceException {
        return opTemplateService.getTemplateByName(templateName);
    }

    @DELETE
    @Path("/delete/name/{templateName}")
    public void deleteByName(@PathParam("templateName") String templateName) throws ServiceException {
        opTemplateService.deleteTemplateByName(templateName);
    }

    @POST
    @Path("/operation/add")
    public void addOperation(@QueryParam("templateId") String templateId, @QueryParam("operationName") String operationName, @QueryParam("isDeny") boolean isDeny) throws ServiceException {
        opTemplateService.addOperation(templateId, operationName, isDeny);
    }

    @POST
    @Path("/operation/access")
    public void getOperationAccess(@QueryParam("templateId") String templateId, @QueryParam("operationName") String operationName) throws ServiceException {
        opTemplateService.getOperationAccess(templateId, operationName);
    }
}