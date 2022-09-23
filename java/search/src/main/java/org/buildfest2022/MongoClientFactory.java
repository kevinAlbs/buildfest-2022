package org.buildfest2022;

import com.mongodb.AutoEncryptionSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import io.micronaut.configuration.mongo.core.AbstractMongoConfiguration;
import org.buildfest2022.qe.Credentials;

import java.util.HashMap;
import java.util.Map;

public class MongoClientFactory {
    public static MongoClient Create(MongoDbConfiguration mongoConf, AbstractMongoConfiguration config) throws Exception {
        if (mongoConf.getEncrypted()) {
            Map<String, String> credentials = Credentials.getCredentials();
            // Create KMS providers.
            Map<String, Map<String, Object>> kmsProviders = new HashMap<String, Map<String, Object>>();
            {
                String kmsProvider = "local";
                Map<String, Object> providerDetails = new HashMap<>();
                providerDetails.put("key", credentials.get("LOCAL_KEY_BASE64"));
                kmsProviders.put("local", providerDetails);
            }

            Map<String, Object> extraOptions = new HashMap<String, Object>();
            extraOptions.put("cryptSharedLibPath", credentials.get("SHARED_LIB_PATH"));
            var autoEncryptionSettings = AutoEncryptionSettings.builder().keyVaultNamespace("encryption.__keyVault").kmsProviders(kmsProviders).extraOptions(extraOptions).build();
            var settings = config.getClientSettings().autoEncryptionSettings(autoEncryptionSettings).build();
            return MongoClients.create(settings);
        }

        return MongoClients.create(config.buildSettings());
    }
}
