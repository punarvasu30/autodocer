package com.autodocer.AiDescription;

import com.autodocer.DTO.AiGenerationResult;
import com.autodocer.DTO.EndpointContext;

public interface AiDescriptionService {

    AiGenerationResult generateDescription(EndpointContext context) ;
}
