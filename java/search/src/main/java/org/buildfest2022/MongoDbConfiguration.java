package org.buildfest2022;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.naming.Named;

@ConfigurationProperties("db")
public interface MongoDbConfiguration extends Named {

  @NonNull
  String getLemmaCollection();

  @NonNull
  String getDocumentCollection();

  @NonNull
  String getSecretCollection();

  boolean getEncrypted();
}
