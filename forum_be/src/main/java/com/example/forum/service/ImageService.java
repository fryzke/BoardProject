package com.example.forum.service;

import com.example.forum.domain.Image;
import com.example.forum.domain.Post;
import com.example.forum.domain.User;
import com.example.forum.dto.ImageRequestDto;
import com.example.forum.dto.ImageResponseDto;
import com.example.forum.repository.ImageRepository;
import com.example.forum.repository.PostRepository;
import com.example.forum.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageService {
    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    // 이미지 저장
    public ImageResponseDto createImage(ImageRequestDto dto, Long postId, String loginUserId) {
        Post post = null;
        if (postId != null) {
            post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        }
        User user = userRepository.findByUserId(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        Image image = Image.builder()
                .originalName(dto.getOriginalName())
                .storedName(dto.getStoredName())
                .accessUrl(dto.getAccessUrl())
                .post(post)
                .author(user)
                .fileSize(dto.getFileSize())
                .contentType(dto.getContentType())
                .build();

        imageRepository.save(image);
        return new ImageResponseDto(image);
    }

    /*
     * 이미지 단건 조회
     */
    @Transactional(readOnly = true)
    public ImageResponseDto getImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이미지입니다."));

        return new ImageResponseDto(image);
    }

    // 이미지 목록 조회
    @Transactional(readOnly = true)
    public List<ImageResponseDto> getImages(Long postId) {
        List<Image> images = imageRepository.findAllByPostId(postId);
        List<ImageResponseDto> result = new ArrayList<>();

        for (Image image : images) {
            result.add(new ImageResponseDto(image));
        }

        return result;
    }

    // 이미지 삭제
    public void deleteImage(Long id, String loginId) {
        if (loginId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이미지입니다."));

        if (!image.getAuthor().getUserId().equals(loginId)) {
            throw new IllegalArgumentException("본인이 작성한 글의 이미지만 삭제할 수 있습니다.");
        }

        imageRepository.delete(image);
    }

    // 이미지 수정
    public ImageResponseDto editImage(ImageRequestDto dto, Long id, String loginId) {
        if (loginId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이미지입니다."));

        if(!loginId.equals(image.getAuthor().getUserId())){
            throw new IllegalArgumentException("본인이 작성한 글의 이미지만 수정할 수 있습니다.");
        }
        image.update(dto);
        return new ImageResponseDto(image);
    }
}
