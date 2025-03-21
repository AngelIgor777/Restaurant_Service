package org.test.restaurant_service.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.test.restaurant_service.service.S3Service;
import org.test.restaurant_service.util.KeyUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final AmazonS3 amazonS3;

    private static final String IMAGE_MIME_TYPE_PREFIX = "image/";

    @Override
    @Async
    public CompletableFuture<String> upload(MultipartFile file, String fileName) {
        if (fileName.charAt(0) == '/') {
            fileName = fileName.substring(1);
        }
        String filePath = "uploads/images/" + fileName;

        // Determine content type
        String contentType = determineContentType(file.getOriginalFilename());
        if (contentType == null) {
            log.warn("Unsupported file type for file: {}", file.getOriginalFilename());
            return null;
        }

        try {
            byte[] optimizedImage = optimizeImage(file.getBytes());

            save(filePath, contentType, optimizedImage);

            return CompletableFuture.completedFuture(fileName);

        } catch (IOException e) {
            log.error("Error uploading file", e);
            return CompletableFuture.completedFuture(null);
        }
    }


    @Override
    public CompletableFuture<String> upload(String fileUrl, String fileName) {
        String filePath = "uploads/images/" + fileName;

        try (InputStream inputStream = new URL(fileUrl).openStream()) {

            // Get content type from URL connection
            URLConnection connection = new URL(fileUrl).openConnection();
            String contentType = "image/jpeg";
            if (contentType == null) {
                log.warn("Cannot determine content type for file: {}", fileUrl);
                return null;
            }

            byte[] fileBytes = inputStream.readAllBytes();

            byte[] optimizedImage = optimizeImage(fileBytes);

            save(filePath, contentType, optimizedImage);

            return CompletableFuture.completedFuture(fileName);

        } catch (IOException e) {
            log.error("Error uploading file from URL: {}", fileUrl, e);
            return null;
        }
    }

    private void save(String filePath, String contentType, byte[] optimizedImage) {
        InputStream optimizedInputStream = new ByteArrayInputStream(optimizedImage);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(optimizedImage.length);
        metadata.setContentDisposition("inline");

        TransferManager transferManager = TransferManagerBuilder.standard()
                .withS3Client(amazonS3)
                .withMultipartUploadThreshold((long) (10 * 1024 * 1024)) // 10MB threshold
                .build();

        PutObjectRequest request = new PutObjectRequest(
                KeyUtil.getBucketName(), filePath, optimizedInputStream, metadata
        ).withCannedAcl(CannedAccessControlList.PublicRead);

        transferManager.upload(request);
    }


    private byte[] optimizeImage(byte[] imageBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

        int maxWidth = 800;
        int newHeight = (image.getHeight() * maxWidth) / image.getWidth();


        Thumbnails.of(image)
                .size(maxWidth, newHeight)
                .outputQuality(0.95)
                .outputFormat("jpg")
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }

    @Override
    public boolean delete(String fileS3Path) {
        String filePath = fileS3Path.substring(fileS3Path.indexOf("uploads/"));

        try {
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(KeyUtil.getBucketName(), filePath);

            amazonS3.deleteObject(deleteObjectRequest);
            return true;
        } catch (Exception e) {
            log.error("Error deleting file: {}", fileS3Path, e);
            return false;
        }
    }


    private String determineContentType(String filename) {
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return IMAGE_MIME_TYPE_PREFIX + "jpeg";
        } else if (filename.endsWith(".png")) {
            return IMAGE_MIME_TYPE_PREFIX + "png";
        }
        return null;
    }
}
