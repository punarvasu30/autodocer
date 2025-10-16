package com.autodocer;

import com.autodocer.DTO.*;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DocumentationParser {

    private final SchemaParser schemaParser;

    public DocumentationParser() {
        this.schemaParser = new SchemaParser();
    }

    public List<ControllerInfo> parse(ApplicationContext context) {
        System.out.println("--- [AutoDocER] Starting Enhanced Scan ---");
        List<ControllerInfo> controllerInfos = new ArrayList<>();
        Map<String, Object> controllers = context.getBeansWithAnnotation(RestController.class);

        if (controllers.isEmpty()) {
            System.out.println("--- [AutoDocER] No @RestController beans found.");
            return controllerInfos;
        }

        System.out.println("--- [AutoDocER] Found " + controllers.size() + " controllers.");

        for (Object controllerBean : controllers.values()) {
            Class<?> controllerClass = org.springframework.aop.support.AopUtils.getTargetClass(controllerBean);
            String controllerName = controllerClass.getSimpleName();

            String basePath = "";
            if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
                if (requestMapping.value().length > 0) {
                    basePath = requestMapping.value()[0];
                }
            }

            List<EndpointInfo> endpointInfos = new ArrayList<>();
            for (Method method : controllerClass.getDeclaredMethods()) {
                parseMethod(method, basePath).ifPresent(endpointInfos::add);
            }

            controllerInfos.add(new ControllerInfo(controllerName, basePath, endpointInfos));
        }

        System.out.println("--- [AutoDocER] Scan Complete ---");
        return controllerInfos;
    }

    private Optional<EndpointInfo> parseMethod(Method method, String basePath) {
        String httpMethod = null;
        String path = "";

        if (method.isAnnotationPresent(GetMapping.class)) {
            httpMethod = "GET";
            GetMapping annotation = method.getAnnotation(GetMapping.class);
            if (annotation.value().length > 0) path = annotation.value()[0];
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            httpMethod = "POST";
            PostMapping annotation = method.getAnnotation(PostMapping.class);
            if (annotation.value().length > 0) path = annotation.value()[0];
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            httpMethod = "PUT";
            PutMapping annotation = method.getAnnotation(PutMapping.class);
            if (annotation.value().length > 0) path = annotation.value()[0];
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            httpMethod = "DELETE";
            DeleteMapping annotation = method.getAnnotation(DeleteMapping.class);
            if (annotation.value().length > 0) path = annotation.value()[0];
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            httpMethod = "PATCH";
            PatchMapping annotation = method.getAnnotation(PatchMapping.class);
            if (annotation.value().length > 0) path = annotation.value()[0];
        }

        if (httpMethod == null) {
            return Optional.empty();
        }

        String fullPath = (basePath + "/" + path).replaceAll("/+", "/");
        if (fullPath.length() > 1 && fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }
        if (fullPath.isEmpty()) {
            fullPath = "/";
        }


        List<ParameterInfo> parameterInfos = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            Object paramType;
            // THE FIX: Decide whether to do a deep scan or just get the name
            if (isSimpleType(parameter.getType())) {
                paramType = parameter.getType().getSimpleName();
            } else {
                paramType = schemaParser.parseSchema(parameter.getType());
            }

            String sourceType = "Unknown";
            boolean isRequired = true;

            if (parameter.isAnnotationPresent(RequestBody.class)) {
                sourceType = "RequestBody";
                isRequired = parameter.getAnnotation(RequestBody.class).required();
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                sourceType = "PathVariable";
            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                sourceType = "RequestParam";
                isRequired = parameter.getAnnotation(RequestParam.class).required();
            }
            parameterInfos.add(new ParameterInfo(parameter.getName(), paramType, sourceType, isRequired));
        }

        Object responseType;
        // THE FIX: Decide whether to do a deep scan for the response type
        if (isSimpleType(method.getReturnType())) {
            responseType = method.getReturnType().getSimpleName();
        } else {
            responseType = schemaParser.parseSchema(method.getReturnType());
        }

        EndpointInfo endpointInfo = new EndpointInfo(method.getName(), httpMethod, fullPath, parameterInfos, responseType);
        return Optional.of(endpointInfo);
    }

    /**
     * Helper method to determine if a type is simple (and should not be scanned).
     */
    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.getPackageName().startsWith("java.")
                || type.equals(Void.TYPE);
    }
}

