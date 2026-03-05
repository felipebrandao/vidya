package br.com.felipebrandao.vidya.mapper;

import br.com.felipebrandao.vidya.dto.request.ClienteRequest;
import br.com.felipebrandao.vidya.dto.response.ClienteResponse;
import br.com.felipebrandao.vidya.entity.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ClienteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "codSankhya", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Cliente toEntity(ClienteRequest request);

    ClienteResponse toResponse(Cliente entity);

    List<ClienteResponse> toResponseList(List<Cliente> entities);
}


