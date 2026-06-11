package rarlog.me.Startup.service;

import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.AccessKeyApi;
import org.openapitools.client.api.BucketApi;
import org.openapitools.client.api.PermissionApi;
import org.openapitools.client.api.SpecialEndpointsApi;
import org.openapitools.client.model.ApiBucketKeyPerm;
import org.openapitools.client.model.BucketKeyPermChangeRequest;
import org.openapitools.client.model.CreateBucketRequest;
import org.openapitools.client.model.GetBucketInfoResponse;
import org.openapitools.client.model.UpdateBucketRequestBody;
import org.openapitools.client.model.UpdateBucketWebsiteAccess;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class StorageApi {

    private final ApiClient client;
    private final String defaultBucketName;
    private final String playlistBucket;
    private final String audioBucket;
    private final String accessKey;

    public boolean healthCheck() {
        SpecialEndpointsApi specialEndpointsApi = new SpecialEndpointsApi(client);
        try {
            log.info("StorageApi healthCheck");
            return specialEndpointsApi.healthWithHttpInfo().getStatusCode() == 200;
        } catch (ApiException e) {
            log.warn(e.getMessage());
            return false;
        }
    }

    public void createMusicPlayStorage() {
        BucketApi bucketApi = new BucketApi(client);
        PermissionApi permissionApi = new PermissionApi(client);
        AccessKeyApi accessKeyApi = new AccessKeyApi(client);

        UpdateBucketRequestBody updateBucketRequestBody = new UpdateBucketRequestBody();
        UpdateBucketWebsiteAccess updateBucketWebsiteAccess = new UpdateBucketWebsiteAccess();
        updateBucketWebsiteAccess.setEnabled(true);
        updateBucketWebsiteAccess.setIndexDocument("index.html");
        updateBucketRequestBody.setWebsiteAccess(updateBucketWebsiteAccess);

        String[] bucketNames = new String[] { playlistBucket, audioBucket };
        try {
            for (String bucketName : bucketNames) {

                log.info("Creating bucket");
                CreateBucketRequest createBucketRequest = new CreateBucketRequest();
                createBucketRequest.setGlobalAlias(bucketName);
                GetBucketInfoResponse getBucketInfoResponse = bucketApi.createBucket(createBucketRequest);

                log.info("Linking default key and bucket");
                BucketKeyPermChangeRequest bucketKeyPermChangeRequest = new BucketKeyPermChangeRequest();
                bucketKeyPermChangeRequest.setBucketId(getBucketInfoResponse.getId());
                bucketKeyPermChangeRequest.accessKeyId(
                        accessKeyApi.getKeyInfo(accessKey, null, false).getAccessKeyId());
                ApiBucketKeyPerm apiBucketKeyPerm = new ApiBucketKeyPerm();
                apiBucketKeyPerm.setOwner(true);
                apiBucketKeyPerm.setRead(true);
                apiBucketKeyPerm.setWrite(true);
                bucketKeyPermChangeRequest.setPermissions(apiBucketKeyPerm);
                permissionApi.allowBucketKey(bucketKeyPermChangeRequest);

                log.info("Allowing bucket web access");
                bucketApi.updateBucket(getBucketInfoResponse.getId(), updateBucketRequestBody);
            }

            GetBucketInfoResponse getBucketInfoResponse = bucketApi.getBucketInfo(null, defaultBucketName, null);
            bucketApi.updateBucket(getBucketInfoResponse.getId(), updateBucketRequestBody);

        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

}
