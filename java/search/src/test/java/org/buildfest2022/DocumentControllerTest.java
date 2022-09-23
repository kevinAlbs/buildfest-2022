package org.buildfest2022;

import static io.micronaut.http.HttpStatus.CREATED;
import static org.junit.jupiter.api.Assertions.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.List;

import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DocumentControllerTest {
  @Inject DocumentClient documentClient;

  @Inject SearchClient searchClient;
  @Inject com.mongodb.client.MongoClient unencryptedClient;

  @Test
  @Timeout(120)
  void documentsEndpointInteractsWithMongo() {
    List<Document> documents = documentClient.findAll();
    assertTrue(documents.isEmpty());

    HttpStatus status = documentClient.save(new Document("Document body lorem ipsum.", "http://google.com"));
    assertEquals(CREATED, status);

    documents = documentClient.findAll();
    assertFalse(documents.isEmpty());
    assertEquals("Document body lorem ipsum.", documents.get(0).getBody());
  }


  @Test
  @Timeout(120)
  void documentsAreEncrypted() {
    // Delete documents to reset test collections.
    {
      var database = unencryptedClient.getDatabase("search");
      var collection = database.getCollection("documents", BsonDocument.class);
      collection.deleteMany(new BsonDocument());
      collection = database.getCollection("lemmas", BsonDocument.class);
      collection.deleteMany(new BsonDocument());
    }

    List<Document> documents = documentClient.findAll();
    assertTrue(documents.isEmpty());

    HttpStatus status = documentClient.save(new Document("Document body lorem ipsum.", "http://google.com"));
    assertEquals(CREATED, status);

    documents = documentClient.findAll();
    assertFalse(documents.isEmpty());
    assertEquals("Document body lorem ipsum.", documents.get(0).getBody());

    // Check that stored data is encrypted with an unencrypted MongoClient.
    {
      var database = unencryptedClient.getDatabase("search");
      var collection = database.getCollection("documents", BsonDocument.class);
      var doc = collection.find().first();
      assertTrue(doc.isBinary("body"));
    }
  }

  @Test
  @Timeout(120)
  void encryptedDocumentsCanBeSearched() {
    // Delete documents to reset test collections.
    {
      var database = unencryptedClient.getDatabase("search");
      var collection = database.getCollection("documents", BsonDocument.class);
      collection.deleteMany(new BsonDocument());
      collection = database.getCollection("lemmas", BsonDocument.class);
      collection.deleteMany(new BsonDocument());
    }

    List<Document> documents = documentClient.findAll();
    assertTrue(documents.isEmpty());

    HttpStatus status = documentClient.save(new Document("Document body lorem ipsum.", "http://google.com"));
    assertEquals(CREATED, status);

    var got = searchClient.search(new Search("lorem"));
    assertEquals (got.size(), 1);

    // Check that stored data is encrypted with an unencrypted MongoClient.
    {
      var database = unencryptedClient.getDatabase("search");
      var collection = database.getCollection("documents", BsonDocument.class);
      var doc = collection.find().first();
      assertTrue(doc.isBinary("body"));
    }
  }
}
