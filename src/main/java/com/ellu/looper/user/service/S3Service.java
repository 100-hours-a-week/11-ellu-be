package com.ellu.looper.user.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

  // FE -> BE -> AI server로 전달하는 구조이므로 음성 파일을 따로 S3에 저장하지 않음(추후 필요에 따라 호출)
  public String uploadAudioFile(MultipartFile file) throws IOException {
    String fileName = createFileName(file.getOriginalFilename());
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(file.getContentType());
    metadata.setContentLength(file.getSize());

    amazonS3Client.putObject(bucket, "audio/" + fileName, file.getInputStream(), metadata);
    return generatePresignedUrl(fileName);
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
    return UUID.randomUUID().toString() + getFileExtension(originalFileName);
  }

  private String getFileExtension(String fileName) {
    try {
      return fileName.substring(fileName.lastIndexOf("."));
    } catch (StringIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("잘못된 형식의 파일입니다.");
    }
  }
}
