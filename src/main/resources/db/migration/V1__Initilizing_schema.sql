CREATE TABLE organization_document (
   id VARCHAR(255) NOT NULL PRIMARY KEY,
   org_id VARCHAR(255),
   organisasjonselement JSONB,
   overordnet VARCHAR(255),
   last_modified_date TIMESTAMP
);

CREATE TABLE organization_underordnet (
  organization_id VARCHAR(255) NOT NULL,
  underordnet VARCHAR(255),
  CONSTRAINT fk_organization_underordnet_document
      FOREIGN KEY (organization_id)
          REFERENCES organization_document (id)
);