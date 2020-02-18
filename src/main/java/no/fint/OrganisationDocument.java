package no.fint;

import lombok.Data;
import lombok.EqualsAndHashCode;
import no.fint.model.administrasjon.organisasjon.Organisasjonselement;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    @Transient
    public String overordnetId() {
        return StringUtils.substringAfterLast(overordnet, "/");
    }

    @Transient
    public List<String> underordnetId() {
        if (underordnet == null) {
            return Collections.emptyList();
        }
        return underordnet.stream().map(s -> StringUtils.substringAfterLast(s, "/")).collect(Collectors.toList());
    }
}
