package rarlog.me.Startup.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient client;

    public void upload(File filePath, String bucket, String uploadedFilePath) {
        try (InputStream inputStream = new FileInputStream(filePath)) {

            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(uploadedFilePath)
                    .stream(inputStream, Long.valueOf(inputStream.available()), -1L)
                    .build());

        } catch (IOException e) {
        } catch (MinioException e) {
        }
    }

}
