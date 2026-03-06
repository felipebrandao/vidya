package br.com.felipebrandao.vidya.mapper;

import br.com.felipebrandao.vidya.dto.response.CityResponse;
import br.com.felipebrandao.vidya.entity.City;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CityMapper {

    CityResponse toResponse(City entity);

    List<CityResponse> toResponseList(List<City> entities);
}

