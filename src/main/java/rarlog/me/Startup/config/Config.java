package rarlog.me.Startup.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.keycloak.admin.client.Keycloak;
import org.openapitools.client.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import rarlog.me.Service.SearchService;
import rarlog.me.Startup.service.StorageApi;
import rarlog.me.Startup.service.StorageService;
import rarlog.me.repository.AlbumRepository;
import rarlog.me.repository.ArtistRepository;
import rarlog.me.repository.SongRepository;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class Config {

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;

    @Value("${storage.host}")
    private String storageHost;

    @Value("${storage.accessKey}")
    private String accessKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public SearchService searchService(
            @Value("${search.host}") String host,
            @Value("${search.port}") String port,
            @Value("${search.collection}") String collection) {

        return new SearchService(String.format("%s:%s", host, port),
                collection, artistRepository, albumRepository, songRepository);
    }

    @Bean
    public StorageService storageService(
            @Value("${storage.hostPort}") String port,
            @Value("${storage.regionName}") String regionName,
            @Value("${storage.secretKey}") String secretKey) {

        return new StorageService(MinioClient.builder()
                .endpoint(HttpUrl.get(String.format("%s:%s", storageHost, port)))
                .region(regionName)
                .credentials(accessKey, secretKey)
                .build());
    }

    @Bean
    public StorageApi storageApi(
            @Value("${storage.hostApiPort}") String port,
            @Value("${storage.accessToken}") String accessToken,
            @Value("${storage.defaultBucket}") String defaultBucket,
            @Value("${storage.playlistBucket}") String playlistBucket,
            @Value("${storage.audioBucket}") String audioBucket) {

        ApiClient client = org.openapitools.client.Configuration.getDefaultApiClient();
        client.setBasePath(String.format("%s:%s", storageHost, port));
        client.setBearerToken(accessToken);
        return new StorageApi(client, defaultBucket, playlistBucket, audioBucket, accessKey);
    }

    @Bean
    public RestClient restClient(@Value("${auth.healthCheckUrl}") String healthCheckUrl) {
        return RestClient.builder()
                .baseUrl(healthCheckUrl)
                .build();
    }

    @Bean
    public Keycloak keycloak(@Value("${auth.apiUrl}") String apiUrl, @Value("${auth.apiUsername}") String username, @Value("${auth.apiPassword}") String password) {
        return Keycloak.getInstance(apiUrl, "master", username, password, "admin-cli");
    }

}
