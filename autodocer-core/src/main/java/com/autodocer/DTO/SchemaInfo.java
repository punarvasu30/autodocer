package com.autodocer.DTO;

import java.util.List;

public record SchemaInfo(
        String className,
        List<FieldInfo> fields
) {}
