package com.mycompany.config;

import org.mapstruct.MapperConfig;

@MapperConfig(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE, nullValueMappingStrategy = org.mapstruct.NullValueMappingStrategy.RETURN_DEFAULT)
public interface MapStructConfig {
}
