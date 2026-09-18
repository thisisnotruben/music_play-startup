package rarlog.me.Startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.examples.Expander;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import rarlog.me.Service.SearchService;
import rarlog.me.Startup.dto.DataDto;
import rarlog.me.Startup.service.HealthService;
import rarlog.me.Startup.service.StorageApi;
import rarlog.me.Startup.service.StorageService;
import rarlog.me.entity.Album;
import rarlog.me.entity.Artist;
import rarlog.me.entity.Song;
import rarlog.me.entity.UtilStartup;
import rarlog.me.repository.AlbumRepository;
import rarlog.me.repository.ArtistRepository;
import rarlog.me.repository.SongRepository;
import rarlog.me.repository.UtilStartupRepository;
import tools.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class Run {

    private final UtilStartupRepository utilStartupRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final SearchService searchService;
    private final StorageService storageService;
    private final StorageApi storageApi;
    private final HealthService healthService;
    private final RestClient restClient;
    private final Keycloak keycloak;

    @Value("${storage.defaultBucket}")
    private String defaultBucket;

    @Value("${storage.audioBucket}")
    private String audioBucket;

    @Value("${auth.realm}")
    private String authRealm;

    @Bean
    public CommandLineRunner initDevEnv(
            @Value("${data.dataPath}") String dataPath,
            @Value("${data.storagePlaylistTarPath}") String storagePlaylistTarPath,
            @Value("${data.storageAudioTarPath}") String storageAudioTarPath,
            @Value("${data.configPath}") String searchConfigPath) {
        return (args) -> {

            UtilStartup utilStartup = new UtilStartup();
            List<UtilStartup> utilStartups = utilStartupRepository.findAll();
            if (!utilStartups.isEmpty()) {
                utilStartup = utilStartups.getFirst();
            }

            boolean isReady = false;
            do {
                isReady = storageApi.healthCheck();
            } while (!isReady);

            if (!utilStartup.isGarageInit()) {
                log.info("Initing storage service");
                storageApi.createMusicPlayStorage();
                utilStartup.setGarageInit(true);
            }

            if (!utilStartup.isDbInit()) {
                log.info("Opening doc for data parsing");
                InputStreamReader reader = new InputStreamReader(new FileInputStream(dataPath));
                ObjectMapper objectMapper = new ObjectMapper();
                DataDto devData = objectMapper.readValue(reader, DataDto.class);

                log.info("Extracting tar files");
                Path extractedPlaylistTarDirPath = Path.of(storagePlaylistTarPath).getParent();
                new Expander().expand(new TarFile(Path.of(storagePlaylistTarPath).toFile()),
                        extractedPlaylistTarDirPath.toFile());

                Path extractedAudioTarDirPath = Path.of(storageAudioTarPath).getParent();
                new Expander().expand(new TarFile(Path.of(storageAudioTarPath).toFile()),
                        extractedAudioTarDirPath.toFile());

                log.info("Loading data into database and storage bucket");
                devData.getData().keySet().stream().forEach(artistName -> {

                    Artist artist = artistRepository.save(Artist.builder()
                            .name(artistName).build());

                    devData.getData().get(artistName).getAlbums().stream().forEach(devDataAlbum -> {
                        Album album = albumRepository.save(Album.builder()
                                .name(devDataAlbum.getName())
                                .coverPath(devDataAlbum.getCoverPath())
                                .artist(artist)
                                .build());

                        List<Song> songs = new ArrayList<>();
                        devDataAlbum.getSongs().stream().forEach(songData -> {
                            songs.add(Song.builder()
                                    .name(songData.getName())
                                    .genre(songData.getGenre())
                                    .length(songData.getLength())
                                    .audioPath(songData.getAudioPath())
                                    .album(album)
                                    .build());

                            Path audioFilePath = Paths.get(extractedAudioTarDirPath.toString(), songData.getAudioPath());
                            if (Files.exists(audioFilePath)) {
                                storageService.upload(audioFilePath.toFile(), audioBucket, songData.getAudioPath());
                            }
                        });

                        songRepository.saveAll(songs);

                        Path playlstCoverFilePath = Paths.get(extractedPlaylistTarDirPath.toString(),
                                devDataAlbum.getCoverPath());
                        if (Files.exists(playlstCoverFilePath)) {
                            storageService.upload(playlstCoverFilePath.toFile(), defaultBucket,
                                    devDataAlbum.getCoverPath());
                        }
                    });
                });
                utilStartup.setDbInit(true);
            }

            if (!utilStartup.isSolrInit()) {
                log.info("Initing search service");
                if (Files.exists(Paths.get(searchConfigPath))) {
                    searchService.initDatabase(searchConfigPath);
                    log.info("Indexing search database");
                    searchService.indexDatabase();
                } else {
                    log.warn(String.format("Cannot find: [%s]", searchConfigPath));
                }
                utilStartup.setSolrInit(true);
            }

            if (!utilStartup.isAuthInit()) {
                log.info("Checking auth server status");

                boolean isAuthServiceReady = false;
                do {
                    try {
                        isAuthServiceReady = restClient.get().retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful();
                    } catch (ResourceAccessException _) {

                    }
                } while (!isAuthServiceReady);

                log.info("Creating demo user");
                UserRepresentation userRepresentation = new UserRepresentation();
                userRepresentation.setUsername("guest");
                userRepresentation.setEmail("guest@mail.com");
                userRepresentation.setEmailVerified(true);
                userRepresentation.setFirstName("John");
                userRepresentation.setLastName("Doe");
                userRepresentation.setEnabled(true);

                CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
                credentialRepresentation.setType("password");
                credentialRepresentation.setValue("guest123");
                userRepresentation.setCredentials(List.of(credentialRepresentation));

                keycloak.realm(authRealm).users().create(userRepresentation);

                utilStartup.setAuthInit(true);
            }

            log.info("Ready");
            healthService.setReady(true);
            utilStartupRepository.save(utilStartup);
        };
    }

}
