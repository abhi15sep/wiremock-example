package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.learnwiremock.constants.MovieAppConstants;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import com.learnwiremock.helper.TestHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.constants.MovieAppConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(WireMockExtension.class)
public class MoviesRestClientTest {

    MoviesRestClient moviesRestClient = null;
    WebClient webClient = null;

    @InjectServer
    WireMockServer wireMockServer;

    @ConfigureWireMock
    Options options = wireMockConfig()
            .port(8088)
            .notifier(new ConsoleNotifier(true))
            .extensions(new ResponseTemplateTransformer(true));

    @BeforeEach
    void setUp() {
        int port = wireMockServer.port();
        String baseUrl = String.format("http://localhost:%s/", port);
        System.out.println("baseUrl : " + baseUrl);
        webClient = WebClient.create(baseUrl);
        moviesRestClient = new MoviesRestClient(webClient);

    }

    @Test
    void getAllMovies() {

        //given
        stubFor(get(anyUrl())
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("all-movies.json")));
        //when
        List<Movie> movieList = moviesRestClient.retrieveAllMovies();
        System.out.println("movieList : " + movieList);

        //then
        assertTrue(!movieList.isEmpty());
    }

    @Test
    void retrieveAllMovies_matchesUrl() {

        //given
        stubFor(get(urlPathEqualTo(MovieAppConstants.GET_ALL_MOVIES_V1))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("all-movies.json")));

        //when
        List<Movie> movieList = moviesRestClient.retrieveAllMovies();
        System.out.println("movieList : " + movieList);

        //then
        assertTrue(movieList.size() > 0);
    }

    @Test
    void retrieveMovieById() {

        //given
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie.json")));
        //given
        Integer movieId = 1;

        //when
        Movie movie = moviesRestClient.retrieveMovieById(movieId);

        //then
        assertEquals("Batman Begins", movie.getName());
    }

    @Test
    void retrieveMovieById_reponseTemplating() {
        //given
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie-template.json")));
        Integer movieId = 8;

        //when
        Movie movie = moviesRestClient.retrieveMovieById(movieId);
        System.out.println("movie : " + movie);

        //then
        assertEquals("Batman Begins", movie.getName());
        assertEquals(8, movie.getMovie_id().intValue());

    }

    @Test
    void retrieveMovieById_NotFound() {
        //given
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieId.json")));
        Integer movieId = 100;

        //when
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieById(movieId));

    }

    @Test
    void retrieveMovieByName() {
        //given
        String movieName = "Avengers";
        stubFor(get(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1+"?movie_name="+movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));

        //when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);

        //then
        String expectedCastName = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        assertEquals(4, movieList.size());
        assertEquals(expectedCastName, movieList.get(0).getCast());
    }

    @Test
    void retrieveMoviebyName_responeTemplating() {

        //given
        String movieName = "Avengers";
        stubFor(get(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1+"?movie_name="+movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie-byName-template.json")));


        //when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);

        //then
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        assertEquals(4, movieList.size());
        assertEquals(castExpected, movieList.get(0).getCast());

    }

    @Test
    void retrieveMoviebyName_approach2() {

        //given
        String movieName = "Avengers";
        stubFor(get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                // .withQueryParam("movie_name", equalTo(movieName) )
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));


        //when
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);

        //then
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        assertEquals(4, movieList.size());
        assertEquals(castExpected, movieList.get(0).getCast());

    }

    @Test
    void retrieveMoviebyName_Not_Found() {

        //given
        String movieName = "ABC";
        stubFor(get(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1+"?movie_name="+movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieName.json")));


        //when
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieByName(movieName));

    }



    @Test
    void retrieveMovieByYear() {
        //given
        Integer year = 2012;
        stubFor(get(urlEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1+"?year="+year))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("year-template.json")));

        //when
        List<Movie> movieList = moviesRestClient.retreieveMovieByYear(year);

        //then
        assertEquals(2, movieList.size());

    }

    @Test
    void retrieveMovieByYear_Not_Found() {
        //given
        Integer year = 1920;
        stubFor(get(urlEqualTo(MOVIE_BY_YEAR_QUERY_PARAM_V1+"?year="+year))
                .withQueryParam("year", equalTo(year.toString()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("404-movieyear.json")));

        //when
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retreieveMovieByYear(year));

    }

    @Test
    void addMovie() {
        //given
        Movie movie = new Movie(null, "Toys Story 4", "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                // .withQueryParam("movie_name", equalTo(movieName) )
                .withRequestBody(matchingJsonPath(("$.name"),equalTo("Toys Story 4")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie.json")));

        //when
        Movie addedMovie = moviesRestClient.addNewMovie(movie);
        System.out.println(addedMovie);

        //then
        assertTrue(addedMovie.getMovie_id() != null);
    }


    @Test
    void addMovie_responseTemplating() {
        //given
        Movie movie = new Movie(null, "Toys Story 4", "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                // .withQueryParam("movie_name", equalTo(movieName) )
                .withRequestBody(matchingJsonPath(("$.name"),equalTo("Toys Story 4")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie-template.json")));

        //when
        Movie addedMovie = moviesRestClient.addNewMovie(movie);
        System.out.println(addedMovie);

        //then
        assertTrue(addedMovie.getMovie_id() != null);
    }

    @Test
    void addMovie_badRequest() {
        //given
        Movie movie = new Movie(null, null, "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));
        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("400-invalid-input.json")));

        //when
        String expectedErrorMessage = "Please pass all the input fields : [name]";
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.addNewMovie(movie), expectedErrorMessage);
    }

    @Test
    void updateMovie() {
        //given
        Integer movieId = 3;
        String cast = "ABC";
        Movie movie = new Movie(null, null, cast, null, null);
        stubFor(put(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .withRequestBody(matchingJsonPath(("$.cast"), containing(cast)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("updatemovie-template.json")));

        //when
        Movie updatedMovie = moviesRestClient.updateMovie(movieId, movie);

        //then
        assertTrue(updatedMovie.getCast().contains(cast));
    }

    @Test
    void updateMovie_notFound() {
        //given
        Integer movieId = 100;
        String cast = "ABC";
        Movie movie = new Movie(null, null, cast, null, null);
        stubFor(put(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .withRequestBody(matchingJsonPath(("$.cast"), containing(cast)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));


        //then
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movieId, movie));
    }

    @Test
    void deleteMovie() {
        //given
        Movie movie = new Movie(null, "Toys Story 5", "Tom Hanks, Tim Allen", 2019, LocalDate.of(2019, 06, 20));

        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"),equalTo("Toys Story 5")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("add-movie-template.json")));
        Movie addedMovie = moviesRestClient.addNewMovie(movie);

        String expectedErrorMessage = "Movie Deleted Successfully";
        stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(expectedErrorMessage)));

        //when
        String responseMessage = moviesRestClient.deleteMovieById(addedMovie.getMovie_id().intValue());

        //then
        assertEquals(expectedErrorMessage, responseMessage);
    }

    @Test
    void deleteMovie_NotFound() {
        //given
        Integer id = 100;
        stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));

        //then
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovieById(id));

    }
}
