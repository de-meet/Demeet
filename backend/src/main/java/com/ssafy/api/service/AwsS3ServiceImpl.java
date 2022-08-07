package com.ssafy.api.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.ssafy.common.customException.NotImageException;
import com.ssafy.db.entity.ProfileImagePath;
import com.ssafy.db.entity.Users;
import com.ssafy.db.repository.ProfileImagePathRepository;
import com.ssafy.db.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ListIterator;
import java.util.UUID;

@Service("AwsS3Service")
public class AwsS3ServiceImpl implements AwsS3Service{
    @Autowired
    private AmazonS3Client amazonS3Client;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ProfileImagePathRepository profileImagePathRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 파일을 업로드 한다.
    @Override
    public String putImage(MultipartFile multipartFile, Long id, String flag) throws IOException, NotImageException {
        if(!flag.equals("profile") && !flag.equals("drawing")){
            throw new NotImageException("there is no Image");
        }
        String fileName = getFileName(multipartFile.getOriginalFilename(), id, flag);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(multipartFile.getSize());
        objectMetadata.setContentType(multipartFile.getContentType());

        try(InputStream inputStream = multipartFile.getInputStream()){
            amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata).withCannedAcl(CannedAccessControlList.PublicRead));
            // 이미지의 링크주소 반환
            return amazonS3Client.getUrl(bucket, fileName).toString();
        }catch (IOException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");
        }
    }

    // dipid를 받으면 이미지 주소를 리턴한다.
    @Override
    public String getImage(Long imgid){
        return null;
    }

    // dipid를 받으면 이미지를 삭제한다.
    @Override
    public void DeleteImage(Long imgid, String flag) throws NotImageException{
        if(!flag.equals("profile") && !flag.equals("drawing")){
            throw new NotImageException("method must be profile or drawing");
        }
        String folderName = getFolderName(imgid, flag);
        ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request().withBucketName(bucket).withPrefix(folderName);
        ListObjectsV2Result listObjectsV2Result = amazonS3Client.listObjectsV2(listObjectsV2Request);
        ListIterator<S3ObjectSummary> listIterator = listObjectsV2Result.getObjectSummaries().listIterator();

        while (listIterator.hasNext()){
            S3ObjectSummary objectSummary = listIterator.next();
            DeleteObjectRequest request = new DeleteObjectRequest(bucket, objectSummary.getKey());
            amazonS3Client.deleteObject(request);
            System.out.println("Image Deleted");
        }
    }

    @Override
    public String getFolderName(Long imgid, String flag) {
        StringBuilder sb = new StringBuilder();
        sb.append(flag).append('/').append(imgid).append('/');

        return sb.toString();
    }

    @Override
    public String getFileName(String originalFileName, Long id, String flag) throws NotImageException{
        StringBuilder sb = new StringBuilder();
        sb.append(flag).append('/').append(id).append("/").append(UUID.randomUUID()).append(getFileExtension(originalFileName));

        return sb.toString();
    }

    @Override
    public String getFileExtension(String fileName) throws NotImageException { // file 형식이 잘못된 경우를 확인하기 위해 만들어진 로직이며, 파일 타입과 상관없이 업로드할 수 있게 하기 위해 .의 존재 유무만 판단하였습니다.
        try {
            String type = fileName.substring(fileName.lastIndexOf("."));
            if(type.equals(".jpg") || type.equals(".png")){
                return type;
            }else{
                throw new NotImageException("확장자가 잘못되었습니다");
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일(" + fileName + ") 입니다.");
        }
    }

    @Override
    public Users saveImagePath(String path, long uid, String flag) throws NotImageException {
        if(!flag.equals("profile") && !flag.equals("drawing")){
            throw new NotImageException("method must be profile or drawing");
        }
        Users users = usersRepository.getOne(uid);
        users.getProfileImagePath().setPath(path);
        return usersRepository.save(users);
    }
}