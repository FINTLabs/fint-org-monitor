package no.fint.organization;

import lombok.extern.slf4j.Slf4j;
import no.fint.*;
import no.fint.mailing.MailingService;
import no.fint.model.resource.Link;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources;
import no.fint.utils.ResourceConverter;
import no.fint.utils.RestUtil;
import no.fint.utils.TemplateService;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrganizationService {

    private final Config config;
    private final RestUtil restUtil;
    private final OrganizationRepository repository;
    private final TemplateService templateService;
    private final MailingService mailingService;

    public OrganizationService(Config config, RestUtil restUtil, OrganizationRepository repository, TemplateService templateService, MailingService mailingService) {
        this.config = config;
        this.restUtil = restUtil;
        this.repository = repository;
        this.templateService = templateService;
        this.mailingService = mailingService;
    }

    @Scheduled(cron = "${fint.orgmonitor.cron}")
    public void update() {
        List<OrganizationDocument> added = new ArrayList<>();
        List<Tuple2<OrganizationDocument, OrganizationDocument>> updated = new ArrayList<>();
        List<OrganizationDocument> updatedDocuments = new ArrayList<>();
        List<String> parentIds = new ArrayList<>();

        List<OrganizationDocument> documents = repository.getAllByOrgId(config.getOrgid());
        log.info("Repository contains {} documents.", documents.size());

        Map<String, OrganizationDocument> organisationMap = documents.stream().collect(Collectors.toMap(r -> r.getData().getOrganisasjonsId().getIdentifikatorverdi(), Function.identity()));


        OrganisasjonselementResources updates = restUtil.getUpdates(new ParameterizedTypeReference<>() {
        }, config.getEndpoint());
        log.info("Found {} updates.", updates.getContent().size());

        for (OrganisasjonselementResource resource : updates.getContent()) {
            String id = resource.getOrganisasjonsId().getIdentifikatorverdi();

            OrganizationDocument current = organisationMap.get(id);

            try {
                if (current == null) {
                    OrganizationDocument document = createDocument(resource);
                    updatedDocuments.add(document);
                    added.add(document);
                    if (StringUtils.hasText(document.getOverordnet())) {
                        OrganizationDocument parent = organisationMap.get(document.overordnetId());
                        if (parent != null && !parentIds.contains(parent.getId())) {
                            parentIds.add(parent.getId());
                        }
                    }
                } else {
                    OrganizationDocument modified = createDocument(resource);
                    if (!modified.equals(current)) {
                        modified.setId(current.getId());
                        updatedDocuments.add(modified);
                        updated.add(Tuple.tuple(current, modified));
                        if (StringUtils.hasText(modified.getOverordnet())) {
                            OrganizationDocument parent = organisationMap.get(modified.overordnetId());
                            if (parent != null && !parentIds.contains(parent.getId())) {
                                parentIds.add(parent.getId());
                            }
                        }
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
            List<SimpleOrganizationInfo> parentInfo = parentIds.isEmpty() ? new ArrayList<>() : createParentInfo(parentIds);
            String result = templateService.render(added, updated, parentInfo);
            mailingService.send(result);
        }
    }

    private OrganizationDocument createDocument(OrganisasjonselementResource resource) throws IOException {
        OrganizationDocument document = new OrganizationDocument();
        document.setOrgId(config.getOrgid());

        document.setData(ResourceConverter.toOrganisasjonselement(resource));

        document.setOverordnet(
                resource
                        .getOverordnet()
                        .stream()
                        .map(Link::getHref)
                        .findFirst()
                        .orElse(null));

        document.setUnderordnet(
                resource
                        .getUnderordnet()
                        .stream()
                        .map(Link::getHref)
                        .collect(Collectors.toList()));

        return document;
    }

    private List<SimpleOrganizationInfo> createParentInfo(List<String> parentIds) {
            List<SimpleOrganizationInfo> parentInfo = new ArrayList<>();

            for (String id : parentIds) {
                OrganizationDocument document = repository.getOrganizationDocumentByIdAndOrgId(id, config.getOrgid());
                if (document != null && document.getData().getNavn() !=null) {
                    SimpleOrganizationInfo info = new SimpleOrganizationInfo(id, document.getData().getNavn());
                    parentInfo.add(info);
                }
            }
            return parentInfo;
    }
}
