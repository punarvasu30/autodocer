package com.autodocer.AiDescription;

import com.autodocer.DTO.AiGenerationResult;
import com.autodocer.DTO.EndpointContext;
import com.autodocer.DTO.EndpointInfo;
import com.autodocer.DTO.ExampleInfo;

import java.util.List;

public interface AiDescriptionService {

    AiGenerationResult generateDescription(EndpointContext context) ;

    List<ExampleInfo> generateExamples(EndpointInfo endpoint,String serverUrl);
}
