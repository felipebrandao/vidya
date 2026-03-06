package br.com.felipebrandao.vidya.mapper;

import br.com.felipebrandao.vidya.dto.request.ClientRequest;
import br.com.felipebrandao.vidya.dto.response.ClientResponse;
import br.com.felipebrandao.vidya.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "codSankhya", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Client toEntity(ClientRequest request);

    ClientResponse toResponse(Client entity);

    List<ClientResponse> toResponseList(List<Client> entities);
}


