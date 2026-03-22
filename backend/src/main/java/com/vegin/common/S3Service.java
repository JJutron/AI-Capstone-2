package com.vegin.common;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.region:ap-northeast-2}")
    private String region;

    @Value("${cloud.aws.s3.cloudfront-url:}")
    private String cloudfrontUrl;

    @Value("${cloud.aws.access-key}")
    private String accessKey;

    @Value("${cloud.aws.secret-key}")
    private String secretKey;

    // 기존 구조: 매 요청 시 S3Client 생성 (Bean 안 써도 됨)
    private AmazonS3 s3() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(accessKey, secretKey)
                        )
                )
                .build();
    }

    public void upload(MultipartFile file, String key) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("file is empty");
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType() != null
                    ? file.getContentType()
                    : "application/octet-stream");

            log.info("[S3:PUT] bucket={} key={} size={} type={}", bucket, key, file.getSize(), metadata.getContentType());

            s3().putObject(bucket, key, file.getInputStream(), metadata);

            // 업로드 후 즉시 존재 확인
            boolean exists = s3().doesObjectExist(bucket, key);
            if (!exists) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed — object not found after putObject");
            }

            log.info("[S3:OK] s3://{}/{}", bucket, key);

        } catch (AmazonServiceException e) {
            log.error("[S3:Service] Error Code={} Msg={}", e.getErrorCode(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 service error: " + e.getErrorMessage());
        } catch (AmazonClientException e) {
            log.error("[S3:Client] {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 client error");
        } catch (IOException e) {
            log.error("[S3:IO] {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File IO error");
        } catch (Exception e) {
            log.error("[S3:Unknown] {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected S3 error");
        }
    }

    public String getUrl(String key) {
        // CloudFront URL이 설정되어 있으면 사용, 없으면 S3 직접 URL 사용
        if (cloudfrontUrl != null && !cloudfrontUrl.isEmpty()) {
            // CloudFront URL 끝에 슬래시가 없으면 추가
            String baseUrl = cloudfrontUrl.endsWith("/") ? cloudfrontUrl : cloudfrontUrl + "/";
            return baseUrl + key;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
