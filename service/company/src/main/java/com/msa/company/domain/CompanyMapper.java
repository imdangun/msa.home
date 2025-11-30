package com.msa.company.domain;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel="spring")
public interface CompanyMapper {
    CompanyDto toDto(Company company);
    List<CompanyDto> toDtoList(List<Company> companies);

    @Mapping(target="createdDate", ignore=true)
    @Mapping(target="licenseIds", ignore=true)
    Company toEntity(CompanyDto companyDto);
}