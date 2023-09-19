package no.fint.organization;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends MongoRepository<OrganizationDocument, String> {

    List<OrganizationDocument> getAllByOrgId(String orgid);
}
