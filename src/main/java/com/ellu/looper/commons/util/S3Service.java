package com.ellu.looper.commons.util;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class S3Service {

  private final AmazonS3Client amazonS3Client;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  public String uploadFile(MultipartFile file) throws IOException {
    String fileName = createFileName(file.getOriginalFilename());
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(file.getContentType());
    metadata.setContentLength(file.getSize());

    amazonS3Client.putObject(bucket, fileName, file.getInputStream(), metadata);
    return generatePresignedUrl(fileName);
  }

  public String uploadAudioFile(MultipartFile file) throws IOException {
    String fileName = createFileName(Objects.requireNonNull(file.getOriginalFilename()));
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(file.getContentType());
    metadata.setContentLength(file.getSize());

    amazonS3Client.putObject(bucket, "audio/" + fileName, file.getInputStream(), metadata);
    return generatePresignedUrl("audio/" + fileName);
  }

  private String generatePresignedUrl(String fileName) {
    Date expiration = new Date();
    long expTimeMillis = expiration.getTime();
    expTimeMillis += 1000 * 60 * 60;
    expiration.setTime(expTimeMillis);

    GeneratePresignedUrlRequest generatePresignedUrlRequest =
        new GeneratePresignedUrlRequest(bucket, fileName)
            .withMethod(com.amazonaws.HttpMethod.GET)
            .withExpiration(expiration);

    return amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
  }

  public String getPresignedUrl(String fileName) {
    return generatePresignedUrl(fileName);
  }

  public List<String> uploadMultipleFiles(List<MultipartFile> files) throws IOException {
    List<String> fileUrls = new ArrayList<>();
    for (MultipartFile file : files) {
      String fileUrl = uploadFile(file);
      fileUrls.add(fileUrl);
    }
    return fileUrls;
  }

  private String createFileName(String originalFileName) {
    String baseName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
    return baseName + "_" + UUID.randomUUID() + getFileExtension(originalFileName);
  }

  private String getFileExtension(String fileName) {
    try {
      return fileName.substring(fileName.lastIndexOf("."));
    } catch (StringIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("잘못된 형식의 파일입니다.");
    }
  }
}
