package com.msa.license.domain;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel="spring")
public interface LicenseMapper {
    LicenseDto toDto(License license);
    List<LicenseDto> toDtoList(List<License> licenses);
    License toEntity(LicenseDto licenseDto);
}