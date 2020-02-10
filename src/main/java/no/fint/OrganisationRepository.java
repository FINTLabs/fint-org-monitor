package no.fint;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganisationRepository extends MongoRepository<OrganisationDocument, String> {

    List<OrganisationDocument> getAllByOrgId(String orgid);
}
