package no.fint.utils;

import no.fint.organization.SimpleOrganizationInfo;
import org.jooq.lambda.tuple.Tuple2;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import no.fint.organization.OrganizationDocument;

import java.util.List;

@Service
public class TemplateService {

    private final TemplateEngine templateEngine;

    public TemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(List<OrganizationDocument> added, List<Tuple2<OrganizationDocument, OrganizationDocument>> updated, List<SimpleOrganizationInfo> parentInfo) {
        Context context = new Context();
        context.setVariable("added", added);
        context.setVariable("updated", updated);
        context.setVariable("parentInfo", parentInfo);
        return templateEngine.process("email-template", context);
    }
}
