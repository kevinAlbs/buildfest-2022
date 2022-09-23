package org.buildfest2022;


import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import org.reactivestreams.Publisher;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;



@Client("/search")
public interface SearchClient {

    @Post
    List<SearchResult> search(@NonNull @NotNull @Valid Search search);
}