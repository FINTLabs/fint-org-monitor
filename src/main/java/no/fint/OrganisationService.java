package no.fint;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.administrasjon.organisasjon.Organisasjonselement;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrganisationService {

    @Autowired
    @Qualifier("endpoint")
    private String endpoint;

    @Autowired
    @Qualifier("orgid")
    private String orgid;

    @Autowired
    private RestUtil restUtil;

    @Autowired
    private OrganisationRepository repository;

    @Scheduled(initialDelay = 10000L, fixedDelay = 40000L)
    public void update() {
        List<OrganisationDocument> added = new ArrayList<>();
        List<Tuple2<OrganisationDocument, OrganisationDocument>> updated = new ArrayList<>();
        List<OrganisationDocument> updatedDocuments = new ArrayList<>();

        List<OrganisationDocument> documents = repository.getAllByOrgId(orgid);
        log.info("Repository contains {} documents.", documents.size());

        Map<String, OrganisationDocument> organisationMap = documents.stream().collect(Collectors.toMap(r -> r.getData().getOrganisasjonsId().getIdentifikatorverdi(), Function.identity()));

        Resources<Resource<Organisasjonselement>> updates = restUtil.getUpdates(new ParameterizedTypeReference<Resources<Resource<Organisasjonselement>>>() {
        }, endpoint);
        log.info("Found {} updates.", updates.getContent().size());

        for (Resource<Organisasjonselement> resource : updates.getContent()) {
            String id = resource.getContent().getOrganisasjonsId().getIdentifikatorverdi();

            OrganisationDocument current = organisationMap.get(id);

            if (current == null) {
                OrganisationDocument document = createDocument(resource);
                updatedDocuments.add(document);
                added.add(document);
            } else {
                OrganisationDocument modified = createDocument(resource);
                if (!modified.equals(current)) {
                    modified.setId(current.getId());
                    updatedDocuments.add(modified);
                    updated.add(Tuple.tuple(current, modified));
                }
            }
        }

        log.info("Saving {} updates ...", updatedDocuments.size());
        repository.save(updatedDocuments);

        log.info("Added: {} items", added.size());

        log.info("Updated: {} items", updated.size());
    }

    private OrganisationDocument createDocument(Resource<Organisasjonselement> resource) {
        OrganisationDocument document = new OrganisationDocument();
        document.setOrgId(orgid);
        document.setData(resource.getContent());
        Link overordnet = resource.getLink("overordnet");
        if (overordnet != null) {
            document.setOverordnet(overordnet.getHref());
        }
        document.setUnderordnet(resource.getLinks().stream().filter(it -> it.getRel().equals("underordnet")).map(Link::getHref).collect(Collectors.toList()));
        return document;
    }
}
