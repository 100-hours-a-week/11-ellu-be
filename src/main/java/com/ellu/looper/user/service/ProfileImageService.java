package com.ellu.looper.user.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

  private final AmazonS3Client amazonS3Client;
  private final ResourceLoader resourceLoader;
  private final S3Service s3Service;
  private final Random random = new Random();

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  @Value("${cloud.aws.s3.profile-image-prefix}")
  private String profileImagePrefix;

  public String getRandomProfileImage() {
    // S3에 있는 이미지 목록을 가져와서 랜덤하게 선택함
    List<String> imageKeys = listProfileImages();
    if (imageKeys.isEmpty()) {
      throw new IllegalStateException("No profile images available");
    }
    return imageKeys.get(random.nextInt(imageKeys.size()));
  }

  public List<String> listProfileImages() {
    List<String> imageKeys = new ArrayList<>();
    amazonS3Client
        .listObjects(bucket, profileImagePrefix + "/")
        .getObjectSummaries()
        .forEach(summary -> imageKeys.add(summary.getKey()));
    return imageKeys;
  }

  public String uploadProfileImage(MultipartFile file) throws IOException {
    String fileName = createFileName(file.getOriginalFilename());
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(file.getContentType());
    metadata.setContentLength(file.getSize());

    String key = profileImagePrefix + "/" + fileName;
    amazonS3Client.putObject(bucket, key, file.getInputStream(), metadata);
    return key;
  }

  public void uploadDefaultProfileImages() throws IOException {
    // 이미 업로드된 이미지가 있는지 확인
    if (!listProfileImages().isEmpty()) {
      return;
    }

    for (int i = 1; i <= 20; i++) {
      String fileName = String.format("profile_%d.png", i);
      Resource resource = resourceLoader.getResource("classpath:static/profile-images/" + fileName);

      if (resource.exists()) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/png");
        metadata.setContentLength(resource.contentLength());

        amazonS3Client.putObject(
            bucket, profileImagePrefix + "/" + fileName, resource.getInputStream(), metadata);
      }
    }
  }

  public String getProfileImageUrl(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return null;
    }
    return s3Service.getPresignedUrl(fileName);
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
