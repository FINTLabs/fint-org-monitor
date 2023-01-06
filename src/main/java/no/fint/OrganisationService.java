package no.fint;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.Link;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrganisationService {

    private final Config config;

    public OrganisationService(Config config) {
        this.config = config;
    }

    @Autowired
    private RestUtil restUtil;

    @Autowired
    private OrganisationRepository repository;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private MailingService mailingService;

    @Scheduled(cron = "${fint.orgmonitor.cron}")
    public void update() {
        List<OrganisationDocument> added = new ArrayList<>();
        List<Tuple2<OrganisationDocument, OrganisationDocument>> updated = new ArrayList<>();
        List<OrganisationDocument> updatedDocuments = new ArrayList<>();

        List<OrganisationDocument> documents = repository.getAllByOrgId(config.getOrgid());
        log.info("Repository contains {} documents.", documents.size());

        Map<String, OrganisationDocument> organisationMap = documents.stream().collect(Collectors.toMap(r -> r.getData().getOrganisasjonsId().getIdentifikatorverdi(), Function.identity()));


        OrganisasjonselementResources updates = restUtil.getUpdates(new ParameterizedTypeReference<>() {
        }, config.getEndpoint());
        log.info("Found {} updates.", updates.getContent().size());


        for (OrganisasjonselementResource entityModel : updates.getContent()) {
            String id = entityModel.getOrganisasjonsId().getIdentifikatorverdi();

            OrganisationDocument current = organisationMap.get(id);

            try {
                if (current == null) {
                    OrganisationDocument document = createDocument(entityModel);
                    updatedDocuments.add(document);
                    added.add(document);
                } else {
                    OrganisationDocument modified = createDocument(entityModel);
                    if (!modified.equals(current)) {
                        modified.setId(current.getId());
                        updatedDocuments.add(modified);
                        updated.add(Tuple.tuple(current, modified));
                    }
                }
            } catch (IOException e) {
                log.error("Error converting element", e);
                throw new RuntimeException(e);
            }
        }

        log.info("Saving {} updates ...", updatedDocuments.size());
        repository.saveAll(updatedDocuments);

        log.info("Added: {} items", added.size());

        log.info("Updated: {} items", updated.size());

        if (!added.isEmpty() || !updated.isEmpty()) {
            String result = templateService.render(added, updated);
            mailingService.send(result);
        }
    }

    private OrganisationDocument createDocument(OrganisasjonselementResource resource) throws IOException {
        OrganisationDocument document = new OrganisationDocument();
        document.setOrgId(config.getOrgid());
        document.setData(ResourceConverter.toOrganisasjonselement(resource));

        if (resource.getOverordnet().size() > 0) {
            document.setOverordnet(resource.getOverordnet().get(0).getHref());
        }

        document.setUnderordnet(
                resource
                        .getUnderordnet()
                        .stream()
                        .map(Link::getHref)
                        .collect(Collectors.toList()));

        return document;
    }
}
