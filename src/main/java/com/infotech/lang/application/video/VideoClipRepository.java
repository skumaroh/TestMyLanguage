package com.infotech.lang.application.video;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoClipRepository extends JpaRepository<VideoClip, Long> {

    List<VideoClip> findByLanguageOrderByIdDesc(String language);

    List<VideoClip> findByLanguageAndLevelAndStatus(String language, int level, String status);
}
