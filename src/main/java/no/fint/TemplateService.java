package no.fint;

import org.jooq.lambda.tuple.Tuple2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import organization.OrganizationDocument;

import java.util.List;

@Service
public class TemplateService {

    @Autowired
    private TemplateEngine templateEngine;

    public String render(List<OrganizationDocument> added, List<Tuple2<OrganizationDocument, OrganizationDocument>> updated) {
        Context context = new Context();
        context.setVariable("added", added);
        context.setVariable("updated", updated);
        return templateEngine.process("email-template", context);
    }
}
