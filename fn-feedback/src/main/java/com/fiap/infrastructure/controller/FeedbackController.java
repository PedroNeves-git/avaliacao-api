package com.fiap.infrastructure.controller;

import com.fiap.application.usecase.ProcessFeedbackUseCase;
import com.fiap.domain.dto.FeedbackRequest;
import com.fiap.domain.entity.Feedback;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/feedback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FeedbackController {

    @Inject
    ProcessFeedbackUseCase processFeedbackUseCase;

    @POST
    public Response feedback(@Valid FeedbackRequest request) {
        processFeedbackUseCase.execute(request);
        return Response.status(Response.Status.CREATED).build();
    }
}
