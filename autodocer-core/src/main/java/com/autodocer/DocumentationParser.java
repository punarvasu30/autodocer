package com.autodocer;

import com.autodocer.AiDescription.AiDescriptionService;
import com.autodocer.DTO.AiGenerationResult;
import com.autodocer.annotations.ApiServers;
import com.autodocer.annotations.ServerInfo;
import com.autodocer.DTO.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

public class DocumentationParser {

    private final SchemaParser schemaParser;
    private final AiDescriptionService aiService;

    public DocumentationParser(AiDescriptionService aiService) {
        this.schemaParser = new SchemaParser();
        this.aiService = aiService;
    }

    public record ApiDocumentationResult(
            List<ServerData> servers,
            List<ControllerInfo> controllers
    ) {}

    public ApiDocumentationResult parse(ApplicationContext context) {
        System.out.println("--- [AutoDocER] Starting Full Scan (Controllers + Config) ---");

        List<ServerData> serverInfos = extractServerInfo(context);
        List<ControllerInfo> controllerInfos = new ArrayList<>();
        Map<String, Object> controllers = context.getBeansWithAnnotation(RestController.class);

        if (controllers.isEmpty()) {
            System.out.println("--- [AutoDocER] No @RestController beans found.");
        } else {
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
        }
        System.out.println("--- [AutoDocER] Scan Complete ---");
        return new ApiDocumentationResult(serverInfos, controllerInfos);
    }

    private List<ServerData> extractServerInfo(ApplicationContext context) {
        List<ServerData> servers = new ArrayList<>();
        Map<String, Object> mainAppBeans = context.getBeansWithAnnotation(SpringBootApplication.class);

        if (!mainAppBeans.isEmpty()) {
            String mainAppBeanName = mainAppBeans.keySet().iterator().next();
            Object mainAppBean = mainAppBeans.get(mainAppBeanName);

            // Get the actual class, not the proxy
            Class<?> mainAppClass = org.springframework.aop.support.AopUtils.getTargetClass(mainAppBean);

            // If it's still a CGLIB proxy, get the superclass
            if (mainAppClass.getName().contains("$$")) {
                mainAppClass = mainAppClass.getSuperclass();
            }

            if (mainAppClass != null) {
                System.out.println("--- [AutoDocER] Found main application class: " + mainAppClass.getName());

                // Use AnnotationUtils.findAnnotation which searches the class hierarchy
                ApiServers apiServersAnnotation = AnnotationUtils.findAnnotation(mainAppClass, ApiServers.class);

                if (apiServersAnnotation != null) {
                    System.out.println("--- [AutoDocER] Found @ApiServers annotation on " + mainAppClass.getSimpleName());
                    for (ServerInfo serverInfoAnnotation : apiServersAnnotation.value()) {
                        servers.add(new ServerData(serverInfoAnnotation.url(), serverInfoAnnotation.description()));
                        System.out.println("    -> Server Added: URL=" + serverInfoAnnotation.url() + ", Desc=" + serverInfoAnnotation.description());
                    }
                } else {
                    System.out.println("--- [AutoDocER] No @ApiServers annotation found on main application class: " + mainAppClass.getSimpleName());
                    servers.add(new ServerData("/", "Default Server (Relative Path)"));
                }
            } else {
                System.out.println("--- [AutoDocER] Could not determine type for main application bean: " + mainAppBeanName);
                servers.add(new ServerData("/", "Default Server (Relative Path)"));
            }

        } else {
            System.out.println("--- [AutoDocER] Could not find @SpringBootApplication class to scan for @ApiServers.");
            servers.add(new ServerData("/", "Default Server (Relative Path)"));
        }
        return servers;
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
            Object paramType = parseTypeWithGenerics(parameter.getParameterizedType(), parameter.getType());
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

        Object responseType = parseTypeWithGenerics(method.getGenericReturnType(), method.getReturnType());

        List<String> paramContextStrings = parameterInfos.stream()
                .map(p -> String.format("%s %s %s",
                        p.sourceType(),
                        formatTypeForContext(p.type()),
                        p.name()))
                .collect(Collectors.toList());

        String responseContextString = formatTypeForContext(responseType);

        EndpointContext context = new EndpointContext(
                method.getName(),
                httpMethod,
                fullPath,
                paramContextStrings,
                responseContextString
        );

        System.out.println("--- [AutoDocER] Generating description for: " + method.getName());
        AiGenerationResult aiResult = aiService.generateDescription(context);

        EndpointInfo endpointInfo = new EndpointInfo(
                method.getName(),
                httpMethod,
                fullPath,
                parameterInfos,
                responseType,
                aiResult.summary(),
                aiResult.description()
        );

        return Optional.of(endpointInfo);
    }

    private String formatTypeForContext(Object type) {
        if (type instanceof String typeName) {
            return typeName;
        }
        if (type instanceof SchemaInfo schema) {
            return schema.className();
        }
        if (type instanceof ArraySchemaInfo array) {
            return "List<" + formatTypeForContext(array.itemType()) + ">";
        }
        return "Object";
    }

    private Object parseTypeWithGenerics(Type genericType, Class<?> rawType) {
        if (isSimpleType(rawType)) {
            return rawType.getSimpleName();
        }
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type rawTypeFromParam = parameterizedType.getRawType();
            if (rawTypeFromParam instanceof Class<?>) {
                Class<?> rawClass = (Class<?>) rawTypeFromParam;
                if (Collection.class.isAssignableFrom(rawClass)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0) {
                        Type elementType = typeArguments[0];
                        Object itemTypeSchema;
                        if (elementType instanceof Class<?>) {
                            Class<?> elementClass = (Class<?>) elementType;
                            if (isSimpleType(elementClass)) {
                                itemTypeSchema = elementClass.getSimpleName();
                            } else {
                                itemTypeSchema = schemaParser.parseSchema(elementType);
                            }
                        } else {
                            itemTypeSchema = schemaParser.parseSchema(elementType);
                        }
                        System.out.println("    -> Detected Collection type: " + rawClass.getSimpleName() +
                                " with element type: " + elementType);
                        return new ArraySchemaInfo(itemTypeSchema);
                    }
                }
            }
        }
        if (rawType.isArray()) {
            Class<?> componentType = rawType.getComponentType();
            Object itemTypeSchema;
            if (isSimpleType(componentType)) {
                itemTypeSchema = componentType.getSimpleName();
            } else {
                itemTypeSchema = schemaParser.parseSchema(componentType);
            }
            System.out.println("    -> Detected Array type: " + rawType.getSimpleName());
            return new ArraySchemaInfo(itemTypeSchema);
        }
        return schemaParser.parseSchema(genericType);
    }

    private boolean isSimpleType(Class<?> type) {
        return type == null
                || type.isPrimitive()
                || type.getPackageName().equals("java.lang")
                || type.getPackageName().startsWith("java.time")
                || type.equals(Void.TYPE)
                || type.equals(Void.class)
                || java.util.Date.class.isAssignableFrom(type)
                || java.util.UUID.class.equals(type)
                || java.math.BigDecimal.class.equals(type)
                || java.math.BigInteger.class.equals(type)
                || type.isEnum();
    }
}