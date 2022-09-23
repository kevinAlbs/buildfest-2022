package org.buildfest2022;

import com.mongodb.client.model.*;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import io.micronaut.configuration.mongo.core.AbstractMongoConfiguration;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;
import org.bson.types.ObjectId;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.*;

@Singleton
public class MongoDbDocumentRepository implements DocumentRepository {

  private final MongoDbConfiguration mongoConf;

  private MongoClient mongoClient;

  public MongoDbDocumentRepository(MongoDbConfiguration mongoConf, AbstractMongoConfiguration config) throws Exception {
    this.mongoConf = mongoConf;
    this.mongoClient = MongoClientFactory.Create(mongoConf, config);
  }

  @Override
  public Publisher<Document> list() {
    return getDocumentsCollection().find();
  }

  @Override
  public Mono<Boolean> save(Document document) {
    ObjectId documentId =
        Mono.from(getDocumentsCollection().insertOne(document))
            .map(insertOneResult -> insertOneResult.getInsertedId().asObjectId().getValue())
            .block();
    if (documentId == null) {
      return Mono.just(false);
    }

    List<String> lemmas = getLemmas(document.getBody());

    return upsertAllLemmas(lemmas, documentId);
  }

  @Override
  public Publisher<SearchResult> search(String query) {
    List<String> lemmas = getLemmas(query);
    return getLemmasCollection()
        .aggregate(
            Arrays.asList(
                Aggregates.match(Filters.in("lemma", lemmas)),
                Aggregates.unwind("$document_ids"),
                Aggregates.sortByCount("$document_ids"),
                Aggregates.lookup("documents", "_id", "_id", "document"),
                Aggregates.project(Projections.include("count", "document.url")),
                Aggregates.unwind("$document"),
                Aggregates.limit(10)),
            SearchResult.class);
  }

  private Mono<Boolean> upsertAllLemmas(List<String> lemmas, ObjectId documentId) {
    List<WriteModel<Lemma>> bulkUpsert = new ArrayList<>();
    for (String lemma : lemmas) {
      bulkUpsert.add(
          new UpdateOneModel<>(
              Filters.eq("lemma", lemma),
              Updates.addToSet("document_ids", documentId),
              new UpdateOptions().upsert(true)));
    }

    return Mono.from(getLemmasCollection().bulkWrite(bulkUpsert))
        .map(bulkWriteResult -> true)
        .onErrorReturn(false);
  }

  // db.lemmas.updateOne({ lemma: "way" }, { $addToSet: { document_ids:
  // ObjectId("632baf390dac80ee9b2d7149") } }, { upsert: true })

  @NonNull
  private MongoCollection<Document> getDocumentsCollection() {
    return mongoClient
        .getDatabase(mongoConf.getName())
        .getCollection(mongoConf.getDocumentCollection(), Document.class);
  }

  @NonNull
  private MongoCollection<Lemma> getLemmasCollection() {
    return mongoClient
        .getDatabase(mongoConf.getName())
        .getCollection(mongoConf.getLemmaCollection(), Lemma.class);
  }

  private static StanfordCoreNLP lemmatizePipeline;

  static {
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize,pos,lemma");

    lemmatizePipeline = new StanfordCoreNLP(props);
  }

  private static List<String> getLemmas(String body) {
    CoreDocument coreDocument = lemmatizePipeline.processToCoreDocument(body);

    List<String> lemmas = new ArrayList<>();
    Stemmer stemmer = new Stemmer();
    for (CoreLabel tok : coreDocument.tokens()) {
      // Lowercase and remove all non-word characters from the lemma string.
      String lemma = stemmer.stem(tok.lemma()).toLowerCase().replaceAll("\\W", "");
      // If the string is blank or if it's one of the excluded words, skip it.
      if (lemma.isBlank() || excludedWords.contains(lemma)) {
        continue;
      }
      lemmas.add(lemma);
      System.out.print(String.format("%s(%s), ", tok.word(), lemma));
    }
    System.out.println();

    return lemmas;
  }

  private static Set<String> excludedWords =
      Set.of(
          "a",
          "about",
          "above",
          "after",
          "again",
          "against",
          "all",
          "am",
          "an",
          "and",
          "any",
          "are",
          "as",
          "at",
          "be",
          "because",
          "been",
          "before",
          "being",
          "below",
          "between",
          "both",
          "but",
          "by",
          "could",
          "did",
          "do",
          "does",
          "doing",
          "down",
          "during",
          "each",
          "few",
          "for",
          "from",
          "further",
          "had",
          "has",
          "have",
          "having",
          "he",
          "her",
          "here",
          "hers",
          "herself",
          "him",
          "himself",
          "his",
          "how",
          "i",
          "if",
          "in",
          "into",
          "is",
          "it",
          "its",
          "itself",
          "me",
          "more",
          "most",
          "my",
          "myself",
          "nor",
          "of",
          "on",
          "once",
          "only",
          "or",
          "other",
          "ought",
          "our",
          "ours",
          "ourselves",
          "out",
          "over",
          "own",
          "same",
          "she",
          "should",
          "so",
          "some",
          "such",
          "than",
          "that",
          "the",
          "their",
          "theirs",
          "them",
          "themselves",
          "then",
          "there",
          "these",
          "they",
          "this",
          "those",
          "through",
          "to",
          "too",
          "under",
          "until",
          "up",
          "very",
          "was",
          "we",
          "were",
          "what",
          "when",
          "where",
          "which",
          "while",
          "who",
          "whom",
          "why",
          "with",
          "will",
          "would",
          "you",
          "your",
          "yours",
          "yourself",
          "yourselves",
          // MongoDB documentation specific common words.
          "mongodb");
}
