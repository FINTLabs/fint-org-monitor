package no.fint;

import lombok.Data;
import lombok.EqualsAndHashCode;
import no.fint.model.administrasjon.organisasjon.Organisasjonselement;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Document
@Data
public class OrganisationDocument {
    @Id
    @EqualsAndHashCode.Exclude
    private String id;

    @Field
    private String orgId;

    @Field
    private Organisasjonselement data;

    @Field
    private String overordnet;

    @Field
    private List<String> underordnet;

    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

}
