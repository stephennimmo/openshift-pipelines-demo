package com.stephennimmo.helloworld.api;

import com.stephennimmo.helloworld.service.HelloService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/hello")
@Tag(name = "Hello", description = "Hello World operations")
public class HelloResource {

    @Inject
    HelloService helloService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Say hello", description = "Returns a hello world message")
    @APIResponse(responseCode = "200", description = "Successful hello response")
    public Response hello() {
        return Response.ok(new HelloResponse(helloService.hello())).build();
    }

}
