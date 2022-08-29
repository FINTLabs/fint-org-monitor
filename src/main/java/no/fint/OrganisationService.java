package no.fint;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.administrasjon.organisasjon.Organisasjonselement;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Scheduled(initialDelay = 10000L, fixedDelayString = "${fint.orgmonitor.interval}")
    public void update() {
        List<OrganisationDocument> added = new ArrayList<>();
        List<Tuple2<OrganisationDocument, OrganisationDocument>> updated = new ArrayList<>();
        List<OrganisationDocument> updatedDocuments = new ArrayList<>();

        List<OrganisationDocument> documents = repository.getAllByOrgId(config.getOrgid());
        log.info("Repository contains {} documents.", documents.size());

        Map<String, OrganisationDocument> organisationMap = documents.stream().collect(Collectors.toMap(r -> r.getData().getOrganisasjonsId().getIdentifikatorverdi(), Function.identity()));

        CollectionModel<EntityModel<Organisasjonselement>> updates = restUtil.getUpdates(new ParameterizedTypeReference<CollectionModel<EntityModel<Organisasjonselement>>>() {
        }, config.getEndpoint());
        log.info("Found {} updates.", updates.getContent().size());

        for (EntityModel<Organisasjonselement> EntityModel : updates.getContent()) {
            String id = EntityModel.getContent().getOrganisasjonsId().getIdentifikatorverdi();

            OrganisationDocument current = organisationMap.get(id);

            if (current == null) {
                OrganisationDocument document = createDocument(EntityModel);
                updatedDocuments.add(document);
                added.add(document);
            } else {
                OrganisationDocument modified = createDocument(EntityModel);
                if (!modified.equals(current)) {
                    modified.setId(current.getId());
                    updatedDocuments.add(modified);
                    updated.add(Tuple.tuple(current, modified));
                }
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

    private OrganisationDocument createDocument(EntityModel<Organisasjonselement> EntityModel) {
        OrganisationDocument document = new OrganisationDocument();
        document.setOrgId(config.getOrgid());
        document.setData(EntityModel.getContent());
        Optional<Link> overordnet = EntityModel.getLink("overordnet");
        overordnet.ifPresent(link -> document.setOverordnet(link.getHref()));
        document.setUnderordnet(EntityModel.getLinks().stream().filter(it -> it.getRel().value().equals("underordnet")).map(Link::getHref).collect(Collectors.toList()));
        return document;
    }
}
